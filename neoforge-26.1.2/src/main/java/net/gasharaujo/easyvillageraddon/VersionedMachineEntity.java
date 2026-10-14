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
    protected UUID owner;
    protected int targetLevel = 1;
    protected int bribeTicks;
    protected int attempts;
    protected boolean enabled;

    VersionedMachineEntity(BlockPos pos, BlockState state) {
        super(AddonRegistry.MACHINE.get(), state, pos, state);
    }
    @Override protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput out) {
        super.saveAdditional(out);
        net.minecraft.world.ContainerHelper.saveAllItems(out, inventory);
        if (owner != null) out.putString("Owner", owner.toString());
        if (!targetResult.isEmpty()) out.store("TargetResult", ItemStack.CODEC, targetResult);
        if (!targetCostA.isEmpty()) out.store("TargetCostA", ItemStack.CODEC, targetCostA);
        if (!targetCostB.isEmpty()) out.store("TargetCostB", ItemStack.CODEC, targetCostB);
        out.putInt("TargetLevel", targetLevel); out.putInt("BribeTicks", bribeTicks);
        out.putInt("Attempts", attempts); out.putBoolean("Enabled", enabled);
    }
    @Override protected void loadAdditional(net.minecraft.world.level.storage.ValueInput in) {
        super.loadAdditional(in);
        net.minecraft.world.ContainerHelper.loadAllItems(in, inventory);
        owner = parseOwner(in.getStringOr("Owner", ""));
        targetResult = in.read("TargetResult", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        targetCostA = in.read("TargetCostA", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        targetCostB = in.read("TargetCostB", ItemStack.CODEC).orElse(ItemStack.EMPTY);
        targetLevel = Math.max(1, Math.min(5, in.getIntOr("TargetLevel", 1)));
        bribeTicks = Math.max(0, Math.min(399, in.getIntOr("BribeTicks", 0)));
        attempts = in.getIntOr("Attempts", 0); enabled = in.getBooleanOr("Enabled", false);
    }
    @Override public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        ((MachineEntity) this).dropContents(); super.preRemoveSideEffects(pos, state);
    }
    private static UUID parseOwner(String text) {
        try { return UUID.fromString(text); } catch (IllegalArgumentException ignored) { return null; }
    }
}
