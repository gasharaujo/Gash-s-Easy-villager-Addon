package net.gasharaujo.easyvillageraddon;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingRecipe;
import java.util.List;

final class VerificationCrafting {
    static ItemStack craft(MinecraftServer server, String id, List<ItemStack> grid) {
        CraftingRecipe recipe = (CraftingRecipe) server.overworld().recipeAccess().byKey(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.RECIPE, net.minecraft.resources.Identifier.fromNamespaceAndPath(EasyVillagerAddon.MOD_ID, id))).orElseThrow().value();
        var input = net.minecraft.world.item.crafting.CraftingInput.of(3, 3, grid);
        return recipe.matches(input, server.overworld()) ? recipe.assemble(input) : ItemStack.EMPTY;
    }
    static ItemStack potion(String id, boolean splash) {
        var potion = switch (id) {
            case "long_weakness" -> Potions.LONG_WEAKNESS;
            case "weakness" -> Potions.WEAKNESS;
            case "water" -> Potions.WATER;
            case "long_slowness" -> Potions.LONG_SLOWNESS;
            default -> throw new AssertionError("Unknown test potion");
        };
        return net.minecraft.world.item.alchemy.PotionContents.createItemStack(splash ? Items.SPLASH_POTION : Items.POTION, potion);
    }
}
