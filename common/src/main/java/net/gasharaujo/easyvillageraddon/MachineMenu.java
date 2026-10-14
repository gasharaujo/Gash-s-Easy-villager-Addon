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
    public boolean clientGenericTargets, clientSelectedTargets;
    private final Inventory playerInventory;
    private final Merchant merchant;
    private final MerchantContainer trading;
    private final Container identity, storage;
    private TradeCatalog sentCatalog;
    private int revision, sentCount;
    private long lastSync = Long.MIN_VALUE;
    private int selectedTrade;

    public MachineMenu(int id, Inventory inventory) { this(id, inventory, null, new net.minecraft.world.entity.npc.ClientSideMerchant(inventory.player)); }
    public MachineMenu(int id, Inventory inventory, MachineEntity machine) { this(id, inventory, machine, new MachineMerchant(machine, inventory.player)); }
    private MachineMenu(int id, Inventory inventory, MachineEntity machine, Merchant merchant) {
        super(id, inventory, merchant);
        this.machine = machine; this.playerInventory = inventory; this.merchant = merchant;
        trading = (MerchantContainer) slots.get(0).container;
        identity = machine == null ? new SimpleContainer(2) : new IdentityInventory(machine);
        storage = machine == null ? new SimpleContainer(27) : machine;
        configureSlots();
        if (machine != null) { machine.viewer = inventory.player.getUUID(); machine.rebuildCatalog(); applyDiscounts(); }
    }
    public void configureSlots() {
        replace(0, paymentSlot(0, 136)); replace(1, paymentSlot(1, 162));
        replace(2, new MerchantResultSlot(playerInventory.player, merchant, trading, 2, 220, 37) {
            @Override public boolean mayPickup(Player player) { return paymentVisible() && manualTrading() && super.mayPickup(player); }
            @Override public boolean isActive() { return paymentVisible(); }
        });
        int ix = MachineLayout.inventoryX(kind(), clientPane), iy = MachineLayout.inventoryY(kind(), clientPane);
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) {
            replace(3 + row * 9 + col, playerSlot(9 + row * 9 + col, ix + col * 18, iy + row * 18));
        }
        for (int col = 0; col < 9; col++) replace(30 + col, playerSlot(col, ix + col * 18, iy + 58));
        int vx = kind() == MachineKind.LEVELER ? 8 : kind() == MachineKind.BRIBER ? 26 : 62;
        int vy = kind() == MachineKind.LEVELER ? 24 : 35;
        replace(VILLAGER, new Slot(identity, 0, vx, vy) {
            @Override public boolean mayPlace(ItemStack stack) { return isActive() && stack.getItem() instanceof VillagerItem; }
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean isActive() { return !picker() && kind() != MachineKind.DESNEURALIZER && (kind() != MachineKind.ADVANCED_TRADER || clientPane == 2); }
        });
        replace(WORKSTATION, new Slot(identity, 1, kind() == MachineKind.LEVELER ? 30 : 98, vy) {
            @Override public boolean mayPlace(ItemStack stack) { return isActive() && stack.getItem() instanceof BlockItem item && (machine == null || machine.isValidBlock(item.getBlock())); }
            @Override public int getMaxStackSize() { return 1; }
            @Override public boolean isActive() { return !picker() && supportsWorkstation() && (kind() != MachineKind.ADVANCED_TRADER || clientPane == 2); }
        });
        for (int i = 0; i < 18; i++) {
            final int bufferSlot = 9 + i, cell = i % 9;
            final boolean output = i >= 9;
            int x = kind() == MachineKind.DESNEURALIZER ? 52 + cell * 18 : kind() == MachineKind.BRIBER ? 62 + cell % 3 * 18 : (output ? 116 : 8) + cell % 3 * 18;
            int y = kind() == MachineKind.DESNEURALIZER ? (output ? 51 : 20) : kind() == MachineKind.BRIBER ? 17 + cell / 3 * 18 : 64 + cell / 3 * 18;
            replace(INPUT + i, new Slot(storage, bufferSlot, x, y) {
                @Override public boolean mayPlace(ItemStack stack) {
                    if (!isActive() || output) return false;
                    if (machine != null) return machine.canPlaceItem(bufferSlot, stack);
                    return kind() == MachineKind.DESNEURALIZER ? stack.getItem() instanceof VillagerItem : kind() != MachineKind.BRIBER || stack.is(net.minecraft.world.item.Items.EMERALD);
                }
                @Override public int getMaxStackSize() { return kind() == MachineKind.DESNEURALIZER ? 1 : super.getMaxStackSize(); }
                @Override public boolean isActive() {
                    if (picker()) return false;
                    return switch (kind()) { case ADVANCED_TRADER -> false; case DESNEURALIZER -> cell < 4; case BRIBER -> !output; case LEVELER -> true; };
                }
            });
        }
    }
    private Slot paymentSlot(int index, int x) {
        return new Slot(trading, index, x, 37) {
            @Override public boolean isActive() { return paymentVisible(); }
            @Override public boolean mayPlace(ItemStack stack) { return paymentVisible() && manualTrading(); }
            @Override public boolean mayPickup(Player player) { return paymentVisible(); }
        };
    }
    private Slot playerSlot(int index, int x, int y) { return new Slot(playerInventory, index, x, y) { @Override public boolean isActive() { return !picker(); } }; }
    private boolean picker() { return MachineLayout.picker(kind(), clientPane); }
    public void setPane(int pane) { clientPane = pane; configureSlots(); }
    private void replace(int index, Slot slot) { if (index == slots.size()) addSlot(slot); else { slot.index = index; slots.set(index, slot); } }
    @Override public MenuType<?> getType() { return AddonRegistry.MENU.get(); }
    private boolean supportsWorkstation() { return kind() == MachineKind.ADVANCED_TRADER || kind() == MachineKind.LEVELER; }
    public MachineKind kind() { return machine == null ? MachineKind.values()[Math.max(0, Math.min(3, state.kind))] : machine.kind(); }
    private boolean paymentVisible() { return kind() == MachineKind.ADVANCED_TRADER && clientPane == 0; }
    public boolean manualTrading() { return kind() == MachineKind.ADVANCED_TRADER && (machine == null ? state.hasVillager && !state.enabled : machine.hasVillager() && !machine.enabled); }
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
                if ((kind() == MachineKind.LEVELER || kind() == MachineKind.ADVANCED_TRADER) && !machine.targetResult.isEmpty()
                        && (kind() != MachineKind.ADVANCED_TRADER || machine.getVillagerEntity() != null && machine.getVillagerEntity().getVillagerXp() == 0)) {
                    machine.enabled = !machine.enabled;
                    machine.status = "gui.gashs_easy_villager_addon.status." + (machine.enabled ? "running" : "paused");
                }
            }
            case UiAction.PANE -> {
                int pane = action.value();
                if (pane < 0 || pane > (kind() == MachineKind.ADVANCED_TRADER ? 2 : kind() == MachineKind.LEVELER ? 1 : 0)) return;
                if (pane != 0 && kind() == MachineKind.ADVANCED_TRADER) returnPayments(player);
                setPane(pane);
            }
            case UiAction.LEVEL -> {
                if (kind() == MachineKind.LEVELER && action.value() >= 1 && action.value() <= 5 && action.value() != machine.targetLevel) {
                    machine.migrateTargets(); machine.targetLevel = action.value();
                    machine.invalidateCatalog();
                }
            }
            case UiAction.TARGET_EXACT, UiAction.TARGET_GENERIC -> {
                if (kind() == MachineKind.LEVELER && action.revision() == revision && sentCatalog == machine.catalog && machine.catalog != null
                        && action.value() >= 0 && action.value() < Math.min(sentCount, machine.catalog.offers.size()))
                    machine.toggleTarget(machine.catalog.offers.get(action.value()), action.action() == UiAction.TARGET_GENERIC);
            }
            case UiAction.REMOVE_TARGET -> {
                if (kind() == MachineKind.LEVELER && action.revision() == machine.targetsRevision) machine.removeTarget(action.value());
            }
            case UiAction.MATCH_MODE -> {
                if (kind() == MachineKind.LEVELER && (action.value() == 0 || action.value() == 1)) {
                    machine.migrateTargets(); machine.matchAll = action.value() == 1; machine.targetsChanged();
                }
            }
            case UiAction.CLEAR_TARGETS -> { if (kind() == MachineKind.LEVELER) machine.clearTargets(); }
            case UiAction.CHOOSE -> {
                if ((kind() == MachineKind.ADVANCED_TRADER || kind() == MachineKind.LEVELER)
                        && action.revision() == revision && sentCatalog == machine.catalog && machine.catalog != null
                        && action.value() >= 0 && action.value() < Math.min(sentCount, machine.catalog.offers.size())) {
                    if (kind() == MachineKind.ADVANCED_TRADER) returnPayments(player);
                    machine.select(machine.catalog.offers.get(action.value()));
                }
            }
            case UiAction.TRADE -> {
                if (manualTrading() && action.value() >= 0 && action.value() < getOffers().size()) {
                    selectedTrade = action.value(); setSelectionHint(selectedTrade); tryMoveItems(selectedTrade);
                }
            }
            case UiAction.REFRESH -> { sentCatalog = null; sentCount = 0; machine.rebuildCatalog(); }
            default -> { return; }
        }
        applyDiscounts(); machine.setChanged(); forceSync();
    }
    private void returnPayments(Player player) {
        for (int i = 0; i < 2; i++) {
            ItemStack stack = trading.removeItemNoUpdate(i);
            if (!stack.isEmpty() && !playerInventory.add(stack)) player.drop(stack, false);
        }
        trading.updateSellItem();
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
        if (kind() == MachineKind.ADVANCED_TRADER) {
            player.sendMerchantOffers(containerId, getOffers(), v == null ? 1 : Platform.villagerLevel(v), merchant.getVillagerXp(), true, false);
            setSelectionHint(Math.max(0, Math.min(selectedTrade, getOffers().size() - 1)));
            trading.updateSellItem();
        }
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
            s.targets = machine.effectiveTargets(); s.matchAll = machine.matchAll; s.targetsRevision = machine.targetsRevision;
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
        boolean changedKind = state.kind != snapshot.kind;
        state = snapshot;
        if (changedKind) { clientPane = 0; configureSlots(); }
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !stillValid(player)) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack source = slot.getItem(), original = source.copy();
        boolean moved;
        if (index == 2) {
            if (!manualTrading()) return ItemStack.EMPTY;
            moved = moveItemStackTo(source, 3, 39, true);
            if (moved) slot.onQuickCraft(source, original);
        } else if (index < 3 || index >= VILLAGER) moved = moveItemStackTo(source, 3, 39, true);
        else if (kind() == MachineKind.DESNEURALIZER && source.getItem() instanceof VillagerItem) moved = moveItemStackTo(source, INPUT, INPUT + 4, false);
        else if (slots.get(VILLAGER).isActive() && source.getItem() instanceof VillagerItem && !slots.get(VILLAGER).hasItem()) moved = moveItemStackTo(source, VILLAGER, VILLAGER + 1, false);
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
