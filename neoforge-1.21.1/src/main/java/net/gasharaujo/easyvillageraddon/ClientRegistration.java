package net.gasharaujo.easyvillageraddon;
public final class ClientRegistration {
    static void register(net.neoforged.bus.api.IEventBus bus) {
        if (net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) ClientOnly.register(bus);
    }
    private static final class ClientOnly {
        static void register(net.neoforged.bus.api.IEventBus bus) {
            bus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                event.register(AddonRegistry.MENU.get(), net.gasharaujo.easyvillageraddon.client.MachineScreen::new));
        }
    }
}
