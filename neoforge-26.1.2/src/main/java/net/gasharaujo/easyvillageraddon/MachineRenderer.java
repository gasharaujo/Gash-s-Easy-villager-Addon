package net.gasharaujo.easyvillageraddon;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import de.maxhenkel.easyvillagers.blocks.tileentity.render.TraderRendererBase;
import de.maxhenkel.easyvillagers.blocks.tileentity.render.TraderRenderState;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.entity.ZombieVillagerRenderer;
import net.minecraft.client.renderer.entity.state.ZombieVillagerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.phys.Vec3;

public final class MachineRenderer extends TraderRendererBase<MachineEntity> {
    private ZombieVillager zombie;
    private ZombieVillagerRenderer zombieRenderer;

    public MachineRenderer(EntityModelSet models, BlockModelResolver blocks) { super(models, blocks); }
    public static final class State extends TraderRenderState {
        boolean converter;
        boolean renderZombie;
        final ZombieVillagerRenderState zombie = new ZombieVillagerRenderState();
    }
    @Override public State createRenderState() { return new State(); }

    @Override public void extractRenderState(MachineEntity machine, TraderRenderState renderState, float partialTicks, Vec3 position, ModelFeatureRenderer.CrumblingOverlay overlay) {
        super.extractRenderState(machine, renderState, partialTicks, position, overlay);
        State state = (State) renderState;
        state.converter = machine.kind() == MachineKind.DESNEURALIZER;
        state.renderZombie = state.converter && minecraft.level != null;
        if (!state.renderZombie) return;
        if (zombie == null || zombie.level() != minecraft.level) zombie = new ZombieVillager(EntityType.ZOMBIE_VILLAGER, minecraft.level);
        if (zombieRenderer == null) zombieRenderer = new ZombieVillagerRenderer(createEntityRenderer());
        zombieRenderer.extractRenderState(zombie, state.zombie, partialTicks);
        state.zombie.lightCoords = getLightOrDefault(machine, state);
    }

    @Override public void submit(TraderRenderState renderState, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        State state = (State) renderState;
        if (!state.converter) { super.submit(state, pose, collector, camera); return; }
        if (state.renderVillager) {
            pose.pushPose();
            pose.translate(0.5D, 1D / 16D, 0.5D);
            pose.mulPose(Axis.YP.rotationDegrees(-state.direction.toYRot()));
            pose.translate(-5D / 16D, 0D, 0D);
            pose.mulPose(Axis.YP.rotationDegrees(90));
            pose.scale(0.4F, 0.4F, 0.4F);
            getVillagerRenderer().submit(state.villagerRenderState, pose, collector, camera);
            pose.popPose();
        }
        if (state.renderZombie) {
            pose.pushPose();
            pose.translate(0.5D, 1D / 16D, 0.5D);
            pose.mulPose(Axis.YP.rotationDegrees(-state.direction.toYRot()));
            pose.translate(5D / 16D, 0D, 0D);
            pose.mulPose(Axis.YP.rotationDegrees(-90));
            pose.scale(0.4F, 0.4F, 0.4F);
            zombieRenderer.submit(state.zombie, pose, collector, camera);
            pose.popPose();
        }
    }
}
