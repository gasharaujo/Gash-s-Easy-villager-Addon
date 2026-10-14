package net.gasharaujo.easyvillageraddon;

import de.maxhenkel.easyvillagers.blocks.tileentity.TraderTileentityBase;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import java.util.UUID;

abstract class VersionedMachineEntity extends TraderTileentityBase {
    protected final NonNullList<ItemStack> inventory = NonNullList.withSize(27, ItemStack.EMPTY);
    protected ItemStack targetResult = ItemStack.EMPTY;
    protected ItemStack targetCostA = ItemStack.EMPTY;
    protected ItemStack targetCostB = ItemStack.EMPTY;
    protected final java.util.List<TradeTarget> targets = new java.util.ArrayList<>();
    protected boolean matchAll;
    protected UUID owner;
    protected int targetLevel = 1;
    protected int bribeTicks;
    protected int attempts;
    protected boolean enabled;

    VersionedMachineEntity(BlockPos pos, BlockState state) {
        super(AddonRegistry.MACHINE.get(), state, pos, state);
    }
    public void saveVillagerEntity() {
        if (villagerEntity != null) {
            // Easy Villagers 1.20.1 reuses its compound; clear stale Offers after a reset.
            villager.removeTagKey("villager");
            de.maxhenkel.easyvillagers.items.ModItems.VILLAGER.get().setVillager(villager, villagerEntity);
        }
    }
    @Override protected void saveAdditional(net.minecraft.nbt.CompoundTag tag) {
        super.saveAdditional(tag);
        net.minecraft.world.ContainerHelper.saveAllItems(tag, inventory);
        TradeTarget.LIST_CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE, targets).result().ifPresent(data -> tag.put("Targets", data)); tag.putBoolean("MatchAll", matchAll);
        if (owner != null) tag.putString("Owner", owner.toString());
        if (!targetResult.isEmpty()) tag.put("TargetResult", targetResult.save(new net.minecraft.nbt.CompoundTag()));
        if (!targetCostA.isEmpty()) tag.put("TargetCostA", targetCostA.save(new net.minecraft.nbt.CompoundTag()));
        if (!targetCostB.isEmpty()) tag.put("TargetCostB", targetCostB.save(new net.minecraft.nbt.CompoundTag()));
        tag.putInt("TargetLevel", targetLevel); tag.putInt("BribeTicks", bribeTicks);
        tag.putInt("Attempts", attempts); tag.putBoolean("Enabled", enabled);
    }
    @Override public void load(net.minecraft.nbt.CompoundTag tag) {
        super.load(tag);
        net.minecraft.world.ContainerHelper.loadAllItems(tag, inventory);
        targets.clear();
        if (tag.contains("Targets")) targets.addAll(TradeTarget.LIST_CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE, tag.get("Targets")).result().orElse(java.util.List.of()).stream().limit(TradeTarget.MAX_TARGETS).toList());
        matchAll = tag.getBoolean("MatchAll");
        owner = parseOwner(tag.getString("Owner"));
        targetResult = ItemStack.of(tag.getCompound("TargetResult"));
        targetCostA = ItemStack.of(tag.getCompound("TargetCostA"));
        targetCostB = ItemStack.of(tag.getCompound("TargetCostB"));
        targetLevel = Math.max(1, Math.min(5, tag.getInt("TargetLevel")));
        bribeTicks = Math.max(0, Math.min(399, tag.getInt("BribeTicks")));
        attempts = tag.getInt("Attempts"); enabled = tag.getBoolean("Enabled");
    }
    private static UUID parseOwner(String text) {
        try { return UUID.fromString(text); } catch (IllegalArgumentException ignored) { return null; }
    }
}
