package net.gasharaujo.easyvillageraddon.client;

import net.gasharaujo.easyvillageraddon.MachineKind;
import net.gasharaujo.easyvillageraddon.Platform;
import net.gasharaujo.easyvillageraddon.UiAction;
import net.gasharaujo.easyvillageraddon.UiNetwork;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.inventory.ContainerInput;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Exercises the real client connection and captures the actual rendered screens. */
public final class VerificationClient {
    private static int stage, delay, age;
    private static boolean configured, completedSearch, searchStarted;
    public static void install() { NeoForge.EVENT_BUS.addListener((ClientTickEvent.Post event) -> tick()); }
    private static void tick() {
        Minecraft mc = Minecraft.getInstance(); mc.getToastManager().clear();
        if (++age > 6000) { System.err.println("ADDON_CLIENT_UI_FAILED timeout stage=" + stage); mc.stop(); return; }
        if (!configured && mc.options != null) {
            configured = true; mc.options.pauseOnLostFocus = false; mc.options.guiScale().set(3); mc.options.languageCode = "pt_br";
            mc.getLanguageManager().setSelected("pt_br"); mc.reloadResourcePacks();
        }
        if (stage >= 25) { if (delay > 0) delay--; else mc.stop(); return; }
        if (mc.player == null) return;
        if (stage >= 20 && stage < 25) {
            mc.player.setYRot(stage == 20 ? 180F : 135F);
            mc.player.setXRot(stage == 20 ? 12F : 25F);
            mc.options.hideGui = stage >= 21;
        }
        if (delay > 0) { delay--; return; }
        if (stage >= 25) { mc.stop(); return; }
        try {
            if (stage == 0 && Boolean.getBoolean("addon.modelVerificationOnly")) { command(mc, "gallery"); stage = 20; delay = 60; return; }
            if (stage == 0) { command(mc, "advanced_trader"); stage++; delay = 70; return; }
            if (stage >= 20) {
                switch (stage) {
                    case 20 -> {
                        for (MachineKind kind : MachineKind.values()) {
                            var entity = mc.level.getBlockEntity(new net.minecraft.core.BlockPos(kind.ordinal() * 2, 100, 0));
                            if (!(entity instanceof net.gasharaujo.easyvillageraddon.MachineEntity machine)
                                    || !((Object) mc.getBlockEntityRenderDispatcher().getRenderer(machine) instanceof net.gasharaujo.easyvillageraddon.MachineRenderer)) throw new AssertionError("Missing native content renderer: " + kind);
                        }
                        capture(mc, "11-machines.png"); command(mc, "model_advanced_trader"); stage++; delay = 25;
                    }
                    case 21 -> { capture(mc, "12-advanced-trader-model.png"); command(mc, "model_desneuralizer"); stage++; delay = 25; }
                    case 22 -> { capture(mc, "13-desneuralizer-model.png"); command(mc, "model_briber"); stage++; delay = 25; }
                    case 23 -> { capture(mc, "14-briber-model.png"); command(mc, "model_leveler"); stage++; delay = 25; }
                    case 24 -> { capture(mc, "15-leveler-model.png"); System.out.println("ADDON_CLIENT_MODELS_PASSED count=4"); command(mc, "stop"); stage++; delay = 25; }
                    default -> mc.stop();
                }
                return;
            }
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
                    mc.gameMode.handleContainerInput(menu.containerId, 2, 0, ContainerInput.PICKUP, mc.player);
                    stage++; delay = 25;
                }
                case 5 -> {
                    if (menu.getCarried().isEmpty() || menu.state.xp <= 0) throw new AssertionError("Network purchase did not return its real item and XP");
                    int empty = java.util.stream.IntStream.range(3, 39).filter(i -> !menu.getSlot(i).hasItem()).findFirst().orElseThrow();
                    mc.gameMode.handleContainerInput(menu.containerId, empty, 0, ContainerInput.PICKUP, mc.player);
                    System.out.println("ADDON_CLIENT_REAL_TRADE_PASSED xp=" + menu.state.xp);
                    command(mc, "leveler"); stage++; delay = 50;
                }
                case 6 -> {
                    if (menu.kind() != MachineKind.LEVELER) return;
                    if (menu.getSlot(2).isActive()) throw new AssertionError("Processor unexpectedly has merchant result");
                    screen.pane(1); stage++; delay = 40;
                }
                case 7 -> {
                    if (!menu.state.catalogComplete) { delay = 20; return; }
                    pressNamed(screen, "Troca exata"); filter(screen, "picareta de diamante"); pressRow(screen, 0);
                    stage++; delay = 25;
                }
                case 8 -> {
                    if (menu.state.targets.size() != 1 || !menu.state.targets.get(0).generic() || !menu.state.targets.get(0).result().is(net.minecraft.world.item.Items.DIAMOND_PICKAXE)) throw new AssertionError("Generic pickaxe was not synchronized");
                    capture(mc, "08-leveler-generic.png"); pressNamed(screen, "4"); stage++; delay = 30;
                }
                case 9 -> {
                    if (!menu.state.catalogComplete) { delay = 20; return; }
                    filter(screen, "machado de diamante"); pressRow(screen, 0); stage++; delay = 25;
                }
                case 10 -> {
                    if (menu.state.targets.size() != 2 || menu.state.targets.get(0).level() != 5 || menu.state.targets.get(1).level() != 4) throw new AssertionError("Multi-level targets were lost");
                    pressNamed(screen, "OU"); pressNamed(screen, "Alvos: 2"); filter(screen, ""); stage++; delay = 25;
                }
                case 11 -> {
                    if (!menu.state.matchAll) throw new AssertionError("AND mode was not synchronized");
                    capture(mc, "09-leveler-multiple-and.png"); pressRow(screen, 1); stage++; delay = 25;
                }
                case 12 -> {
                    if (menu.state.targets.size() != 1 || !menu.state.targets.get(0).result().is(net.minecraft.world.item.Items.DIAMOND_PICKAXE)) throw new AssertionError("Removing one target changed the other");
                    pressNamed(screen, "Catálogo"); pressNamed(screen, "Qualquer item"); filter(screen, "machado de diamante"); pressRow(screen, 0); stage++; delay = 25;
                }
                case 13 -> {
                    if (menu.state.targets.size() != 2 || !menu.state.targets.get(0).generic() || menu.state.targets.get(1).generic()) throw new AssertionError("Generic and exact targets cannot coexist");
                    pressNamed(screen, "Alvos: 2"); filter(screen, ""); stage++; delay = 20;
                }
                case 14 -> { capture(mc, "10-leveler-mixed-targets.png"); pressNamed(screen, "Pronto"); stage++; delay = 25; }
                case 15 -> {
                    capture(mc, "04-leveler-real-inventory.png"); pressNamed(screen, "Iniciar"); stage++; delay = 50;
                }
                case 16 -> {
                    if (!menu.state.enabled) throw new AssertionError("Leveler did not start with multiple targets");
                    capture(mc, "05-leveler-running.png");
                    System.out.println("ADDON_CLIENT_TARGETS_PASSED count=" + menu.state.targets.size() + " and=" + menu.state.matchAll);
                    command(mc, "briber"); stage++; delay = 40;
                }
                case 17 -> { if (menu.kind() != MachineKind.BRIBER) return; if (menu.getSlot(2).isActive()) throw new AssertionError("Briber unexpectedly has merchant result"); capture(mc, "06-briber.png"); command(mc, "desneuralizer"); stage++; delay = 35; }
                case 18 -> { if (menu.kind() != MachineKind.DESNEURALIZER) return; capture(mc, "07-desneuralizer.png"); stage++; delay = 20; }
                case 19 -> { System.out.println("ADDON_CLIENT_UI_PASSED"); command(mc, "gallery"); stage++; delay = 60; }
                default -> mc.stop();
            }
        } catch (Throwable failure) {
            System.err.println("ADDON_CLIENT_UI_FAILED stage=" + stage); failure.printStackTrace();
            command(mc, "stop"); mc.stop();
        }
    }
    private static void press(net.minecraft.client.gui.components.Button button) { button.onPress(new net.minecraft.client.input.KeyEvent(257, 0, 0)); }
    private static void pressNamed(MachineScreen screen, String label) {
        var button = screen.children().stream().filter(net.minecraft.client.gui.components.Button.class::isInstance).map(net.minecraft.client.gui.components.Button.class::cast)
                .filter(b -> b.visible && b.active && b.getMessage().getString().equals(label)).findFirst().orElseThrow(() -> new AssertionError("Missing button: " + label));
        press(button);
    }
    private static void pressRow(MachineScreen screen, int row) {
        var buttons = screen.children().stream().filter(net.minecraft.client.gui.components.Button.class::isInstance).map(net.minecraft.client.gui.components.Button.class::cast)
                .filter(b -> b.visible && b.active && b.getWidth() == 248).sorted(java.util.Comparator.comparingInt(net.minecraft.client.gui.components.Button::getY)).toList();
        if (row >= buttons.size()) throw new AssertionError("Missing filtered row " + row);
        press(buttons.get(row));
    }
    private static void filter(MachineScreen screen, String query) { screen.searchBox().setValue(query); screen.searchBox().setFocused(false); screen.containerTick(); }
    private static void command(Minecraft mc, String machine) { mc.player.connection.sendCommand("addon_ui_test " + machine); }
    private static void capture(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), 1, result -> System.out.println("ADDON_UI_SCREENSHOT " + name + " " + result.getString()));
    }
}
