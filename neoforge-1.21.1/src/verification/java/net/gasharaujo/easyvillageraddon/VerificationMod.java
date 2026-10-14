package net.gasharaujo.easyvillageraddon;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
@Mod("gashs_easy_villager_addon_verification")
public final class VerificationMod {
    public VerificationMod() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((ServerStartedEvent event) -> VerificationChecks.run(event.getServer()));
    }
    static void menuChecks(MachineEntity machine) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft((net.minecraft.server.level.ServerLevel) machine.getLevel());
        var menu = new MachineMenu(1, player.getInventory(), machine, "");
        player.containerMenu = menu;
        for (int i = 0; i < 10000 && !machine.catalog.complete(); i++) machine.catalog.step();
        menu.broadcastChanges();
        menu.clicked(27, 0, net.minecraft.world.inventory.ClickType.PICKUP, player);
        VerificationChecks.check(menu.getCarried().isEmpty(), "Catalog preview cannot be extracted");
        VerificationChecks.check(menu.quickMoveStack(player, 27).isEmpty(), "Catalog preview cannot be shift-clicked");
        var search = new SearchMenu(2, player.getInventory(), machine, "");
        player.containerMenu = search;
        search.setItemName("remendo");
        VerificationChecks.check("remendo".equals(machine.lastSearch.get(player.getUUID())), "Vanilla rename protocol applies search");
        search.removed(player);
        VerificationChecks.check(search.getCarried().isEmpty(), "Search UI does not mint ghost paper");
    }
    static void persistence(MachineEntity machine) {
        var saved = machine.saveWithFullMetadata(machine.getLevel().registryAccess());
        MachineEntity loaded = (MachineEntity) net.minecraft.world.level.block.entity.BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), saved, machine.getLevel().registryAccess());
        VerificationChecks.check(loaded != null, "Block entity type reloads");
        loaded.setLevel(machine.getLevel());
        VerificationChecks.check(machine.getVillagerEntity().getOffers().size() == loaded.getVillagerEntity().getOffers().size(), "Offers survive save/reload without stale trades");
        VerificationChecks.check(machine.getVillagerEntity().getGossips().getReputation(machine.owner, t -> true) == loaded.getVillagerEntity().getGossips().getReputation(machine.owner, t -> true), "Reputation survives save/reload");
        VerificationChecks.check(machine.owner.equals(loaded.owner), "Placer survives save/reload");
        VerificationChecks.check(machine.enabled == loaded.enabled && machine.targetLevel == loaded.targetLevel, "Automation state survives save/reload");
        VerificationChecks.check(Platform.sameStack(machine.targetResult, loaded.targetResult), "Target components survive save/reload");
        for (int i = 9; i < 27; i++) VerificationChecks.check(machine.inventory.get(i).getCount() == loaded.inventory.get(i).getCount()
                && Platform.sameStack(machine.inventory.get(i), loaded.inventory.get(i)), "Inventory slot " + i + " survives save/reload");
        VerificationChecks.check(machine.getVillagerEntity().getVillagerXp() == loaded.getVillagerEntity().getVillagerXp(), "Villager XP survives save/reload");
    }
}
