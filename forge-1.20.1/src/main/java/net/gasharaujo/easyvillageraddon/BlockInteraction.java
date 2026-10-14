package net.gasharaujo.easyvillageraddon;

/** Match Easy Villagers' empty-main-hand crouch override, including an occupied offhand. */
final class BlockInteraction {
    static void register() { net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener(BlockInteraction::rightClick); }
    private static void rightClick(net.minecraftforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().getBlockState(event.getPos()).getBlock() instanceof MachineBlock block
                && block.kind != MachineKind.DESNEURALIZER && event.getEntity().isShiftKeyDown()
                && event.getEntity().getMainHandItem().isEmpty()) event.setUseBlock(net.minecraftforge.eventbus.api.Event.Result.ALLOW);
    }
}
