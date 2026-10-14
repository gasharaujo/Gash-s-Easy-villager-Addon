package net.gasharaujo.easyvillageraddon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.maxhenkel.easyvillagers.blocks.tileentity.render.TraderRenderer;
import de.maxhenkel.easyvillagers.blocks.tileentity.render.VillagerRendererBase;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.ZombieVillager;

public final class MachineRenderer extends VillagerRendererBase<MachineEntity> {
    private ZombieVillager zombie;
    private ZombieVillagerRenderer zombieRenderer;

    public MachineRenderer(BlockEntityRendererProvider.Context context) { super(context); }

    @Override public void render(MachineEntity machine, float partialTicks, PoseStack pose, MultiBufferSource buffer, int light, int overlay) {
        super.render(machine, partialTicks, pose, buffer, light, overlay);
        if (machine.kind() == MachineKind.BRIBER) {
            if (machine.getVillagerEntity() == null) return;
            var direction = machine.isFakeWorld() ? net.minecraft.core.Direction.SOUTH : machine.getBlockState().getValue(MachineBlock.FACING);
            pose.pushPose();
            pose.translate(0.5D, 1D / 16D, 0.5D);
            pose.mulPose(Axis.YP.rotationDegrees(-direction.toYRot()));
            pose.scale(0.45F, 0.45F, 0.45F);
            villagerRenderer.render(machine.getVillagerEntity(), 0F, 1F, pose, buffer, light);
            pose.popPose();
            return;
        }
        if (machine.kind() != MachineKind.DESNEURALIZER) {
            TraderRenderer.renderTraderBase(villagerRenderer, machine, partialTicks, pose, buffer, light, overlay);
            return;
        }
        if (minecraft.level == null) return;
        if (zombie == null || zombie.level() != minecraft.level) zombie = new ZombieVillager(EntityType.ZOMBIE_VILLAGER, minecraft.level);
        if (zombieRenderer == null) zombieRenderer = new ZombieVillagerRenderer(createEntityRenderer());
        float rotation = -machine.getBlockState().getValue(MachineBlock.FACING).toYRot();
        var villager = machine.getVillagerEntity();
        if (villager != null) {
            pose.pushPose();
            pose.translate(0.5D, 1D / 16D, 0.5D);
            pose.mulPose(Axis.YP.rotationDegrees(rotation));
            pose.translate(-5D / 16D, 0D, 0D);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.scale(0.4F, 0.4F, 0.4F);
            villagerRenderer.render(villager, 0F, 1F, pose, buffer, light);
            pose.popPose();
        }
        pose.pushPose();
        pose.translate(0.5D, 1D / 16D, 0.5D);
        pose.mulPose(Axis.YP.rotationDegrees(rotation));
        pose.translate(5D / 16D, 0D, 0D);
        pose.mulPose(Axis.YP.rotationDegrees(-90));
        pose.scale(0.4F, 0.4F, 0.4F);
        zombieRenderer.render(zombie, 0F, 1F, pose, buffer, light);
        pose.popPose();
    }
}
