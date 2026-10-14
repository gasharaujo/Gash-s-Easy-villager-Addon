package net.gasharaujo.easyvillageraddon;

import de.maxhenkel.easyvillagers.items.VillagerItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.MerchantContainer;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.MerchantResultSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import java.util.ArrayList;
import java.util.List;

/** Real vanilla merchant slots, a player inventory and typed machine storage slots. */
public final class MachineMenu extends MerchantMenu {
    public static final int VILLAGER = 39, WORKSTATION = 40, INPUT = 41, OUTPUT = 50, END = 59;
    public final MachineEntity machine;
    public UiSnapshot state = new UiSnapshot();
    public final List<OfferView> catalog = new ArrayList<>();
    public int clientPane;
    private final Inventory playerInventory;
    private final Merchant merchant;
    private TradeCatalog sentCatalog;
    private int revision, sentCount;
    private long lastSync = Long.MIN_VALUE;
    private int selectedTrade;

    public MachineMenu(int id, Inventory inventory) { this(id, inventory, null, new net.minecraft.world.entity.npc.ClientSideMerchant(inventory.player)); }
    public MachineMenu(int id, Inventory inventory, MachineEntity machine) { this(id, inventory, machine, new MachineMerchant(machine, inventory.player)); }
    private MachineMenu(int id, Inventory inventory, MachineEntity machine, Merchant merchant) {
        super(id, inventory, merchant);
        this.machine = machine; this.playerInventory = inventory; this.merchant = merchant;
        MerchantContainer trading = (MerchantContainer) slots.get(0).container;
        replace(0, new Slot(trading, 0, 202, 82) { @Override public boolean isActive() { return paymentVisible(); } });
        replace(1, new Slot(trading, 1, 228, 82) { @Override public boolean isActive() { return paymentVisible(); } });
        replace(2, new MerchantResultSlot(inventory.player, merchant, trading, 2, 286, 82) {
            @Override public boolean mayPickup(Player player) { return manualTrading() && super.mayPickup(player); }
            @Override public boolean isActive() { return paymentVisible(); }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            replace(3 + row * 9 + col, new Slot(inventory, 9 + row * 9 + col, 202 + col * 18, 148 + row * 18));
        }
        for (int col = 0; col < 9; col++) replace(30 + col, new Slot(inventory, col, 202 + col * 18, 206));
        Container identity = machine == null ? new SimpleContainer(2) : new IdentityInventory(machine);
        addSlot(new Slot(identity, 0, 202, 34) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof VillagerItem; }
            @Override public int getMaxStackSize() { return 1; }
        });
        addSlot(new Slot(identity, 1, 242, 34) {
            @Override public boolean mayPlace(ItemStack stack) { return supportsWorkstation() && stack.getItem() instanceof BlockItem item && (machine == null || machine.isValidBlock(item.getBlock())); }
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean isActive() { return supportsWorkstation(); }
        });
        Container storage = machine == null ? new SimpleContainer(27) : machine;
        for (int i = 0; i < 18; i++) {
            final int bufferSlot = 9 + i;
            final boolean output = i >= 9;
            addSlot(new Slot(storage, bufferSlot, (output ? 108 : 24) + (i % 3) * 18, 100 + (i % 9 / 3) * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return !output && (machine == null ? kind() != MachineKind.BRIBER || stack.is(net.minecraft.world.item.Items.EMERALD) : machine.canPlaceItem(bufferSlot, stack)); }
                @Override public boolean isActive() { return machine != null || clientPane == 2 && (!output || kind() == MachineKind.LEVELER); }
            });
        }
        if (machine != null) { machine.viewer = inventory.player.getUUID(); machine.rebuildCatalog(); applyDiscounts(); }
    }
    private void replace(int index, Slot slot) { slot.index = index; slots.set(index, slot); }
    @Override public MenuType<?> getType() { return AddonRegistry.MENU.get(); }
    private boolean supportsWorkstation() { return kind() == MachineKind.ADVANCED_TRADER || kind() == MachineKind.LEVELER; }
    public MachineKind kind() { return machine == null ? MachineKind.values()[Math.max(0, Math.min(3, state.kind))] : machine.kind(); }
    private boolean paymentVisible() { return machine != null || clientPane != 1; }
    public boolean manualTrading() { return machine == null ? state.hasVillager && !state.enabled : machine.hasVillager() && !machine.enabled; }
    @Override public void setSelectionHint(int hint) { super.setSelectionHint(hint); selectedTrade = hint; }
    @Override public boolean stillValid(Player player) { return machine == null || machine.accessible(player); }

    private void applyDiscounts() {
        if (machine == null || machine.getVillagerEntity() == null) return;
        var v = machine.getVillagerEntity();
        int reputation = v.getPlayerReputation(playerInventory.player);
        for (var offer : v.getOffers()) {
            offer.resetSpecialPriceDiff();
            offer.addToSpecialPriceDiff(-net.minecraft.util.Mth.floor(reputation * offer.getPriceMultiplier()));
            var hero = playerInventory.player.getEffect(net.minecraft.world.effect.MobEffects.HERO_OF_THE_VILLAGE);
            if (hero != null) offer.addToSpecialPriceDiff(-Math.max(1, (int) Math.floor((0.3 + 0.0625 * hero.getAmplifier()) * offer.getBaseCostA().getCount())));
        }
    }

    public void act(ServerPlayer player, UiAction action) {
        if (machine == null || !stillValid(player)) return;
        switch (action.action()) {
            case UiAction.TOGGLE -> {
                if (kind() == MachineKind.LEVELER && !machine.targetResult.isEmpty()) {
                    machine.enabled = !machine.enabled;
                    machine.status = "gui.gashs_easy_villager_addon.status." + (machine.enabled ? "running" : "paused");
                }
            }
            case UiAction.LEVEL -> {
                if (kind() == MachineKind.LEVELER && action.value() >= 1 && action.value() <= 5 && action.value() != machine.targetLevel) {
                    machine.targetLevel = action.value(); machine.targetResult = ItemStack.EMPTY;
                    machine.targetCostA = ItemStack.EMPTY; machine.targetCostB = ItemStack.EMPTY;
                    machine.invalidateCatalog();
                }
            }
            case UiAction.CHOOSE -> {
                if ((kind() == MachineKind.ADVANCED_TRADER || kind() == MachineKind.LEVELER)
                        && action.revision() == revision && sentCatalog == machine.catalog && machine.catalog != null
                        && action.value() >= 0 && action.value() < Math.min(sentCount, machine.catalog.offers.size())) {
                    machine.select(machine.catalog.offers.get(action.value()));
                }
            }
            case UiAction.TRADE -> {
                if (manualTrading() && action.value() >= 0 && action.value() < getOffers().size()) {
                    selectedTrade = action.value(); setSelectionHint(selectedTrade); tryMoveItems(selectedTrade);
                }
            }
            case UiAction.RESET -> { if (kind() == MachineKind.DESNEURALIZER) machine.resetProfession(false); }
            case UiAction.REFRESH -> { sentCatalog = null; sentCount = 0; machine.rebuildCatalog(); }
            default -> { return; }
        }
        applyDiscounts(); machine.setChanged(); forceSync();
    }

    @Override public void broadcastChanges() {
        super.broadcastChanges();
        if (machine != null && playerInventory.player instanceof ServerPlayer player) {
            long tick = machine.getLevel().getGameTime();
            if (lastSync == Long.MIN_VALUE || tick - lastSync >= 5) { lastSync = tick; sync(player); }
        }
    }
    public void forceSync() {
        if (machine != null && playerInventory.player instanceof ServerPlayer player) { lastSync = machine.getLevel().getGameTime(); sync(player); }
    }
    private void sync(ServerPlayer player) {
        if (sentCatalog != machine.catalog) { sentCatalog = machine.catalog; sentCount = 0; revision++; }
        applyDiscounts();
        var v = machine.getVillagerEntity();
        player.sendMerchantOffers(containerId, getOffers(), v == null ? 1 : Platform.villagerLevel(v), merchant.getVillagerXp(), true, kind() != MachineKind.ADVANCED_TRADER);
        setSelectionHint(Math.max(0, Math.min(selectedTrade, getOffers().size() - 1)));
        ((MerchantContainer) slots.get(0).container).updateSellItem();
        int available = sentCatalog == null ? 0 : sentCatalog.offers.size();
        int batches = 0;
        do {
            UiSnapshot s = new UiSnapshot();
            s.menuId = containerId; s.kind = kind().ordinal(); s.enabled = machine.enabled;
            s.hasVillager = v != null; s.hasWorkstation = machine.hasWorkstation();
            s.level = v == null ? 1 : Platform.villagerLevel(v); s.xp = merchant.getVillagerXp();
            s.canChooseInitial = v != null && !v.isBaby() && s.level == 1 && s.xp == 0;
            s.name = v != null && v.hasCustomName() ? v.getCustomName().getString() : "";
            s.profession = v == null ? "none" : Platform.profession(v);
            s.targetLevel = machine.targetLevel; s.bribeTicks = machine.bribeTicks; s.attempts = machine.attempts;
            s.reputation = v == null || machine.owner == null ? 0 : v.getGossips().getReputation(machine.owner, t -> t == GossipType.MINOR_POSITIVE);
            s.reputationLimit = GossipType.MINOR_POSITIVE.max; s.status = machine.status;
            s.target = machine.targetResult.copy(); s.revision = revision; s.offset = sentCount;
            s.catalogSize = available; s.catalogComplete = sentCatalog != null && sentCatalog.complete();
            List<OfferView> batch = new ArrayList<>();
            int end = Math.min(available, sentCount + 48);
            while (sentCount < end) { batch.add(OfferView.of(sentCount, sentCatalog.offers.get(sentCount))); sentCount++; }
            s.entries = batch; UiNetwork.send(player, s);
        } while (sentCount < available && ++batches < 4);
    }

    public void receive(UiSnapshot snapshot) {
        if (snapshot.menuId != containerId) return;
        if (state.revision != snapshot.revision) catalog.clear();
        if (snapshot.offset == catalog.size()) catalog.addAll(snapshot.entries);
        state = snapshot;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !stillValid(player)) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack source = slot.getItem(), original = source.copy();
        boolean moved;
        if (index == 2) {
            if (!manualTrading()) return ItemStack.EMPTY;
            moved = moveItemStackTo(source, 3, 39, true);
            if (moved) slot.onQuickCraft(source, original);
        } else if (index < 3 || index >= VILLAGER) moved = moveItemStackTo(source, 3, 39, true);
        else if (source.getItem() instanceof VillagerItem && !slots.get(VILLAGER).hasItem()) moved = moveItemStackTo(source, VILLAGER, VILLAGER + 1, false);
        else if (supportsWorkstation() && slots.get(WORKSTATION).mayPlace(source) && !slots.get(WORKSTATION).hasItem()) moved = moveItemStackTo(source, WORKSTATION, WORKSTATION + 1, false);
        else if (kind() == MachineKind.LEVELER || kind() == MachineKind.BRIBER) moved = moveItemStackTo(source, INPUT, OUTPUT, false);
        else moved = index < 30 ? moveItemStackTo(source, 30, 39, false) : moveItemStackTo(source, 3, 30, false);
        if (!moved || source.getCount() == original.getCount()) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, source);
        if (machine != null) machine.setChanged();
        return original;
    }
    @Override public void removed(Player player) {
        super.removed(player);
        if (machine != null) {
            if (player.getUUID().equals(machine.viewer)) machine.viewer = null;
            if (machine.getVillagerEntity() != null) machine.getVillagerEntity().recalculateOffers();
            machine.saveVillagerEntity(); machine.setChanged();
        }
    }

    private static final class IdentityInventory extends SimpleContainer {
        private final MachineEntity machine;
        IdentityInventory(MachineEntity machine) { super(2); this.machine = machine; }
        @Override public ItemStack getItem(int index) { return index == 0 ? machine.getVillager() : machine.hasWorkstation() ? new ItemStack(machine.getWorkstation()) : ItemStack.EMPTY; }
        @Override public void setItem(int index, ItemStack stack) {
            if (index == 0) machine.setVillager(stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1));
            else if (stack.getItem() instanceof BlockItem item) machine.setWorkstation(item.getBlock());
            else machine.removeWorkstation();
            machine.invalidateCatalog();
        }
        @Override public ItemStack removeItem(int index, int count) {
            if (count <= 0) return ItemStack.EMPTY;
            ItemStack result = index == 0 ? machine.removeVillager() : machine.hasWorkstation() ? new ItemStack(machine.removeWorkstation()) : ItemStack.EMPTY;
            machine.invalidateCatalog(); return result;
        }
        @Override public ItemStack removeItemNoUpdate(int index) { return removeItem(index, 1); }
        @Override public void setChanged() { if (machine != null) machine.setChanged(); }
    }
}
