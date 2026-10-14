package net.gasharaujo.easyvillageraddon;

import de.maxhenkel.easyvillagers.items.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.gossip.GossipType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import java.util.UUID;

/** Compiled only by -Pverification; runs against a real dedicated-server registry. */
public final class VerificationChecks {
    private static int assertions;
    private static ServerLevel world;
    private static int offset;

    public static void run(MinecraftServer server) {
        try {
            world = server.overworld();
            world.getChunk(0, 0);
            advancedTrader();
            desneuralizer();
            briber();
            leveler();
            VerificationMod.menuChecks(machine(MachineKind.ADVANCED_TRADER));
            for (var kind : MachineKind.values()) if (kind != MachineKind.ADVANCED_TRADER) VerificationMod.processorMenuChecks(machine(kind));
            System.out.println("ADDON_VERIFICATION_PASSED assertions=" + assertions);
        } catch (Throwable failure) {
            System.err.println("ADDON_VERIFICATION_FAILED");
            failure.printStackTrace();
        } finally {
            server.halt(false);
        }
    }

    static void check(boolean condition, String message) {
        assertions++;
        if (!condition) throw new AssertionError(message);
    }

    private static MachineEntity machine(MachineKind kind) {
        BlockPos pos = new BlockPos(offset++ % 12, 100, 0);
        world.setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
        world.setBlockAndUpdate(pos, AddonRegistry.MACHINES.get(kind).get().defaultBlockState());
        MachineEntity machine = (MachineEntity) world.getBlockEntity(pos);
        machine.owner = UUID.fromString("9c77f042-0218-420e-8248-b848b3e76580");
        machine.setVillager(new ItemStack(ModItems.VILLAGER.get()));
        check(machine.getVillagerEntity() != null, "Easy Villagers item loads in " + kind);
        if (kind == MachineKind.ADVANCED_TRADER || kind == MachineKind.LEVELER) machine.setWorkstation(Blocks.LECTERN);
        return machine;
    }

    private static void ticks(MachineEntity machine, int amount) { for (int i = 0; i < amount; i++) machine.tickServer(); }

    private static void advancedTrader() {
        MachineEntity machine = machine(MachineKind.ADVANCED_TRADER);
        machine.rebuildCatalog();
        for (int i = 0; i < 10000 && !machine.catalog.complete(); i++) machine.catalog.step();
        check(machine.catalog.complete(), "Novice catalog finishes");
        check(!machine.catalog.offers.isEmpty(), "Novice catalog contains actual pack trades");
        var mending = machine.catalog.offers.stream().filter(offer -> Platform.searchText(offer.getResult()).contains("mending")).findFirst();
        check(mending.isPresent(), "Tradeable mending enchantment is in novice catalog");
        check(Platform.searchText(mending.orElseThrow().getResult()).contains("remendo"), "Portuguese enchantment search alias");
        var before = machine.getVillagerEntity().getOffers();
        check(machine.select(mending.orElseThrow()), "Initial offer can be selected");
        check(machine.getVillagerEntity().getOffers() == before, "Selection does not inject or overwrite an offer");
        for (int i = 0; i < 10000 && machine.enabled; i++) ticks(machine, 1);
        check(!machine.enabled, "Natural reroll finds the selected enchantment");
        check(machine.getVillagerEntity().getOffers().stream().anyMatch(o -> TradeCatalog.sameTrade(o, mending.orElseThrow())), "Naturally rolled set contains target at any index");
        check(machine.getVillagerEntity().getOffers().size() == 2, "Whole novice trade set is retained");
        var v = machine.getVillagerEntity();
        v.setOffers(null); VerificationMod.seedTrades(v, 143573L);
        var expected = v.getOffers();
        var goal = expected.get(expected.size() - 1);
        v.setOffers(new net.minecraft.world.item.trading.MerchantOffers()); VerificationMod.seedTrades(v, 143573L);
        check(machine.select(goal), "Natural bundle can be selected");
        ticks(machine, 1);
        check(!machine.enabled && machine.attempts == 1, "Identical vanilla random seed finds target on first natural roll");
        var actual = v.getOffers();
        check(actual.size() == expected.size(), "Native random roll keeps the entire original bundle");
        for (int i = 0; i < expected.size(); i++) {
            var a = actual.get(i); var e = expected.get(i);
            check(TradeCatalog.sameTrade(a, e) && a.getBaseCostA().getCount() == e.getBaseCostA().getCount()
                    && a.getCostB().getCount() == e.getCostB().getCount() && a.getResult().getCount() == e.getResult().getCount()
                    && a.getXp() == e.getXp(), "Natural prices, XP, order and companion trades are unchanged");
        }
        var offer = actual.get(0);
        check(offer.getMaxUses() == Integer.MAX_VALUE, "Rolled offer does not exhaust");
        offer.increaseUses();
        ticks(machine, 1);
        check(offer.getUses() == 0, "Advanced Trader resets uses");
        machine.getVillagerEntity().setVillagerXp(1);
        check(!machine.select(mending.orElseThrow()), "Initial selection refuses traded villagers");
        VerificationMod.persistence(machine);
    }

    private static void desneuralizer() {
        MachineEntity machine = machine(MachineKind.DESNEURALIZER);
        machine.setWorkstation(Blocks.LECTERN);
        Platform.assignProfession(machine.getVillagerEntity(), machine.getWorkstationProfession());
        machine.getVillagerEntity().setVillagerXp(250);
        machine.getVillagerEntity().getGossips().add(machine.owner, GossipType.MINOR_POSITIVE, 10);
        var input = machine.removeVillager();
        machine.inventory.set(9, input);
        check(machine.canPlaceItem(9, input) && !machine.canPlaceItem(9, new ItemStack(Items.EMERALD)), "Desneuralizer accepts only villagers in its input");
        for (int i = 18; i < 22; i++) machine.inventory.set(i, input.copy());
        ticks(machine, 40);
        check(!machine.inventory.get(9).isEmpty() && !machine.hasVillager(), "Full villager output leaves queued input untouched");
        for (int i = 18; i < 22; i++) machine.inventory.set(i, ItemStack.EMPTY);
        ticks(machine, 20);
        check(machine.inventory.get(9).isEmpty() && !machine.hasVillager() && !machine.inventory.get(18).isEmpty(), "Desneuralizer transfers input villager to real output");
        var processed = machine.inventory.get(18);
        machine.setVillager(processed.copy());
        check(Platform.unemployed(machine.getVillagerEntity()), "Desneuralizer removes profession");
        check(machine.getVillagerEntity().getVillagerXp() == 0 && Platform.villagerLevel(machine.getVillagerEntity()) == 1, "Desneuralizer resets XP and level");
        check(machine.getVillagerEntity().getGossips().getReputation(machine.owner, t -> t == GossipType.MINOR_POSITIVE) == 10, "Desneuralizer preserves gossip");
        VerificationMod.persistence(machine);
    }

    private static void briber() {
        MachineEntity machine = machine(MachineKind.BRIBER);
        machine.inventory.set(9, new ItemStack(Items.EMERALD, 16));
        machine.inventory.set(10, new ItemStack(Items.EMERALD, 48));
        ticks(machine, 399);
        check(machine.inventory.get(9).getCount() == 16 && machine.inventory.get(10).getCount() == 48, "No bribe charged before 20 seconds");
        ticks(machine, 1);
        check(machine.inventory.get(9).isEmpty() && machine.inventory.get(10).getCount() == 32, "Bribe consumes exactly 32 split emeralds");
        check(machine.getVillagerEntity().getGossips().getReputation(machine.owner, t -> t == GossipType.MINOR_POSITIVE) == 1, "Bribe gives exactly +1 to placer");
        VerificationMod.persistence(machine);
        machine.getVillagerEntity().getGossips().add(machine.owner, GossipType.MINOR_POSITIVE, GossipType.MINOR_POSITIVE.max);
        ticks(machine, 800);
        check(machine.inventory.get(10).getCount() == 32, "Briber does not charge at gossip cap");
        VerificationMod.persistence(machine);
    }

    private static void leveler() {
        MachineEntity machine = machine(MachineKind.LEVELER);
        var villager = machine.getVillagerEntity();
        check(!villager.getOffers().isEmpty(), "Workstation generates initial offers");
        var offer = villager.getOffers().stream().filter(o -> o.getXp() > 0).findFirst().orElseThrow();
        machine.targetLevel = 2;
        machine.targetResult = new ItemStack(Items.BEDROCK); // deliberately unattainable test target
        machine.targetCostA = new ItemStack(Items.BEDROCK);
        machine.enabled = true;
        villager.setVillagerXp(9);
        machine.inventory.set(9, offer.getCostA().copy());
        machine.inventory.set(10, offer.getCostB().copy());
        for (int slot = 18; slot < 27; slot++) machine.inventory.set(slot, new ItemStack(Items.BEDROCK, 64));
        ticks(machine, 20);
        check(villager.getVillagerXp() == 9 && machine.inventory.get(9).getCount() == offer.getCostA().getCount(), "Full output does not consume inputs or grant XP");
        for (int slot = 18; slot < 27; slot++) machine.inventory.set(slot, ItemStack.EMPTY);
        ticks(machine, 20);
        check(Platform.villagerLevel(villager) == 2, "Leveler performs a paid trade and levels up");
        check(!machine.inventory.get(18).isEmpty(), "Leveler saves trade product");
        ticks(machine, 20);
        check(Platform.villagerLevel(villager) == 1 && villager.getVillagerXp() == 0 && machine.attempts == 1,
                "Missing target resets profession, level and XP: level=" + Platform.villagerLevel(villager)
                        + " xp=" + villager.getVillagerXp() + " attempts=" + machine.attempts + " status=" + machine.status);
        check(!villager.getOffers().isEmpty(), "Reset regenerates novice offers");
        machine.targetLevel = 1;
        var found = villager.getOffers().get(0);
        machine.targetResult = found.getResult().copy();
        machine.targetCostA = found.getBaseCostA().copy();
        machine.targetCostB = found.getCostB().copy();
        ticks(machine, 20);
        check(!machine.enabled, "Leveler stops when target is found");
        VerificationMod.persistence(machine);
    }
}
