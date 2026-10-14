package net.gasharaujo.easyvillageraddon;

import de.maxhenkel.easyvillagers.items.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/** Local client-render verification scene, never included in release jars. */
public final class VerificationScene {
    public static void commands(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!Boolean.getBoolean("addon.uiVerification")) return;
        var root = Commands.literal("addon_ui_test");
        for (MachineKind kind : MachineKind.values()) root.then(Commands.literal(kind.id).executes(context -> {
            open(context.getSource().getPlayerOrException(), kind); return 1;
        }));
        root.then(Commands.literal("gallery").executes(context -> { gallery(context.getSource().getPlayerOrException()); return 1; }));
        for (MachineKind kind : MachineKind.values()) root.then(Commands.literal("model_" + kind.id).executes(context -> {
            var player = context.getSource().getPlayerOrException();
            player.setYRot(135); player.setXRot(25);
            player.teleportTo(kind.ordinal() * 2 + 2.3, 100, 2.3); return 1;
        }));
        root.then(Commands.literal("stop").executes(context -> { context.getSource().getServer().halt(false); return 1; }));
        dispatcher.register(root);
    }
    private static void gallery(ServerPlayer player) {
        player.closeContainer();
        var world = (net.minecraft.server.level.ServerLevel) player.level();
        world.getServer().getCommands().performPrefixedCommand(world.getServer().createCommandSourceStack().withSuppressedOutput(), "time set noon");
        for (int x = -3; x <= 11; x++) for (int z = -3; z <= 8; z++) {
            world.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.SMOOTH_STONE.defaultBlockState());
            for (int y = 100; y <= 104; y++) world.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
        world.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,
                new net.minecraft.world.phys.AABB(-4, 98, -4, 13, 106, 10)).forEach(net.minecraft.world.entity.Entity::discard);
        player.getInventory().clearContent();
        for (MachineKind kind : MachineKind.values()) {
            BlockPos pos = new BlockPos(kind.ordinal() * 2, 100, 0);
            world.setBlockAndUpdate(pos, AddonRegistry.MACHINES.get(kind).get().defaultBlockState().setValue(MachineBlock.FACING, net.minecraft.core.Direction.SOUTH));
            var machine = (MachineEntity) world.getBlockEntity(pos);
            machine.owner = player.getUUID();
            if (kind != MachineKind.DESNEURALIZER) machine.setVillager(new ItemStack(ModItems.VILLAGER.get()));
            if (kind == MachineKind.ADVANCED_TRADER || kind == MachineKind.LEVELER) machine.setWorkstation(Blocks.LECTERN);
            machine.setChanged();
            player.getInventory().add(new ItemStack(AddonRegistry.MACHINES.get(kind).get()));
        }
        player.setYRot(180); player.setXRot(12);
        player.teleportTo(3.5, 100, 5.8);
    }
    private static void open(ServerPlayer player, MachineKind kind) {
        player.closeContainer();
        var world = (net.minecraft.server.level.ServerLevel) player.level();
        BlockPos pos = new BlockPos(kind.ordinal() * 3, 100, 0);
        world.getChunkAt(pos);
        for (int dx = -2; dx <= 3; dx++) for (int dz = -2; dz <= 4; dz++) world.setBlockAndUpdate(pos.offset(dx, -1, dz), Blocks.STONE.defaultBlockState());
        world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        world.setBlockAndUpdate(pos, AddonRegistry.MACHINES.get(kind).get().defaultBlockState());
        MachineEntity machine = (MachineEntity) world.getBlockEntity(pos);
        machine.owner = player.getUUID();
        machine.setVillager(new ItemStack(ModItems.VILLAGER.get()));
        if (kind != MachineKind.BRIBER) machine.setWorkstation(kind == MachineKind.LEVELER ? Blocks.SMITHING_TABLE : Blocks.LECTERN);
        if (kind == MachineKind.DESNEURALIZER) {
            machine.getVillagerEntity().setVillagerXp(100);
            var queued = machine.removeVillager();
            for (int i = 9; i < 13; i++) machine.inventory.set(i, queued.copy());
        }
        if (kind == MachineKind.BRIBER) { machine.inventory.set(9, new ItemStack(Items.EMERALD, 64)); machine.bribeTicks = 120; }
        if (kind == MachineKind.LEVELER) {
            machine.targetLevel = 5;
            machine.rebuildCatalog();
            while (!machine.catalog.complete()) machine.catalog.step();
            machine.inventory.set(9, new ItemStack(Items.EMERALD, 64));
            machine.inventory.set(10, new ItemStack(Items.COAL, 64));
            machine.inventory.set(11, new ItemStack(Items.COAL, 64));
        }
        player.teleportTo(pos.getX() + 1, 100, 3);
        player.getInventory().clearContent();
        player.getInventory().add(new ItemStack(Items.EMERALD, 64));
        player.getInventory().add(new ItemStack(Items.BOOK, 64));
        player.getInventory().add(new ItemStack(Items.PAPER, 64));
        machine.open(player);
    }
}
