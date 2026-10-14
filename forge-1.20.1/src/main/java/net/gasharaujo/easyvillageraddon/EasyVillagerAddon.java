package net.gasharaujo.easyvillageraddon;

import net.minecraftforge.fml.common.Mod;

@Mod(EasyVillagerAddon.MOD_ID)
public final class EasyVillagerAddon {
    public static final String MOD_ID = "gashs_easy_villager_addon";
    public EasyVillagerAddon() { AddonRegistry.register(net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext.get().getModEventBus()); }
}
