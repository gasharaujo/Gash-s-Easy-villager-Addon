package net.gasharaujo.easyvillageraddon;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

final class AddonRegistry {
    private static final String ID = EasyVillagerAddon.MOD_ID;
    static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(Registries.BLOCK, ID);
    static final DeferredRegister<Item> ITEMS = DeferredRegister.create(Registries.ITEM, ID);
    static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ID);
    static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ID);
    public static final Supplier<net.minecraft.world.inventory.MenuType<MachineMenu>> MENU = MENUS.register("machine", () -> net.minecraftforge.common.extensions.IForgeMenuType.create((id, inventory, data) -> new MachineMenu(id, inventory)));
    static final Map<MachineKind, Supplier<MachineBlock>> MACHINES = new LinkedHashMap<>();
    static final Supplier<BlockEntityType<MachineEntity>> MACHINE;
    static {
        for (MachineKind kind : MachineKind.values()) {
            Supplier<MachineBlock> block = BLOCKS.register(kind.id, () -> new MachineBlock(kind, BlockBehaviour.Properties.of().strength(3.0F).requiresCorrectToolForDrops()));
            MACHINES.put(kind, block);
            ITEMS.register(kind.id, () -> new BlockItem(block.get(), new Item.Properties()));
        }
        MACHINE = ENTITIES.register("machine", () -> BlockEntityType.Builder.of(MachineEntity::new, MACHINES.values().stream().map(Supplier::get).toArray(Block[]::new)).build(null));
    }
    static void register(IEventBus bus) {
        BLOCKS.register(bus);
        ITEMS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        UiNetwork.register(bus);
        ClientRegistration.register(bus);
        bus.addListener(AddonRegistry::creative);
    }
    private static void creative(net.minecraftforge.event.BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.FUNCTIONAL_BLOCKS) {
            MACHINES.values().forEach(block -> event.accept(block.get()));
        }
    }
}
