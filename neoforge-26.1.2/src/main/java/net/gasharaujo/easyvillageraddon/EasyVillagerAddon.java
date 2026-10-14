package net.gasharaujo.easyvillageraddon;

import net.neoforged.fml.common.Mod;

@Mod(EasyVillagerAddon.MOD_ID)
public final class EasyVillagerAddon {
    public static final String MOD_ID = "gashs_easy_villager_addon";
    public EasyVillagerAddon(net.neoforged.bus.api.IEventBus bus) { AddonRegistry.register(bus); }
}
