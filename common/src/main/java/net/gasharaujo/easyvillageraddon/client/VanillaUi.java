package net.gasharaujo.easyvillageraddon.client;

/** Uses the dependency's own container frame and slots, without copying its assets. */
final class VanillaUi {
    static final String IO = "easy_villagers:textures/gui/container/input_output.png";
    static final String TRADER = "minecraft:textures/gui/container/villager.png";
    static final String AUTO = "easy_villagers:textures/gui/container/auto_trader.png";
    static final int INK = 0xff404040;
    private VanillaUi() { }
    static void blit(UiCanvas g, String id, int x, int y, int u, int v, int w, int h) { g.texture(id, x, y, w, h, u, v, w, h, 256, 256); }
    static void frame(UiCanvas g, int x, int y, int w, int h) {
        g.fill(x + 4, y + 4, x + w - 4, y + h - 4, 0xffc6c6c6);
        blit(g, IO, x, y, 0, 0, 4, 4); blit(g, IO, x + w - 4, y, 172, 0, 4, 4);
        blit(g, IO, x, y + h - 4, 0, 160, 4, 4); blit(g, IO, x + w - 4, y + h - 4, 172, 160, 4, 4);
        g.texture(IO, x + 4, y, w - 8, 4, 8, 0, 1, 4, 256, 256);
        g.texture(IO, x + 4, y + h - 4, w - 8, 4, 8, 160, 1, 4, 256, 256);
        g.texture(IO, x, y + 4, 4, h - 8, 0, 8, 4, 1, 256, 256);
        g.texture(IO, x + w - 4, y + 4, 4, h - 8, 172, 8, 4, 1, 256, 256);
    }
    static void slot(UiCanvas g, int x, int y) { blit(g, IO, x - 1, y - 1, 51, 19, 18, 18); }
    static void arrow(UiCanvas g, int x, int y) { blit(g, AUTO, x, y, 85, 20, 24, 17); }
}
