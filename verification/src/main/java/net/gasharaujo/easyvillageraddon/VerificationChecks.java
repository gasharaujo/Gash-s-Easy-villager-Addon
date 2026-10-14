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
            levelerSpeed();
            levelerTargets();
            VerificationMod.menuChecks(machine(MachineKind.ADVANCED_TRADER));
            VerificationMod.interactionChecks(machine(MachineKind.ADVANCED_TRADER));
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
        check(machine.select(VerificationMod.testOffer(new ItemStack(Items.BEDROCK), new ItemStack(Items.BEDROCK))), "Unattainable test target starts a bounded natural search");
        ticks(machine, 1);
        check(machine.enabled && machine.attempts == 32, "Advanced Trader retains 32 natural attempts per tick");
        ticks(machine, 1);
        check(machine.enabled && machine.attempts == 64, "Advanced Trader retries on consecutive ticks without delay");
        machine.enabled = false;
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
        ticks(machine, 1);
        check(villager.getVillagerXp() == 9 && machine.inventory.get(9).getCount() == offer.getCostA().getCount(), "Full output does not consume inputs or grant XP");
        for (int slot = 18; slot < 27; slot++) machine.inventory.set(slot, ItemStack.EMPTY);
        ticks(machine, 1);
        check(!machine.inventory.get(18).isEmpty(), "Leveler saves trade product");
        check(Platform.villagerLevel(villager) == 1 && villager.getVillagerXp() == 0 && machine.attempts == 1,
                "Paid level-up and missing target reset finish in one tick: level=" + Platform.villagerLevel(villager)
                        + " xp=" + villager.getVillagerXp() + " attempts=" + machine.attempts + " status=" + machine.status);
        check(!villager.getOffers().isEmpty(), "Reset regenerates novice offers");
        machine.targetLevel = 1;
        var found = villager.getOffers().get(0);
        machine.targetResult = found.getResult().copy();
        machine.targetCostA = found.getBaseCostA().copy();
        machine.targetCostB = found.getCostB().copy();
        ticks(machine, 1);
        check(!machine.enabled, "Leveler stops when target is found");
        VerificationMod.persistence(machine);
    }

    private static void levelerSpeed() {
        MachineEntity machine = machine(MachineKind.LEVELER);
        var villager = machine.getVillagerEntity();
        machine.targetResult = new ItemStack(Items.BEDROCK);
        machine.targetCostA = new ItemStack(Items.BEDROCK);
        machine.enabled = true;
        for (int attempt = 1; attempt <= 3; attempt++) {
            ticks(machine, 1);
            check(machine.attempts == attempt, "Leveler completes one novice retry on consecutive tick " + attempt);
        }
        machine.enabled = false; ticks(machine, 1);
        check(machine.attempts == 3, "Paused Leveler does not retry");

        machine.targetLevel = 2; machine.attempts = 0; machine.enabled = true;
        var offers = new net.minecraft.world.item.trading.MerchantOffers();
        offers.add(VerificationMod.testOffer(new ItemStack(Items.EMERALD), new ItemStack(Items.DIAMOND)));
        villager.setOffers(offers);
        machine.inventory.set(9, new ItemStack(Items.DIAMOND, 64));
        ticks(machine, 1);
        check(machine.attempts == 1 && Platform.villagerLevel(villager) == 1 && villager.getVillagerXp() == 0,
                "Ten paid trades, unlock, target check and reset complete in one tick");
        check(machine.inventory.get(9).getCount() == 54 && machine.inventory.get(18).getCount() == 10,
                "Fast attempt pays for every trade and retains every product");

        offers = new net.minecraft.world.item.trading.MerchantOffers();
        offers.add(VerificationMod.testOffer(new ItemStack(Items.EMERALD), new ItemStack(Items.DIAMOND)));
        villager.setOffers(offers); machine.attempts = 0;
        for (int slot = 18; slot < 27; slot++) machine.inventory.set(slot, new ItemStack(Items.BEDROCK, 64));
        machine.inventory.set(26, new ItemStack(Items.EMERALD, 63));
        ticks(machine, 1);
        check(villager.getVillagerXp() == 1 && machine.inventory.get(9).getCount() == 53 && machine.inventory.get(26).getCount() == 64,
                "Fast batch stops immediately when the last output space fills");
        ticks(machine, 1);
        check(villager.getVillagerXp() == 1 && machine.inventory.get(9).getCount() == 53 && machine.attempts == 0,
                "Full output on consecutive tick cannot consume another payment");

        for (int slot = 9; slot < 18; slot++) machine.inventory.set(slot, new ItemStack(Items.EMERALD, 64));
        for (int slot = 18; slot < 27; slot++) machine.inventory.set(slot, ItemStack.EMPTY);
        villager.setVillagerXp(0); machine.targetLevel = 5;
        offers = new net.minecraft.world.item.trading.MerchantOffers();
        offers.add(Platform.unlimited(VerificationMod.testOffer(new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD)), villager));
        villager.setOffers(offers);
        ticks(machine, 1);
        check(villager.getVillagerXp() == 64 && machine.attempts == 0, "Large attempt is bounded to 64 paid trades per tick");
        ticks(machine, 1);
        check(villager.getVillagerXp() == 128 && machine.attempts == 0, "Bounded attempt resumes on the very next tick");
        ticks(machine, 2);
        check(machine.attempts == 1 && villager.getVillagerXp() == 0 && Platform.villagerLevel(villager) == 1,
                "Large attempt completes and resets at the actual unlock level");
        check(machine.inventory.subList(9, 18).stream().mapToInt(ItemStack::getCount).sum() == 326
                        && machine.inventory.subList(18, 27).stream().mapToInt(ItemStack::getCount).sum() == 250,
                "Bounded attempt consumes and produces exactly 250 trades without starting a second attempt");
        VerificationMod.persistence(machine);
    }

    private static void levelerTargets() {
        MachineEntity machine = machine(MachineKind.LEVELER);
        machine.setWorkstation(Blocks.SMITHING_TABLE); machine.targetLevel = 5; machine.rebuildCatalog();
        while (!machine.catalog.complete()) machine.catalog.step();
        var pickaxes = machine.catalog.offers.stream().filter(o -> o.getResult().is(Items.DIAMOND_PICKAXE)).toList();
        check(!pickaxes.isEmpty(), "Actual toolsmith catalog exposes diamond pickaxes");
        var pick = pickaxes.get(0);
        var variant = pickaxes.stream().filter(o -> !Platform.sameStack(o.getResult(), pick.getResult())).findFirst().orElseThrow();
        var generic = TradeTarget.of(5, true, pick); var exact = TradeTarget.of(5, false, pick);
        check(generic.matches(pick) && generic.matches(variant), "Generic pickaxe accepts different enchantments");
        check(exact.matches(pick) && !exact.matches(variant), "Exact target preserves enchantments");
        machine.toggleTarget(pick, true);
        check(machine.targets.size() == 1 && machine.targets.get(0).generic(), "Generic target is selected");
        machine.toggleTarget(variant, true);
        check(machine.targets.isEmpty() && machine.targetResult.isEmpty(), "Another variant toggles the same generic item without duplicates");
        machine.toggleTarget(pick, true);
        machine.targetLevel = 1;
        var other = VerificationMod.testOffer(new ItemStack(Items.DIAMOND), new ItemStack(Items.EMERALD, 7));
        machine.toggleTarget(other, false);
        check(machine.targets.size() == 2 && machine.targets.get(0).level() == 5 && machine.targets.get(1).level() == 1, "Selections retain their own unlock levels");
        check(!machine.targetsSatisfied(java.util.List.of(pick), 1), "Later-level target cannot match prematurely");
        machine.matchAll = false;
        check(machine.targetsSatisfied(java.util.List.of(other), 1), "OR stops at an earlier-level match");
        check(machine.targetsSatisfied(java.util.List.of(variant), 5), "OR accepts any selected target");
        machine.matchAll = true;
        check(!machine.targetsSatisfied(java.util.List.of(variant), 5) && !machine.targetsSatisfied(java.util.List.of(other), 5), "AND never accumulates matches from different villagers or rolls");
        check(machine.targetsSatisfied(java.util.List.of(variant, other), 5), "AND accepts all selected targets on the same villager");
        VerificationMod.persistence(machine);
        var v = machine.getVillagerEntity();
        while (Platform.villagerLevel(v) < 5) Platform.levelUp(v);
        var offers = new net.minecraft.world.item.trading.MerchantOffers(); offers.add(variant); v.setOffers(offers);
        machine.enabled = true; ticks(machine, 1);
        check(machine.enabled && machine.attempts == 1 && Platform.villagerLevel(v) == 1, "AND resets and continues when only one target exists at the highest level");
        while (Platform.villagerLevel(v) < 5) Platform.levelUp(v);
        offers = new net.minecraft.world.item.trading.MerchantOffers(); offers.add(variant); offers.add(other); v.setOffers(offers);
        ticks(machine, 1);
        check(!machine.enabled && machine.status.endsWith("target_found"), "AND automation stops after all targets are found together");
        machine.removeTarget(0);
        check(machine.targets.size() == 1 && !machine.targets.get(0).generic(), "Selected target can be removed independently");
        machine.clearTargets();
        check(machine.effectiveTargets().isEmpty() && !machine.enabled, "Clear removes all targets including the legacy preview");
        machine.targetResult = other.getResult().copy(); machine.targetCostA = other.getBaseCostA().copy(); machine.targetCostB = other.getCostB().copy(); machine.targetLevel = 1;
        machine.migrateTargets();
        check(machine.targets.size() == 1 && machine.targets.get(0).matches(other), "Version 0.3 single target migrates without loss");
        VerificationMod.persistence(machine);
    }
}
