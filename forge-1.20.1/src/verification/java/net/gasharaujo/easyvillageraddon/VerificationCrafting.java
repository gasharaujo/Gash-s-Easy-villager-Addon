package net.gasharaujo.easyvillageraddon;

import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.CraftingRecipe;
import java.util.List;

final class VerificationCrafting {
    static ItemStack craft(MinecraftServer server, String id, List<ItemStack> grid) {
        CraftingRecipe recipe = (CraftingRecipe) server.getRecipeManager().byKey(new net.minecraft.resources.ResourceLocation(EasyVillagerAddon.MOD_ID, id)).orElseThrow();
        var input = new net.minecraft.world.inventory.TransientCraftingContainer(new net.minecraft.world.inventory.AbstractContainerMenu(null, -1) {
            @Override public ItemStack quickMoveStack(net.minecraft.world.entity.player.Player player, int slot) { return ItemStack.EMPTY; }
            @Override public boolean stillValid(net.minecraft.world.entity.player.Player player) { return true; }
        }, 3, 3);
        for (int i = 0; i < grid.size(); i++) input.setItem(i, grid.get(i).copy());
        return recipe.matches(input, server.overworld()) ? recipe.assemble(input, server.overworld().registryAccess()) : ItemStack.EMPTY;
    }
    static ItemStack potion(String id, boolean splash) {
        var potion = switch (id) {
            case "long_weakness" -> Potions.LONG_WEAKNESS;
            case "weakness" -> Potions.WEAKNESS;
            case "water" -> Potions.WATER;
            case "long_slowness" -> Potions.LONG_SLOWNESS;
            default -> throw new AssertionError("Unknown test potion");
        };
        return net.minecraft.world.item.alchemy.PotionUtils.setPotion(new ItemStack(splash ? Items.SPLASH_POTION : Items.POTION), potion);
    }
}
