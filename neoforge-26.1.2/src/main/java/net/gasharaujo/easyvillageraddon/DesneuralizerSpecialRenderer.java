package net.gasharaujo.easyvillageraddon;

import com.mojang.serialization.MapCodec;
import de.maxhenkel.easyvillagers.blocks.tileentity.render.TraderRenderState;
import de.maxhenkel.easyvillagers.items.render.ItemSpecialRendererBase;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

public final class DesneuralizerSpecialRenderer extends ItemSpecialRendererBase<MachineEntity, TraderRenderState> {
    private MachineEntity preview;
    private DesneuralizerSpecialRenderer(SpecialModelRenderer.BakingContext context) {
        super(() -> AddonRegistry.MACHINES.get(MachineKind.DESNEURALIZER).get().defaultBlockState(), MachineEntity.class);
        renderer = new MachineRenderer(context.entityModelSet(), Minecraft.getInstance().getBlockModelResolver());
    }
    @Override public MachineEntity extractArgument(ItemStack stack) {
        if (preview == null) preview = new MachineEntity(BlockPos.ZERO, blockSupplier.get());
        preview.setFakeWorld(minecraft.level);
        return preview;
    }
    public static final class Unbaked implements SpecialModelRenderer.Unbaked<MachineEntity> {
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(Unbaked::new);
        @Override public SpecialModelRenderer<MachineEntity> bake(SpecialModelRenderer.BakingContext context) { return new DesneuralizerSpecialRenderer(context); }
        @Override public MapCodec<Unbaked> type() { return MAP_CODEC; }
    }
}
