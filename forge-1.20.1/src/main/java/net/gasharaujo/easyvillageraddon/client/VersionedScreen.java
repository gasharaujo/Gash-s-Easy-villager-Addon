package net.gasharaujo.easyvillageraddon.client;
import net.gasharaujo.easyvillageraddon.MachineMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
abstract class VersionedScreen extends AbstractContainerScreen<MachineMenu> {
    VersionedScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 384; imageHeight = 236;
    }
    protected abstract void drawBackground(UiCanvas canvas, int mouseX, int mouseY);
    protected abstract void drawForeground(UiCanvas canvas, int mouseX, int mouseY);
    protected abstract boolean screenMouse(double x, double y, int button);
    protected abstract boolean screenDrag(double x, double y);
    protected abstract void endDrag();
    protected abstract boolean screenScroll(double x, double y, double delta);
    protected abstract int screenKey(int key, int modifiers);
    protected abstract EditBox searchBox();

    @Override protected void renderBg(GuiGraphics graphics, float ticks, int x, int y) { drawBackground(new Canvas(graphics), x, y); }
    @Override protected void renderLabels(GuiGraphics graphics, int x, int y) { }
    @Override public void render(GuiGraphics graphics, int x, int y, float ticks) {
        renderBackground(graphics);
        super.render(graphics, x, y, ticks);
        graphics.pose().pushPose(); graphics.pose().translate(0, 0, 500);
        drawForeground(new Canvas(graphics), x, y); graphics.pose().popPose(); renderTooltip(graphics, x, y);
    }


    @Override public boolean mouseClicked(double x, double y, int button) {
        return screenMouse(x, y, button) || super.mouseClicked(x, y, button);
    }
    @Override public boolean mouseDragged(double x, double y, int button, double dx, double dy) {
        return screenDrag(x, y) || super.mouseDragged(x, y, button, dx, dy);
    }
    @Override public boolean mouseReleased(double x, double y, int button) { endDrag(); return super.mouseReleased(x, y, button); }
    @Override public boolean keyPressed(int key, int scan, int modifiers) {
        int result = screenKey(key, modifiers);
        if (result == 1) return true;
        if (result == 2) { searchBox().keyPressed(key, scan, modifiers); return true; }
        return super.keyPressed(key, scan, modifiers);
    }

    @Override public boolean mouseScrolled(double x, double y, double delta) { return screenScroll(x, y, delta) || super.mouseScrolled(x, y, delta); }

    private final class Canvas implements UiCanvas {
        private final GuiGraphics graphics;
        Canvas(GuiGraphics graphics) { this.graphics = graphics; }
        public void fill(int x, int y, int right, int bottom, int color) { graphics.fill(x, y, right, bottom, color); }
        public void text(net.minecraft.network.chat.Component value, int x, int y, int color) { graphics.drawString(font, value, x, y, color, false); }
        public void item(net.minecraft.world.item.ItemStack stack, int x, int y) {
            graphics.renderItem(stack, x, y);
            graphics.renderItemDecorations(font, stack, x, y);
        }
        public void tooltip(net.minecraft.world.item.ItemStack stack, int x, int y) { graphics.renderTooltip(font, stack, x, y); }
        public void tooltip(net.minecraft.network.chat.Component text, int x, int y) { graphics.renderTooltip(font, text, x, y); }
    }
}
