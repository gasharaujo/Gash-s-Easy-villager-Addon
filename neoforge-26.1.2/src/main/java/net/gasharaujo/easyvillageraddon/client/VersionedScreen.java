package net.gasharaujo.easyvillageraddon.client;
import net.gasharaujo.easyvillageraddon.MachineMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
abstract class VersionedScreen extends AbstractContainerScreen<MachineMenu> {
    VersionedScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, net.gasharaujo.easyvillageraddon.MachineLayout.width(menu.kind(), menu.clientPane), net.gasharaujo.easyvillageraddon.MachineLayout.height(menu.kind(), menu.clientPane));

    }
    protected abstract void drawBackground(UiCanvas canvas, int mouseX, int mouseY);
    protected abstract void drawForeground(UiCanvas canvas, int mouseX, int mouseY);
    protected abstract boolean screenMouse(double x, double y, int button);
    protected abstract boolean screenDrag(double x, double y);
    protected abstract void endDrag();
    protected abstract boolean screenScroll(double x, double y, double delta);
    protected abstract int screenKey(int key, int modifiers);
    protected abstract EditBox searchBox();

    @Override public void extractBackground(GuiGraphicsExtractor graphics, int x, int y, float ticks) {
        super.extractBackground(graphics, x, y, ticks); drawBackground(new Canvas(graphics), x, y);
    }
    @Override public void extractRenderState(GuiGraphicsExtractor graphics, int x, int y, float ticks) {
        super.extractRenderState(graphics, x, y, ticks); graphics.nextStratum(); drawForeground(new Canvas(graphics), x, y);
    }
    @Override protected void extractLabels(GuiGraphicsExtractor graphics, int x, int y) { }


    @Override public boolean mouseClicked(net.minecraft.client.input.MouseButtonEvent event, boolean doubleClick) {
        return screenMouse(event.x(), event.y(), event.button()) || super.mouseClicked(event, doubleClick);
    }
    @Override public boolean mouseDragged(net.minecraft.client.input.MouseButtonEvent event, double dx, double dy) {
        return screenDrag(event.x(), event.y()) || super.mouseDragged(event, dx, dy);
    }
    @Override public boolean mouseReleased(net.minecraft.client.input.MouseButtonEvent event) {
        endDrag(); return super.mouseReleased(event);
    }
    @Override public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        int result = screenKey(event.key(), event.modifiers());
        if (result == 1) return true;
        if (result == 2) { searchBox().keyPressed(event); return true; }
        return super.keyPressed(event);
    }

    @Override public boolean mouseScrolled(double x, double y, double dx, double dy) { return screenScroll(x, y, dy) || super.mouseScrolled(x, y, dx, dy); }

    private final class Canvas implements UiCanvas {
        private final GuiGraphicsExtractor graphics;
        Canvas(GuiGraphicsExtractor graphics) { this.graphics = graphics; }
        public void tradeArrow(int x, int y) { graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sprite("trade_arrow"), x, y, 10, 9); }
        public void experienceBar(int x, int y, int progress) {
            graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sprite("experience_bar_background"), x, y, 102, 5);
            if (progress > 0) graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sprite("experience_bar_current"), 102, 5, 0, 0, x, y, progress, 5);
        }
        public void scroller(int x, int y, boolean enabled) { graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sprite(enabled ? "scroller" : "scroller_disabled"), x, y, 6, 27); }
        private net.minecraft.resources.Identifier sprite(String name) { return net.minecraft.resources.Identifier.parse("minecraft:container/villager/" + name); }
        public void texture(String location, int x, int y, int width, int height, int u, int v, int sourceWidth, int sourceHeight, int textureWidth, int textureHeight) { graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, net.minecraft.resources.Identifier.parse(location), x, y, (float) u, (float) v, width, height, sourceWidth, sourceHeight, textureWidth, textureHeight); }
        public void fill(int x, int y, int right, int bottom, int color) { graphics.fill(x, y, right, bottom, color); }
        public void text(net.minecraft.network.chat.Component value, int x, int y, int color) { graphics.text(font, value, x, y, color, false); }
        public void item(net.minecraft.world.item.ItemStack stack, int x, int y) {
            graphics.item(stack, x, y);
            graphics.itemDecorations(font, stack, x, y);
        }
        public void tooltip(net.minecraft.world.item.ItemStack stack, int x, int y) { graphics.setTooltipForNextFrame(font, stack, x, y); }
        public void tooltip(net.minecraft.network.chat.Component text, int x, int y) { graphics.setTooltipForNextFrame(font, text, x, y); }
    }
}
