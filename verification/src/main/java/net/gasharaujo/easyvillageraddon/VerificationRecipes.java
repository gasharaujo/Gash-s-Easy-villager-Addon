package net.gasharaujo.easyvillageraddon;

import de.maxhenkel.easyvillagers.blocks.ModBlocks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import java.util.ArrayList;
import java.util.List;

final class VerificationRecipes {
    static void run(MinecraftServer server) {
        for (MachineKind kind : MachineKind.values()) {
            String pattern = switch (kind) {
                case ADVANCED_TRADER -> "GGGGTGDDD";
                case DESNEURALIZER -> "GGGGCGWAW";
                case BRIBER -> "GGGGTGEEE";
                case LEVELER -> "GGGGZGSXS";
            };
            List<ItemStack> grid = new ArrayList<>();
            for (char symbol : pattern.toCharArray()) grid.add(switch (symbol) {
                case 'G' -> new ItemStack(Items.GLASS_PANE);
                case 'T' -> new ItemStack(ModBlocks.TRADER.get());
                case 'D' -> new ItemStack(Items.DIAMOND);
                case 'C' -> new ItemStack(ModBlocks.CONVERTER.get());
                case 'W' -> VerificationCrafting.potion("long_weakness", false);
                case 'A' -> new ItemStack(Items.GOLDEN_APPLE);
                case 'E' -> new ItemStack(Items.EMERALD);
                case 'Z' -> new ItemStack(AddonRegistry.MACHINES.get(MachineKind.ADVANCED_TRADER).get());
                case 'X' -> new ItemStack(AddonRegistry.MACHINES.get(MachineKind.DESNEURALIZER).get());
                case 'S' -> new ItemStack(Items.SLIME_BALL);
                default -> throw new AssertionError("Unknown recipe symbol");
            });
            var result = VerificationCrafting.craft(server, kind.id, grid);
            VerificationChecks.check(result.is(AddonRegistry.MACHINES.get(kind).get().asItem()) && result.getCount() == 1, "Native loaded recipe crafts exactly one " + kind);
            for (int slot = 0; slot < 9; slot++) {
                var missing = copy(grid); missing.set(slot, ItemStack.EMPTY);
                VerificationChecks.check(VerificationCrafting.craft(server, kind.id, missing).isEmpty(), "Recipe rejects missing slot " + slot + " in " + kind);
            }
            var wrongGlass = copy(grid); wrongGlass.set(0, new ItemStack(Items.GLASS));
            VerificationChecks.check(VerificationCrafting.craft(server, kind.id, wrongGlass).isEmpty(), "Recipe requires glass panes, not glass blocks: " + kind);
            if (kind == MachineKind.DESNEURALIZER) {
                for (String potion : List.of("weakness", "water", "long_slowness")) {
                    var wrong = copy(grid); wrong.set(6, VerificationCrafting.potion(potion, false));
                    VerificationChecks.check(VerificationCrafting.craft(server, kind.id, wrong).isEmpty(), "Desneuralizer rejects " + potion + " instead of four-minute Weakness");
                }
                var splash = copy(grid); splash.set(8, VerificationCrafting.potion("long_weakness", true));
                VerificationChecks.check(VerificationCrafting.craft(server, kind.id, splash).isEmpty(), "Desneuralizer requires regular four-minute Weakness potions");
                var named = copy(grid); Platform.name(named.get(6), "Fraqueza 4:00");
                VerificationChecks.check(!VerificationCrafting.craft(server, kind.id, named).isEmpty(), "A renamed four-minute potion remains valid");
            }
            var block = AddonRegistry.MACHINES.get(kind).get();
            VerificationChecks.check(block.getStateDefinition().getPossibleStates().size() == 4, "Glass machine supports all four horizontal directions: " + kind);
        }
    }
    private static List<ItemStack> copy(List<ItemStack> source) { return new ArrayList<>(source.stream().map(ItemStack::copy).toList()); }
}
