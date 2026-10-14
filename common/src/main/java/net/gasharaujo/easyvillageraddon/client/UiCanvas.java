package net.gasharaujo.easyvillageraddon.client;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
interface UiCanvas {
    void fill(int x, int y, int right, int bottom, int color);
    void text(Component text, int x, int y, int color);
    void item(ItemStack item, int x, int y);
    void tooltip(ItemStack item, int x, int y);
    void tooltip(Component text, int x, int y);
}
