package net.gasharaujo.easyvillageraddon;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.event.server.ServerStartedEvent;
@Mod("gashs_easy_villager_addon_verification")
public final class VerificationMod {
    public VerificationMod() {
        if (Boolean.getBoolean("addon.uiVerification") && net.minecraftforge.fml.loading.FMLEnvironment.dist.isClient()) net.gasharaujo.easyvillageraddon.client.VerificationClient.install();
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.RegisterCommandsEvent event) -> VerificationScene.commands(event.getDispatcher()));
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addListener((ServerStartedEvent event) -> { if (!Boolean.getBoolean("addon.uiVerification")) VerificationChecks.run(event.getServer()); });
    }
    static void seedTrades(de.maxhenkel.easyvillagers.entity.EasyVillagerEntity v, long seed) { v.getRandom().setSeed(seed); }
    static net.minecraft.world.item.trading.MerchantOffer testOffer(net.minecraft.world.item.ItemStack result, net.minecraft.world.item.ItemStack cost) { return new net.minecraft.world.item.trading.MerchantOffer(cost, result, 12, 1, 0.05F); }
    static void interactionChecks(MachineEntity machine) {
        var world = (net.minecraft.server.level.ServerLevel) machine.getLevel();
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft(world);
        player.containerMenu = player.inventoryMenu;
        player.setPos(machine.getBlockPos().getX()+0.5, machine.getBlockPos().getY()+0.5, machine.getBlockPos().getZ()+0.5);
        player.getInventory().clearContent(); player.setShiftKeyDown(true);
        machine.getVillagerEntity().setVillagerXp(12);
        machine.getVillagerEntity().setCustomName(net.minecraft.network.chat.Component.literal("Shift test"));
        player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.IRON_PICKAXE));
        var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(machine.getBlockPos()), net.minecraft.core.Direction.UP, machine.getBlockPos(), false);
        player.gameMode.useItemOn(player, world, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        VerificationChecks.check(!machine.hasVillager() && machine.hasWorkstation(), "Native crouch-right-click withdraws villager before workstation with occupied offhand");
        var stored = player.getMainHandItem().copy();
        VerificationChecks.check(stored.getItem() instanceof de.maxhenkel.easyvillagers.items.VillagerItem && stored.getCount() == 1, "Withdrawn villager goes to the actual clicked hand");
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.ItemStack.EMPTY);
        player.gameMode.useItemOn(player, world, player.getMainHandItem(), net.minecraft.world.InteractionHand.MAIN_HAND, hit);
        VerificationChecks.check(!machine.hasWorkstation() && player.getMainHandItem().is(net.minecraft.world.level.block.Blocks.LECTERN.asItem()), "Next native crouch-right-click withdraws workstation");
        VerificationChecks.check(player.containerMenu == player.inventoryMenu, "Withdrawal does not open a GUI");
        machine.setVillager(stored);
        VerificationChecks.check(machine.getVillagerEntity().getVillagerXp() == 12 && machine.getVillagerEntity().getName().getString().equals("Shift test"), "Withdrawal preserves villager XP and identity");
        player.setShiftKeyDown(false); player.getInventory().clearContent();
    }
    static void menuChecks(MachineEntity machine) {
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((net.minecraft.server.level.ServerLevel) machine.getLevel());
        player.setPos(machine.getBlockPos().getX()+0.5, machine.getBlockPos().getY()+0.5, machine.getBlockPos().getZ()+0.5);
        player.getInventory().clearContent();
        var menu = new MachineMenu(1, player.getInventory(), machine);
        player.containerMenu = menu;
        VerificationChecks.check(menu.getType() == AddonRegistry.MENU.get(), "Dedicated addon menu type is registered");
        VerificationChecks.check(menu.slots.size() == 59, "Catalog previews never become inventory slots");
        VerificationChecks.check(menu.getSlot(2) instanceof net.minecraft.world.inventory.MerchantResultSlot, "Result uses the vanilla merchant transaction slot");
        VerificationChecks.check(!menu.getSlot(39).isActive(), "Identity controls are outside the clean trading view");
        menu.setPane(2);
        VerificationChecks.check(menu.getSlot(39).mayPlace(new net.minecraft.world.item.ItemStack(de.maxhenkel.easyvillagers.items.ModItems.VILLAGER.get())), "Villager slot accepts a real Easy Villagers item");
        VerificationChecks.check(!menu.getSlot(39).mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD)), "Villager slot rejects unrelated items");
        menu.setPane(0);
        VerificationChecks.check(!menu.getSlot(50).mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD)), "Output is extraction-only");
        var offer = menu.getOffers().get(0);
        menu.getSlot(0).set(offer.getCostA().copy());
        menu.getSlot(1).set(offer.getCostB().copy());
        menu.slotsChanged(menu.getSlot(0).container);
        VerificationChecks.check(Platform.sameStack(menu.getSlot(2).getItem(), offer.getResult()), "Paying both costs populates the real merchant result");
        int xp = machine.getVillagerEntity().getVillagerXp();
        menu.clicked(2, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        VerificationChecks.check(Platform.sameStack(menu.getCarried(), offer.getResult()), "Vanilla output click yields the purchased item");
        VerificationChecks.check(menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty(), "Vanilla output click consumes the exact payment");
        VerificationChecks.check(machine.getVillagerEntity().getVillagerXp() == xp + offer.getXp(), "Vanilla result grants the villager its trading XP");
        menu.setCarried(net.minecraft.world.item.ItemStack.EMPTY);
        menu.removed(player);
    }
    static void processorMenuChecks(MachineEntity machine) {
        var player = net.minecraftforge.common.util.FakePlayerFactory.getMinecraft((net.minecraft.server.level.ServerLevel) machine.getLevel());
        player.setPos(machine.getBlockPos().getX()+0.5, machine.getBlockPos().getY()+0.5, machine.getBlockPos().getZ()+0.5);
        var menu = new MachineMenu(2, player.getInventory(), machine);
        player.containerMenu = menu;
        VerificationChecks.check(!menu.manualTrading() && menu.getOffers().isEmpty(), "Processor has no manual trades: " + machine.kind());
        for (int i = 0; i < 3; i++) VerificationChecks.check(!menu.getSlot(i).isActive(), "Processor hides merchant slot " + i);
        VerificationChecks.check(!menu.getSlot(2).mayPickup(player), "Processor rejects crafted merchant output clicks");
        if (machine.kind() == MachineKind.DESNEURALIZER) {
            VerificationChecks.check(menu.getSlot(41).mayPlace(new net.minecraft.world.item.ItemStack(de.maxhenkel.easyvillagers.items.ModItems.VILLAGER.get())), "Queue input accepts villagers");
            VerificationChecks.check(!menu.getSlot(41).mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD)), "Queue input rejects emeralds");
            VerificationChecks.check(java.util.stream.IntStream.range(41,50).filter(i -> menu.getSlot(i).isActive()).count() == 4, "Incubator-style four input slots");
            VerificationChecks.check(java.util.stream.IntStream.range(50,59).filter(i -> menu.getSlot(i).isActive()).count() == 4, "Incubator-style four output slots");
        } else {
            menu.getSlot(41).set(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD, 17));
            VerificationChecks.check(machine.inventory.get(9).getCount() == 17, "Processor material slots use real machine inventory");
            if (machine.kind() == MachineKind.BRIBER) VerificationChecks.check(!menu.getSlot(41).mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.BOOK)), "Briber rejects non-emeralds");
        }
        VerificationChecks.check(!menu.getSlot(50).mayPlace(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.EMERALD)), "Processor output is extraction-only");
        menu.removed(player);
    }
    static void persistence(MachineEntity machine) {
        var saved = machine.saveWithFullMetadata();
        MachineEntity loaded = (MachineEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), saved);
        VerificationChecks.check(loaded != null, "Block entity type reloads");
        loaded.setLevel(machine.getLevel());
        VerificationChecks.check(machine.getVillagerEntity().getOffers().size() == loaded.getVillagerEntity().getOffers().size(), "Offers survive save/reload without stale trades");
        VerificationChecks.check(machine.getVillagerEntity().getGossips().getReputation(machine.owner, t -> true) == loaded.getVillagerEntity().getGossips().getReputation(machine.owner, t -> true), "Reputation survives save/reload");
        var beforeTargets = machine.effectiveTargets(); var afterTargets = loaded.effectiveTargets();
        VerificationChecks.check(machine.matchAll == loaded.matchAll && beforeTargets.size() == afterTargets.size(), "AND/OR mode and target count survive save/reload");
        for (int i = 0; i < beforeTargets.size(); i++) VerificationChecks.check(beforeTargets.get(i).sameDefinition(afterTargets.get(i)), "Target level, generic flag and item components survive save/reload");
        VerificationChecks.check(machine.owner.equals(loaded.owner), "Placer survives save/reload");
        VerificationChecks.check(machine.enabled == loaded.enabled && machine.targetLevel == loaded.targetLevel, "Automation state survives save/reload");
        VerificationChecks.check(Platform.sameStack(machine.targetResult, loaded.targetResult), "Target components survive save/reload");
        for (int i = 9; i < 27; i++) VerificationChecks.check(machine.inventory.get(i).getCount() == loaded.inventory.get(i).getCount()
                && Platform.sameStack(machine.inventory.get(i), loaded.inventory.get(i)), "Inventory slot " + i + " survives save/reload");
        VerificationChecks.check(machine.getVillagerEntity().getVillagerXp() == loaded.getVillagerEntity().getVillagerXp(), "Villager XP survives save/reload");
    }
}
