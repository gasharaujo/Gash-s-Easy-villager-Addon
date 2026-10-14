package net.gasharaujo.easyvillageraddon;

import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import de.maxhenkel.easyvillagers.items.VillagerItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.ArrayList;
import java.util.List;

public final class MachineEntity extends VersionedMachineEntity implements net.minecraft.world.WorldlyContainer {
    TradeCatalog catalog;
    private boolean dropped;
    private int cycleTicks;
    java.util.UUID viewer;
    String status = "";

    public MachineEntity(BlockPos pos, BlockState state) { super(pos, state); }
    public MachineKind kind() { return ((MachineBlock) getBlockState().getBlock()).kind; }

    public void open(Player player) {
        if (viewer != null && !viewer.equals(player.getUUID()) && level instanceof net.minecraft.server.level.ServerLevel server) {
            var other = server.getServer().getPlayerList().getPlayer(viewer);
            if (other != null && other.containerMenu instanceof MachineMenu open && open.machine == this) {
                if (player instanceof net.minecraft.server.level.ServerPlayer recipient) recipient.sendSystemMessage(Component.translatable("gui.gashs_easy_villager_addon.busy"), true);
                return;
            }
        }
        if (player.containerMenu.getCarried().isEmpty()) {
            player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new MachineMenu(id, inventory, this),
                    Component.literal(kind().title)));
        }
    }

    public void interact(Player player, ItemStack held) {
        if (level.isClientSide()) return;
        if (!hasVillager() && held.getItem() instanceof VillagerItem) {
            setVillager(held.copyWithCount(1));
            held.shrink(1);
            invalidateCatalog();
        } else if (!hasWorkstation() && held.getItem() instanceof BlockItem item && isValidBlock(item.getBlock())
                && (kind() == MachineKind.ADVANCED_TRADER || kind() == MachineKind.LEVELER)) {
            setWorkstation(item.getBlock());
            held.shrink(1);
            invalidateCatalog();
        } else {
            open(player);
        }
    }

    void invalidateCatalog() {
        catalog = null; enabled = false; setChanged();
        if (hasVillager() && (kind() == MachineKind.ADVANCED_TRADER || kind() == MachineKind.LEVELER)) rebuildCatalog();
    }

    void rebuildCatalog() {
        EasyVillagerEntity v = getVillagerEntity();
        catalog = v == null || v.isBaby() ? null : new TradeCatalog(v, kind() == MachineKind.ADVANCED_TRADER ? 1 : targetLevel);
    }

    void resetProfession(boolean restore) {
        EasyVillagerEntity v = getVillagerEntity();
        if (v == null || v.isBaby() || v.isTrading()) return;
        Platform.clearProfession(v);
        if (restore && hasWorkstation()) Platform.assignProfession(v, getWorkstationProfession());
        saveVillagerEntity();
        setChanged();
    }

    boolean select(MerchantOffer choice) {
        EasyVillagerEntity v = getVillagerEntity();
        if (v == null || v.isBaby() || v.isTrading()) return false;
        if (kind() == MachineKind.ADVANCED_TRADER) {
            if (Platform.villagerLevel(v) != 1 || v.getVillagerXp() != 0) {
                status = "gui.gashs_easy_villager_addon.fresh_villager_required";
                return false;
            }
            MerchantOffers offers = v.getOffers();
            if (offers.isEmpty()) offers.add(Platform.unlimited(choice, v));
            else offers.set(0, Platform.unlimited(choice, v));
            saveVillagerEntity();
            status = "gui.gashs_easy_villager_addon.status.initial_selected";
        } else {
            targetResult = choice.getResult().copy();
            targetCostA = choice.getBaseCostA().copy();
            targetCostB = choice.getCostB().copy();
            enabled = false;
            status = "gui.gashs_easy_villager_addon.status.target_selected";
        }
        setChanged();
        return true;
    }

    @Override public void tickServer() {
        if (catalog != null && !catalog.complete()) catalog.step();
        EasyVillagerEntity v = getVillagerEntity();
        if (v == null) return;
        if (kind() == MachineKind.ADVANCED_TRADER || kind() == MachineKind.LEVELER) super.tickServer();
        else if (v.isBaby()) { advanceAge(); setChanged(); }
        if (kind() == MachineKind.ADVANCED_TRADER && !v.isBaby()) {
            MerchantOffers offers = v.getOffers();
            for (int i = 0; i < offers.size(); i++) {
                MerchantOffer offer = offers.get(i);
                if (offer.getMaxUses() < Integer.MAX_VALUE) offers.set(i, Platform.unlimited(offer, v));
                else if (offer.getUses() > 0) offer.resetUses();
            }
        }
        if (v.isBaby() || v.isTrading()) return;
        if (++cycleTicks < 20) return;
        cycleTicks = 0;
        switch (kind()) {
            case DESNEURALIZER -> {
                if (!Platform.unemployed(v) || v.getVillagerXp() != 0 || Platform.villagerLevel(v) != 1) resetProfession(false);
                status = "gui.gashs_easy_villager_addon.status.profession_removed";
            }
            case BRIBER -> bribe(v);
            case LEVELER -> { if (enabled) leveler(v); }
            default -> { }
        }
    }

    private void bribe(EasyVillagerEntity v) {
        if (owner == null) { status = "gui.gashs_easy_villager_addon.status.no_owner"; return; }
        int reputation = v.getGossips().getReputation(owner, type -> type == GossipType.MINOR_POSITIVE);
        if (reputation >= GossipType.MINOR_POSITIVE.max) { bribeTicks = 0; status = "gui.gashs_easy_villager_addon.status.reputation_limit"; return; }
        ItemStack emeralds = new ItemStack(Items.EMERALD, 32);
        List<ItemStack> transaction = inputCopy();
        if (!consume(transaction, emeralds)) { bribeTicks = 0; status = "gui.gashs_easy_villager_addon.status.waiting_emeralds"; return; }
        bribeTicks += 20;
        if (bribeTicks >= 400) {
            commitInput(transaction);
            // Vanilla add() discards new gossip below 2. Seed the owner entry, then
            // store exactly +1 in its live map so the first paid cycle counts.
            var entries = v.getGossips().getGossipEntries().get(owner);
            if (entries == null) {
                v.getGossips().add(owner, GossipType.MINOR_POSITIVE, 2);
                entries = v.getGossips().getGossipEntries().get(owner);
            }
            entries.put(GossipType.MINOR_POSITIVE, reputation + 1);
            bribeTicks = 0;
            saveVillagerEntity();
        }
        status = "gui.gashs_easy_villager_addon.status.bribing";
        setChanged();
    }

    private void leveler(EasyVillagerEntity v) {
        if (targetResult.isEmpty() || !hasWorkstation()) { status = "gui.gashs_easy_villager_addon.select_target_help"; return; }
        boolean found = v.getOffers().stream().anyMatch(this::matchesTarget);
        if (found && Platform.villagerLevel(v) >= targetLevel) {
            enabled = false; status = "gui.gashs_easy_villager_addon.status.target_found"; setChanged(); return;
        }
        if (Platform.villagerLevel(v) >= targetLevel) {
            resetProfession(true);
            attempts++;
            status = "gui.gashs_easy_villager_addon.status.retry";
            setChanged(); return;
        }
        for (MerchantOffer offer : v.getOffers()) {
            if (offer.isOutOfStock() || offer.getXp() <= 0) continue;
            List<ItemStack> input = inputCopy();
            List<ItemStack> output = outputCopy();
            if (!consume(input, offer.getCostA()) || !consume(input, offer.getCostB())
                    || !insert(output, offer.getResult())) continue;
            commitInput(input);
            for (int i = 0; i < 9; i++) inventory.set(18 + i, output.get(i));
            offer.increaseUses();
            v.setVillagerXp(v.getVillagerXp() + offer.getXp());
            if (Platform.canLevelUp(v)) Platform.levelUp(v);
            saveVillagerEntity();
            status = "gui.gashs_easy_villager_addon.status.trading";
            setChanged(); return;
        }
        status = "gui.gashs_easy_villager_addon.status.waiting_items";
    }

    private boolean matchesTarget(MerchantOffer offer) {
        return Platform.sameStack(targetResult, offer.getResult()) && Platform.sameStack(targetCostA, offer.getBaseCostA())
                && Platform.sameStack(targetCostB, offer.getCostB());
    }

    private List<ItemStack> inputCopy() { return copyRange(9); }
    private List<ItemStack> outputCopy() { return copyRange(18); }
    private List<ItemStack> copyRange(int start) {
        List<ItemStack> result = new ArrayList<>();
        for (int i = start; i < start + 9; i++) result.add(inventory.get(i).copy());
        return result;
    }
    private void commitInput(List<ItemStack> items) { for (int i = 0; i < 9; i++) inventory.set(9 + i, items.get(i)); }

    static boolean consume(List<ItemStack> inventory, ItemStack cost) {
        int remaining = cost.getCount();
        if (cost.isEmpty()) return true;
        for (ItemStack stack : inventory) {
            if (Platform.sameStack(stack, cost)) {
                int amount = Math.min(remaining, stack.getCount());
                stack.shrink(amount); remaining -= amount;
                if (remaining == 0) return true;
            }
        }
        return false;
    }

    static boolean insert(List<ItemStack> inventory, ItemStack result) {
        int remaining = result.getCount();
        for (int pass = 0; pass < 2; pass++) for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.get(i);
            if ((pass == 0 && !stack.isEmpty() && Platform.sameStack(stack, result)) || (pass == 1 && stack.isEmpty())) {
                int amount = Math.min(remaining, result.getMaxStackSize() - stack.getCount());
                if (amount <= 0) continue;
                inventory.set(i, result.copyWithCount(stack.getCount() + amount)); remaining -= amount;
                if (remaining == 0) return true;
            }
        }
        return false;
    }

    public void dropContents() {
        if (dropped || level == null || level.isClientSide()) return;
        dropped = true;
        ItemStack stored = removeVillager();
        Containers.dropItemStack(level, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, stored);
        if (hasWorkstation()) {
            Containers.dropItemStack(level, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, new ItemStack(removeWorkstation()));
        }
        for (int i = 9; i < 27; i++) {
            Containers.dropItemStack(level, worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5, inventory.get(i));
            inventory.set(i, ItemStack.EMPTY);
        }
    }

    boolean accessible(Player player) {
        return !isRemoved() && level.getBlockEntity(worldPosition) == this && player.distanceToSqr(worldPosition.getX()+0.5,
                worldPosition.getY()+0.5, worldPosition.getZ()+0.5) <= 64;
    }

    @Override public int getContainerSize() { return inventory.size(); }
    @Override public boolean isEmpty() { return inventory.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return inventory.get(slot); }
    @Override public ItemStack removeItem(int slot, int count) {
        ItemStack removed = net.minecraft.world.ContainerHelper.removeItem(inventory, slot, count);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = net.minecraft.world.ContainerHelper.takeItem(inventory, slot);
        setChanged(); return removed;
    }
    @Override public void setItem(int slot, ItemStack stack) { inventory.set(slot, stack); setChanged(); }
    @Override public boolean stillValid(Player player) { return accessible(player); }
    @Override public void clearContent() { inventory.clear(); setChanged(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) {
        return slot >= 9 && slot < 18 && (kind() != MachineKind.BRIBER || stack.is(Items.EMERALD));
    }
    @Override public int[] getSlotsForFace(net.minecraft.core.Direction face) {
        return java.util.stream.IntStream.range(face == net.minecraft.core.Direction.DOWN ? 18 : 9,
                face == net.minecraft.core.Direction.DOWN ? 27 : 18).toArray();
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction face) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction face) { return slot >= 18 && slot < 27; }
}
