package net.gasharaujo.easyvillageraddon.client;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
interface UiCanvas {
    void tradeArrow(int x, int y);
    void experienceBar(int x, int y, int progress);
    void scroller(int x, int y, boolean enabled);
    void texture(String location, int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight);
    void fill(int x, int y, int right, int bottom, int color);
    void text(Component text, int x, int y, int color);
    void item(ItemStack item, int x, int y);
    void tooltip(ItemStack item, int x, int y);
    void tooltip(Component text, int x, int y);
}
