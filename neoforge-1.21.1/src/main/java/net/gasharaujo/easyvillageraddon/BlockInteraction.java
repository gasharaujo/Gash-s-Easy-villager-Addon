package net.gasharaujo.easyvillageraddon;

/** Match Easy Villagers' empty-main-hand crouch override, including an occupied offhand. */
final class BlockInteraction {
    static void register() { net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(BlockInteraction::rightClick); }
    private static void rightClick(net.neoforged.neoforge.event.entity.player.PlayerInteractEvent.RightClickBlock event) {
        if (event.getLevel().getBlockState(event.getPos()).getBlock() instanceof MachineBlock block
                && block.kind != MachineKind.DESNEURALIZER && event.getEntity().isShiftKeyDown()
                && event.getEntity().getMainHandItem().isEmpty()) event.setUseBlock(net.neoforged.neoforge.common.util.TriState.TRUE);
    }
}
