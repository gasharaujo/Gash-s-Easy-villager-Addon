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
    int targetsRevision;

    public MachineEntity(BlockPos pos, BlockState state) { super(pos, state); }
    public MachineKind kind() { return ((MachineBlock) getBlockState().getBlock()).kind; }
    @Override protected void onAddVillager(EasyVillagerEntity villager) {
        if (kind() == MachineKind.ADVANCED_TRADER || kind() == MachineKind.LEVELER) super.onAddVillager(villager);
    }

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

    public void interact(Player player, ItemStack held) { interact(player, held, net.minecraft.world.InteractionHand.MAIN_HAND); }
    public void interact(Player player, ItemStack held, net.minecraft.world.InteractionHand hand) {
        if (level.isClientSide()) return;
        if (player.isShiftKeyDown() && kind() != MachineKind.DESNEURALIZER && (hasVillager() || hasWorkstation())) {
            if (viewer != null && level instanceof net.minecraft.server.level.ServerLevel server) {
                var other = server.getServer().getPlayerList().getPlayer(viewer);
                if (other != null && other.containerMenu instanceof MachineMenu menu && menu.machine == this) other.closeContainer();
            }
            ItemStack removed = hasVillager() ? removeVillager() : new ItemStack(removeWorkstation());
            invalidateCatalog();
            if (held.isEmpty()) player.setItemInHand(hand, removed);
            else if (!player.getInventory().add(removed)) player.drop(removed, false);
            level.playSound(null, worldPosition, net.minecraft.sounds.SoundEvents.VILLAGER_CELEBRATE, net.minecraft.sounds.SoundSource.BLOCKS, 1F, 1F);
            return;
        }
        if (kind() == MachineKind.DESNEURALIZER && held.getItem() instanceof VillagerItem) {
            for (int i = 9; i < 13; i++) if (inventory.get(i).isEmpty()) {
                inventory.set(i, held.copyWithCount(1)); held.shrink(1); setChanged(); return;
            }
            open(player);
        } else if (!hasVillager() && held.getItem() instanceof VillagerItem) {
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
        if (kind() != MachineKind.ADVANCED_TRADER && kind() != MachineKind.LEVELER) { catalog = null; return; }
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
            // Select a goal, never insert an offer from the preview catalog. Each
            // attempt below regenerates the villager's entire natural offer set.
            targetResult = choice.getResult().copy();
            targetCostA = choice.getBaseCostA().copy(); targetCostB = choice.getCostB().copy();
            attempts = 0; enabled = true;
            status = "gui.gashs_easy_villager_addon.status.rerolling";
        } else {
            targets.clear(); targets.add(TradeTarget.of(targetLevel, false, choice)); syncLegacyTarget(); targetsRevision++;
            enabled = false;
            status = "gui.gashs_easy_villager_addon.status.target_selected";
        }
        setChanged();
        return true;
    }

    @Override public void tickServer() {
        if (kind() == MachineKind.DESNEURALIZER) { desneuralizeQueue(); return; }
        if (catalog != null && !catalog.complete()) catalog.step();
        EasyVillagerEntity v = getVillagerEntity();
        if (v == null) return;
        if (kind() == MachineKind.ADVANCED_TRADER && !v.isBaby() && enabled) rerollInitial(v);
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
            case BRIBER -> bribe(v);
            case LEVELER -> { if (enabled) leveler(v); }
            default -> { }
        }
    }

    private void rerollInitial(EasyVillagerEntity v) {
        if (v.getVillagerXp() != 0 || Platform.villagerLevel(v) != 1 || targetResult.isEmpty()) { enabled = false; return; }
        // A bounded amount of work per tick, even for very rare custom trades.
        for (int budget = 0; budget < 32; budget++) {
            if (v.getOffers().stream().anyMatch(this::matchesTarget)) {
                enabled = false; status = "gui.gashs_easy_villager_addon.status.initial_selected";
                saveVillagerEntity(); setChanged(); return;
            }
            v.setOffers(null);
            v.recalculateOffers();
            attempts++;
        }
        setChanged();
    }

    private void desneuralizeQueue() {
        int output = -1;
        for (int i = 18; i < 22; i++) if (inventory.get(i).isEmpty()) { output = i; break; }
        if (output < 0) { status = "gui.gashs_easy_villager_addon.status.output_full"; return; }
        if (!hasVillager()) {
            for (int i = 9; i < 13; i++) if (inventory.get(i).getItem() instanceof VillagerItem) {
                setVillager(inventory.get(i).split(1)); cycleTicks = 0; setChanged(); break;
            }
        }
        if (!hasVillager()) return;
        if (++cycleTicks < 20) return;
        var v = getVillagerEntity();
        if (v == null) return;
        Platform.clearProfession(v);
        saveVillagerEntity();
        inventory.set(output, removeVillager());
        cycleTicks = 0; status = "gui.gashs_easy_villager_addon.status.profession_removed";
        setChanged();
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
        var goals = effectiveTargets();
        if (goals.isEmpty() || !hasWorkstation()) { status = "gui.gashs_easy_villager_addon.select_target_help"; return; }
        if (targetsSatisfied(v.getOffers(), Platform.villagerLevel(v))) {
            enabled = false; status = "gui.gashs_easy_villager_addon.status.target_found"; setChanged(); return;
        }
        int highestLevel = goals.stream().mapToInt(TradeTarget::level).max().orElse(1);
        if (Platform.villagerLevel(v) >= highestLevel) {
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

    public List<TradeTarget> effectiveTargets() {
        if (kind() != MachineKind.LEVELER) return List.of();
        if (!targets.isEmpty()) return List.copyOf(targets);
        return targetResult.isEmpty() ? List.of() : List.of(new TradeTarget(targetLevel, false, targetResult.copy(), targetCostA.copy(), targetCostB.copy()));
    }
    void migrateTargets() { if (targets.isEmpty()) targets.addAll(effectiveTargets()); }
    private void syncLegacyTarget() {
        targetResult = targets.isEmpty() ? ItemStack.EMPTY : targets.get(0).result().copy();
        targetCostA = targets.isEmpty() ? ItemStack.EMPTY : targets.get(0).costA().copy();
        targetCostB = targets.isEmpty() ? ItemStack.EMPTY : targets.get(0).costB().copy();
    }
    boolean targetsSatisfied(List<MerchantOffer> offers, int level) {
        var goals = effectiveTargets();
        if (goals.isEmpty()) return false;
        java.util.function.Predicate<TradeTarget> found = goal -> level >= goal.level() && offers.stream().anyMatch(goal::matches);
        return matchAll ? goals.stream().allMatch(found) : goals.stream().anyMatch(found);
    }
    void toggleTarget(MerchantOffer offer, boolean generic) {
        migrateTargets(); var goal = TradeTarget.of(targetLevel, generic, offer);
        int existing = -1;
        for (int i = 0; i < targets.size(); i++) if (targets.get(i).sameDefinition(goal)) { existing = i; break; }
        if (existing >= 0) targets.remove(existing);
        else if (targets.size() < TradeTarget.MAX_TARGETS) targets.add(goal);
        else { status = "gui.gashs_easy_villager_addon.status.target_limit"; return; }
        targetsChanged();
    }
    void removeTarget(int index) { migrateTargets(); if (index >= 0 && index < targets.size()) { targets.remove(index); targetsChanged(); } }
    void clearTargets() { targets.clear(); targetsChanged(); }
    void targetsChanged() {
        syncLegacyTarget(); enabled = false; targetsRevision++; setChanged();
        status = "gui.gashs_easy_villager_addon.status.target_selected";
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
        if (kind() == MachineKind.DESNEURALIZER) return slot >= 9 && slot < 13 && stack.getItem() instanceof VillagerItem;
        if (kind() == MachineKind.ADVANCED_TRADER) return false;
        return slot >= 9 && slot < 18 && (kind() != MachineKind.BRIBER || stack.is(Items.EMERALD));
    }
    @Override public int[] getSlotsForFace(net.minecraft.core.Direction face) {
        if (kind() == MachineKind.ADVANCED_TRADER) return new int[0];
        boolean output = face == net.minecraft.core.Direction.DOWN;
        int start = output ? 18 : 9;
        return java.util.stream.IntStream.range(start, start + (kind() == MachineKind.DESNEURALIZER ? 4 : 9)).toArray();
    }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction face) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, net.minecraft.core.Direction face) { return slot >= 18 && slot < 27; }
}
