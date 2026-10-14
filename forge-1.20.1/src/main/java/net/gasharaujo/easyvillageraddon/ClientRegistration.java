package net.gasharaujo.easyvillageraddon;
public final class ClientRegistration {
    static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
        if (net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) ClientOnly.register(bus);
    }
    private static final class ClientOnly {
        static void register(net.minecraftforge.eventbus.api.IEventBus bus) {
            bus.addListener((net.minecraftforge.client.event.EntityRenderersEvent.RegisterRenderers event) -> event.registerBlockEntityRenderer(AddonRegistry.MACHINE.get(), MachineRenderer::new));
            bus.addListener((net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent event) ->
                event.enqueueWork(() -> net.minecraft.client.gui.screens.MenuScreens.register(AddonRegistry.MENU.get(), net.gasharaujo.easyvillageraddon.client.MachineScreen::new)));
        }
    }
}
