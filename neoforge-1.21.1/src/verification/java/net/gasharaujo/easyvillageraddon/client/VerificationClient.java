package net.gasharaujo.easyvillageraddon.client;

import net.gasharaujo.easyvillageraddon.MachineKind;
import net.gasharaujo.easyvillageraddon.Platform;
import net.gasharaujo.easyvillageraddon.UiAction;
import net.gasharaujo.easyvillageraddon.UiNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.inventory.ClickType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Exercises the real client connection and captures the actual rendered screens. */
public final class VerificationClient {
    private static int stage, delay, age;
    private static boolean configured, completedSearch, searchStarted;
    public static void install() { NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick()); }
    private static void tick() {
        Minecraft mc = Minecraft.getInstance(); mc.getToasts().clear();
        if (++age > 6000) { System.err.println("ADDON_CLIENT_UI_FAILED timeout stage=" + stage); mc.stop(); return; }
        if (!configured && mc.options != null) {
            configured = true; mc.options.pauseOnLostFocus = false; mc.options.guiScale().set(3); mc.options.languageCode = "pt_br";
            mc.getLanguageManager().setSelected("pt_br"); mc.reloadResourcePacks();
        }
        if (stage >= 13) { if (delay > 0) delay--; else mc.stop(); return; }
        if (mc.player == null) return;
        if (delay > 0) { delay--; return; }
        if (stage >= 13) { mc.stop(); return; }
        try {
            if (stage == 0) { command(mc, "advanced_trader"); stage++; delay = 70; return; }
            if (!(mc.screen instanceof MachineScreen screen)) return;
            var menu = screen.getMenu();
            if (!menu.state.hasVillager && menu.kind() != MachineKind.DESNEURALIZER) return;
            switch (stage) {
                case 1 -> {
                    capture(mc, "01-advanced-trader.png"); screen.pane(1);
                    stage++; delay = 60;
                }
                case 2 -> {
                    if (!menu.state.catalogComplete) { delay = 20; return; }
                    if (!searchStarted) { screen.searchBox().setValue("Re"); screen.searchBox().setFocused(true); screen.setFocused(screen.searchBox()); searchStarted = true; delay = 20; return; }
                    if (!completedSearch) {
                        capture(mc, "02-autocomplete.png");
                        screen.screenKey(org.lwjgl.glfw.GLFW.GLFW_KEY_TAB, 0);
                        if (screen.searchBox().getValue().equals("Re")) throw new AssertionError("Tab did not complete the enchantment name");
                        screen.searchBox().setValue("Remendo"); completedSearch = true; delay = 15; return;
                    }
                    capture(mc, "02b-enchantment-filter.png");
                    var target = menu.catalog.stream().filter(offer -> Platform.searchText(offer.result()).contains("mending")).findFirst().orElseThrow();
                    UiNetwork.action(new UiAction(menu.containerId, UiAction.CHOOSE, target.id(), menu.state.revision));
                    screen.searchBox().setValue(""); screen.pane(0); stage++; delay = 30;
                }
                case 3 -> {
                    if (menu.state.enabled || menu.getOffers().stream().noneMatch(o -> Platform.searchText(o.getResult()).contains("mending"))) { delay = 10; return; }
                    UiNetwork.action(new UiAction(menu.containerId, UiAction.TRADE, java.util.stream.IntStream.range(0, menu.getOffers().size()).filter(i -> Platform.searchText(menu.getOffers().get(i).getResult()).contains("mending")).findFirst().orElseThrow(), menu.state.revision)); stage++; delay = 25;
                }
                case 4 -> {
                    if (!menu.getSlot(2).hasItem()) throw new AssertionError("Real merchant result is empty after payment");
                    capture(mc, "03-real-merchant-payment.png");
                    mc.gameMode.handleInventoryMouseClick(menu.containerId, 2, 0, ClickType.PICKUP, mc.player);
                    stage++; delay = 25;
                }
                case 5 -> {
                    if (menu.getCarried().isEmpty() || menu.state.xp <= 0) throw new AssertionError("Network purchase did not return its real item and XP");
                    int empty = java.util.stream.IntStream.range(3, 39).filter(i -> !menu.getSlot(i).hasItem()).findFirst().orElseThrow();
                    mc.gameMode.handleInventoryMouseClick(menu.containerId, empty, 0, ClickType.PICKUP, mc.player);
                    System.out.println("ADDON_CLIENT_REAL_TRADE_PASSED xp=" + menu.state.xp);
                    command(mc, "leveler"); stage++; delay = 50;
                }
                case 6 -> {
                    if (menu.kind() != MachineKind.LEVELER) return;
                    if (menu.getSlot(2).isActive()) throw new AssertionError("Processor unexpectedly has merchant result"); stage++; delay = 20;
                }
                case 7 -> {
                    capture(mc, "04-leveler-real-inventory.png");
                    UiNetwork.action(new UiAction(menu.containerId, UiAction.TOGGLE, 0, menu.state.revision)); stage++; delay = 50;
                }
                case 8 -> { capture(mc, "05-leveler-running.png"); command(mc, "briber"); stage++; delay = 40; }
                case 9 -> { if (menu.kind() != MachineKind.BRIBER) return; if (menu.getSlot(2).isActive()) throw new AssertionError("Processor unexpectedly has merchant result"); stage++; delay = 20; }
                case 10 -> { capture(mc, "06-briber.png"); command(mc, "desneuralizer"); stage++; delay = 35; }
                case 11 -> { if (menu.kind() != MachineKind.DESNEURALIZER) return; capture(mc, "07-desneuralizer.png"); stage++; delay = 40; }
                case 12 -> { System.out.println("ADDON_CLIENT_UI_PASSED"); command(mc, "stop"); stage++; delay = 25; }
                default -> mc.stop();
            }
        } catch (Throwable failure) {
            System.err.println("ADDON_CLIENT_UI_FAILED stage=" + stage); failure.printStackTrace();
            command(mc, "stop"); mc.stop();
        }
    }
    private static void command(Minecraft mc, String machine) { mc.player.connection.sendCommand("addon_ui_test " + machine); }
    private static void capture(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), result -> System.out.println("ADDON_UI_SCREENSHOT " + name + " " + result.getString()));
    }
}
