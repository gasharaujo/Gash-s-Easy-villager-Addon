package net.gasharaujo.easyvillageraddon;

import de.maxhenkel.easyvillagers.items.render.BlockItemRendererBase;
import net.minecraft.core.BlockPos;

public final class MachineItemRenderer extends BlockItemRendererBase<MachineRenderer, MachineEntity> {
    public MachineItemRenderer(java.util.function.Supplier<net.minecraft.world.level.block.Block> block) {
        super(MachineRenderer::new, () -> new MachineEntity(BlockPos.ZERO, block.get().defaultBlockState()));
    }
}
