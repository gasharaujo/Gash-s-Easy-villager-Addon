package net.gasharaujo.easyvillageraddon;
public final class ClientRegistration {
    static void register(net.neoforged.bus.api.IEventBus bus) {
        if (net.neoforged.fml.loading.FMLEnvironment.getDist().isClient()) ClientOnly.register(bus);
    }
    private static final class ClientOnly {
        static void register(net.neoforged.bus.api.IEventBus bus) {
            bus.addListener((net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers event) ->
                event.registerBlockEntityRenderer(AddonRegistry.MACHINE.get(), context -> new MachineRenderer(context.entityModelSet(), context.blockModelResolver())));
            bus.addListener((net.neoforged.neoforge.client.event.RegisterSpecialModelRendererEvent event) ->
                event.register(net.minecraft.resources.Identifier.fromNamespaceAndPath(EasyVillagerAddon.MOD_ID, "desneuralizer"), DesneuralizerSpecialRenderer.Unbaked.MAP_CODEC));
            bus.addListener((net.neoforged.neoforge.client.event.RegisterMenuScreensEvent event) ->
                event.register(AddonRegistry.MENU.get(), net.gasharaujo.easyvillageraddon.client.MachineScreen::new));
        }
    }
}
