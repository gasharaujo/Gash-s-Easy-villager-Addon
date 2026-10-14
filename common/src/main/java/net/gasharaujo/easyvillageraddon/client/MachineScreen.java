package net.gasharaujo.easyvillageraddon.client;

import net.gasharaujo.easyvillageraddon.MachineKind;
import net.gasharaujo.easyvillageraddon.MachineMenu;
import net.gasharaujo.easyvillageraddon.OfferView;
import net.gasharaujo.easyvillageraddon.Platform;
import net.gasharaujo.easyvillageraddon.UiAction;
import net.gasharaujo.easyvillageraddon.UiNetwork;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.glfw.GLFW;

/** Merchant-style screen: offer rows and widgets are separate from real inventory slots. */
public final class MachineScreen extends VersionedScreen {
    private static final int INK = 0xff404040, MUTED = 0xff686868, ROW_TOP = 84, ROW_HEIGHT = 22, ROWS = 5;
    private EditBox search;
    private Button tradeTab, catalogTab, storageTab, actionButton, chooseButton, previous, next, clear;
    private final List<Button> levels = new ArrayList<>();
    private List<TradeSearch.Entry> indexed = new ArrayList<>(), filtered = List.of();
    private List<String> completions = List.of();
    private Object indexedOffers;
    private int indexedRevision = -1, indexedSize, indexedPane = -1, scroll, selected = -1, suggestion;
    private String query = "";
    private boolean dragging, suggestionsDismissed;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) { super(menu, inventory, title); }
    private static Component text(String key, Object... args) { return Component.translatable("gui.gashs_easy_villager_addon." + key, args); }

    @Override protected void init() {
        super.init();
        levels.clear();
        tradeTab = button(8, 25, 52, 18, text("trades"), () -> pane(0));
        catalogTab = button(62, 25, 68, 18, text("catalog"), () -> pane(1));
        storageTab = button(132, 25, 52, 18, text("storage"), () -> pane(2));
        search = new EditBox(font, leftPos + 10, topPos + 49, 149, 16, text("search"));
        search.setMaxLength(100); search.setHint(text("search_hint")); search.setValue(query);
        search.setResponder(value -> { query = value; scroll = 0; suggestionsDismissed = false; updateFilter(); });
        addRenderableWidget(search);
        clear = button(163, 47, 21, 20, Component.literal("×"), () -> search.setValue(""));
        clear.setTooltip(Tooltip.create(text("clear_search")));
        actionButton = button(278, 33, 94, 20, text("choose_initial"), this::mainAction);
        chooseButton = button(31, 210, 130, 18, text("apply"), this::applySelection);
        previous = button(8, 210, 20, 18, Component.literal("<"), () -> scroll = Math.max(0, scroll - ROWS));
        next = button(163, 210, 21, 18, Component.literal(">"), () -> scroll = Math.min(maxScroll(), scroll + ROWS));
        for (int i = 1; i <= 5; i++) {
            final int level = i;
            levels.add(button(47 + (i - 1) * 27, 68, 25, 14, Component.literal(Integer.toString(i)), () -> {
                selected = -1; send(UiAction.LEVEL, level); search.setValue("");
            }));
        }
        updateSource(); updateControls();
    }
    private Button button(int x, int y, int width, int height, Component label, Runnable run) {
        return addRenderableWidget(Button.builder(label, button -> run.run()).bounds(leftPos + x, topPos + y, width, height).build());
    }
    public void pane(int pane) {
        menu.clientPane = pane; selected = -1; scroll = 0; indexedPane = -1;
        suggestionsDismissed = true; search.setFocused(false); updateSource(); updateControls();
    }
    private void mainAction() {
        switch (menu.kind()) {
            case ADVANCED_TRADER -> pane(1);
            case LEVELER -> send(UiAction.TOGGLE, 0);
            case DESNEURALIZER -> send(UiAction.RESET, 0);
            default -> { }
        }
    }
    private void send(int action, int value) { UiNetwork.action(new UiAction(menu.containerId, action, value, menu.state.revision)); }
    private void applySelection() {
        if (menu.clientPane != 1 || selected < 0 || !chooseButton.active) return;
        send(UiAction.CHOOSE, selected); pane(0);
    }

    @Override protected void containerTick() {
        super.containerTick(); updateSource(); updateControls();
    }
    private void updateSource() {
        boolean changed = indexedPane != menu.clientPane;
        if (changed) { indexed = new ArrayList<>(); indexedSize = 0; indexedOffers = null; indexedRevision = -1; indexedPane = menu.clientPane; }
        if (menu.clientPane == 1) {
            if (indexedRevision != menu.state.revision || indexedSize > menu.catalog.size()) {
                indexed.clear(); indexedSize = 0; indexedRevision = menu.state.revision; selected = -1; changed = true;
            }
            while (indexedSize < menu.catalog.size()) { indexed.add(TradeSearch.index(menu.catalog.get(indexedSize++))); changed = true; }
        } else if (menu.clientPane == 0 && indexedOffers != menu.getOffers()) {
            indexed.clear(); indexedOffers = menu.getOffers();
            for (int i = 0; i < menu.getOffers().size(); i++) indexed.add(TradeSearch.index(OfferView.of(i, menu.getOffers().get(i))));
            changed = true;
        }
        if (changed) updateFilter();
    }
    private void updateFilter() {
        filtered = indexed.stream().filter(entry -> TradeSearch.matches(entry, query)).toList();
        completions = TradeSearch.suggest(indexed, query); suggestion = 0;
        scroll = Math.min(scroll, maxScroll());
        if (search != null) {
            String completion = completions.isEmpty() ? "" : completions.get(0);
            search.setSuggestion(!query.isEmpty() && completion.regionMatches(true, 0, query, 0, query.length()) ? completion.substring(query.length()) : null);
        }
    }
    private void updateControls() {
        if (search == null) return;
        boolean catalogAvailable = menu.kind() == MachineKind.ADVANCED_TRADER || menu.kind() == MachineKind.LEVELER;
        boolean storageAvailable = menu.kind() == MachineKind.BRIBER || menu.kind() == MachineKind.LEVELER;
        if ((!catalogAvailable && menu.clientPane == 1) || (!storageAvailable && menu.clientPane == 2)) pane(0);
        tradeTab.active = menu.clientPane != 0;
        catalogTab.visible = catalogAvailable; catalogTab.active = menu.clientPane != 1;
        catalogTab.setMessage(text(menu.kind() == MachineKind.ADVANCED_TRADER ? "initial_trade" : "target_trade"));
        storageTab.visible = storageAvailable; storageTab.active = menu.clientPane != 2;
        search.visible = clear.visible = menu.clientPane != 2;
        chooseButton.visible = menu.clientPane == 1;
        chooseButton.active = selected >= 0 && (menu.kind() != MachineKind.ADVANCED_TRADER || menu.state.canChooseInitial);
        chooseButton.setMessage(text(menu.kind() == MachineKind.ADVANCED_TRADER ? "set_initial" : "set_target"));
        previous.visible = next.visible = menu.clientPane != 2;
        previous.active = scroll > 0; next.active = scroll < maxScroll();
        for (int i = 0; i < levels.size(); i++) {
            levels.get(i).visible = menu.clientPane == 1 && menu.kind() == MachineKind.LEVELER;
            levels.get(i).active = menu.state.targetLevel != i + 1;
        }
        actionButton.visible = true;
        actionButton.active = switch (menu.kind()) {
            case ADVANCED_TRADER -> menu.state.canChooseInitial;
            case LEVELER -> menu.state.hasVillager && !menu.state.target.isEmpty();
            case DESNEURALIZER -> menu.state.hasVillager;
            case BRIBER -> false;
        };
        actionButton.setMessage(text(switch (menu.kind()) {
            case ADVANCED_TRADER -> "choose_initial";
            case LEVELER -> menu.state.enabled ? "pause" : "start";
            case DESNEURALIZER -> "reset";
            case BRIBER -> "automatic";
        }));
    }

    @Override protected void drawBackground(UiCanvas g, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        panel(g, x, y, 384, 236);
        g.fill(x + 191, y + 22, x + 192, y + 228, 0xff858585);
        g.fill(x + 192, y + 22, x + 193, y + 228, 0xffffffff);
        g.text(Component.literal(menu.kind().title), x + 8, y + 9, INK);
        Component villagerName = menu.state.name.isBlank() ? professionLabel() : Component.literal(menu.state.name);
        g.text(trim(villagerName, 176), x + 198, y + 9, INK);
        g.text(text("villager"), x + 198, y + 24, MUTED);
        if (menu.kind() == MachineKind.ADVANCED_TRADER || menu.kind() == MachineKind.LEVELER) g.text(text("workstation"), x + 246, y + 24, MUTED);
        for (var slot : menu.slots) if (slot.isActive()) slot(g, x + slot.x - 1, y + slot.y - 1, 18, 18);
        g.text(text("level_xp", Component.translatable("merchant.level." + Math.max(1, Math.min(5, menu.state.level))), menu.state.xp), x + 202, y + 61, MUTED);
        g.text(text(menu.clientPane == 1 ? "chosen_offer" : "payment"), x + 202, y + 72, INK);
        arrow(g, x + 257, y + 86);
        if (menu.clientPane == 1) {
            slot(g, x + 201, y + 81, 18, 18); slot(g, x + 227, y + 81, 18, 18); slot(g, x + 285, y + 81, 18, 18);
            OfferView offer = selectedOffer();
            if (offer != null) { g.item(offer.costA(), x + 202, y + 82); g.item(offer.costB(), x + 228, y + 82); g.item(offer.result(), x + 286, y + 82); }
        }
        if (menu.kind() == MachineKind.ADVANCED_TRADER) {
            g.text(text("unlimited"), x + 309, y + 83, 0xff24662c);
            g.text(text("stock"), x + 309, y + 93, 0xff24662c);
        }
        g.fill(x + 202, y + 104, x + 372, y + 109, 0xff303030);
        int progress = xpProgress();
        g.fill(x + 203, y + 105, x + 203 + progress, y + 108, 0xff70a82a);
        Component status = statusLabel();
        int lineY = y + 115;
        if (menu.kind() == MachineKind.LEVELER && !menu.state.target.isEmpty()) {
            g.text(trim(text("target_label", itemName(menu.state.target)), 170), x + 202, y + 115, 0xff315c26);
            g.text(trim(status, 170), x + 202, y + 125, MUTED);
        } else for (var line : font.getSplitter().splitLines(status, 170, net.minecraft.network.chat.Style.EMPTY)) {
            if (lineY > y + 125) break;
            g.text(Component.literal(line.getString()), x + 202, lineY, MUTED); lineY += 10;
        }
        g.text(Component.translatable("container.inventory"), x + 202, y + 137, INK);
        if (menu.clientPane == 2) drawStorage(g, x, y);
        else drawOffers(g, x, y, mouseX, mouseY);
    }
    private void drawOffers(UiCanvas g, int x, int y, int mouseX, int mouseY) {
        if (menu.clientPane == 1 && menu.kind() == MachineKind.LEVELER) g.text(text("level"), x + 10, y + 72, INK);
        else g.text(text(menu.clientPane == 1 ? "novice_only" : "available_offers"), x + 10, y + 72, MUTED);
        for (int row = 0; row < ROWS; row++) {
            int index = scroll + row, rowY = y + ROW_TOP + row * ROW_HEIGHT;
            if (index >= filtered.size()) break;
            OfferView offer = filtered.get(index).offer();
            boolean hovered = mouseX >= x + 9 && mouseX < x + 177 && mouseY >= rowY && mouseY < rowY + 21;
            boolean chosen = selected == offer.id();
            g.fill(x + 9, rowY, x + 177, rowY + 21, chosen ? 0xff8fbc7c : hovered ? 0xffdbdbdb : 0xffababab);
            g.fill(x + 9, rowY, x + 177, rowY + 1, chosen ? 0xffd5ebc8 : 0xffeeeeee);
            g.fill(x + 9, rowY + 20, x + 177, rowY + 21, 0xff777777);
            g.item(offer.costA(), x + 13, rowY + 2);
            if (!offer.costB().isEmpty()) g.item(offer.costB(), x + 39, rowY + 2);
            arrow(g, x + 63, rowY + 7);
            g.item(offer.result(), x + 80, rowY + 2);
            g.text(trim(Component.literal(itemName(offer.result())), 75), x + 100, rowY + 3, INK);
            g.text(trim(offer.result().getHoverName(), 75), x + 100, rowY + 12, MUTED);
        }
        g.fill(x + 179, y + ROW_TOP, x + 184, y + ROW_TOP + ROWS * ROW_HEIGHT, 0xff656565);
        int handle = maxScroll() == 0 ? 0 : (int) ((ROWS * ROW_HEIGHT - 23) * (double) scroll / maxScroll());
        panel(g, x + 179, y + ROW_TOP + handle, 5, 23);
        if (filtered.isEmpty()) {
            Component empty = !menu.state.hasVillager ? text("insert_villager") : menu.clientPane == 1 && !menu.state.catalogComplete ? text("loading") : query.isBlank() ? text("no_offers") : text("no_results");
            int yy = y + 107;
            for (var line : font.getSplitter().splitLines(empty, 157, net.minecraft.network.chat.Style.EMPTY)) { g.text(Component.literal(line.getString()), x + 14, yy, MUTED); yy += 11; }
        }
        if (menu.clientPane == 0) g.text(text("offer_count", filtered.size()), x + 37, y + 215, MUTED);
        if (menu.clientPane == 1 && !menu.state.catalogComplete && filtered.size() < ROWS) g.text(text("loading_count", menu.catalog.size()), x + 11, y + 198, MUTED);
    }
    private void drawStorage(UiCanvas g, int x, int y) {
        g.text(text("storage"), x + 12, y + 59, INK);
        g.text(text(menu.kind() == MachineKind.BRIBER ? "emeralds" : "input"), x + 22, y + 88, INK);
        if (menu.kind() == MachineKind.LEVELER) g.text(text("output"), x + 106, y + 88, INK);
        Component explanation = text(menu.kind() == MachineKind.BRIBER ? "briber_help" : "leveler_help");
        int yy = y + 163;
        for (var line : font.getSplitter().splitLines(explanation, 164, net.minecraft.network.chat.Style.EMPTY)) { if (yy > y + 202) break; g.text(Component.literal(line.getString()), x + 12, yy, MUTED); yy += 10; }
        if (menu.kind() == MachineKind.BRIBER) {
            g.text(text("reputation", menu.state.reputation, menu.state.reputationLimit), x + 12, y + 202, 0xff315c26);
            g.fill(x + 12, y + 213, x + 180, y + 221, 0xff555555);
            g.fill(x + 13, y + 214, x + 13 + 166 * Math.min(400, menu.state.bribeTicks) / 400, y + 220, 0xff55a84d);
            g.text(text("seconds", menu.state.bribeTicks / 20, 20), x + 60, y + 223, MUTED);
        } else g.text(text("attempts", menu.state.attempts), x + 12, y + 218, MUTED);
    }

    @Override protected void drawForeground(UiCanvas g, int mouseX, int mouseY) {
        if (showSuggestions()) {
            int x = leftPos + 10, y = topPos + 66;
            g.fill(x - 1, y - 1, x + 174, y + completions.size() * 14 + 1, 0xff161616);
            for (int i = 0; i < completions.size(); i++) {
                if (i == suggestion) g.fill(x, y + i * 14, x + 173, y + i * 14 + 14, 0xff45663b);
                g.text(trim(Component.literal(completions.get(i)), 165), x + 3, y + i * 14 + 3, 0xffeeeeee);
            }
            return;
        }
        if (mouseX >= leftPos + 202 && mouseX <= leftPos + 372 && mouseY >= topPos + 115 && mouseY < topPos + 135) g.tooltip(statusLabel(), mouseX, mouseY);
        if (menu.clientPane != 2 && mouseX >= leftPos + 9 && mouseX < leftPos + 177 && mouseY >= topPos + ROW_TOP && mouseY < topPos + ROW_TOP + ROWS * ROW_HEIGHT) {
            int index = scroll + (mouseY - topPos - ROW_TOP) / ROW_HEIGHT;
            if (index < filtered.size()) {
                OfferView offer = filtered.get(index).offer();
                ItemStack hovered = mouseX < leftPos + 37 ? offer.costA() : mouseX < leftPos + 61 && !offer.costB().isEmpty() ? offer.costB() : offer.result();
                g.tooltip(hovered, mouseX, mouseY);
            }
        }
        if (menu.clientPane == 1 && selectedOffer() != null && mouseY >= topPos + 81 && mouseY < topPos + 100) {
            OfferView offer = selectedOffer();
            if (mouseX >= leftPos + 202 && mouseX < leftPos + 220) g.tooltip(offer.costA(), mouseX, mouseY);
            if (mouseX >= leftPos + 228 && mouseX < leftPos + 246 && !offer.costB().isEmpty()) g.tooltip(offer.costB(), mouseX, mouseY);
            if (mouseX >= leftPos + 286 && mouseX < leftPos + 304) g.tooltip(offer.result(), mouseX, mouseY);
        }
    }

    @Override protected boolean screenMouse(double mouseX, double mouseY, int button) {
        if (button != 0) return false;
        if (showSuggestions() && mouseX >= leftPos + 10 && mouseX < leftPos + 184 && mouseY >= topPos + 66 && mouseY < topPos + 66 + completions.size() * 14) {
            complete((int) (mouseY - topPos - 66) / 14); return true;
        }
        if (menu.clientPane != 2 && mouseX >= leftPos + 178 && mouseX <= leftPos + 185 && mouseY >= topPos + ROW_TOP && mouseY < topPos + ROW_TOP + ROWS * ROW_HEIGHT) {
            dragging = true; screenDrag(mouseX, mouseY); return true;
        }
        if (menu.clientPane != 2 && mouseX >= leftPos + 9 && mouseX < leftPos + 177 && mouseY >= topPos + ROW_TOP && mouseY < topPos + ROW_TOP + ROWS * ROW_HEIGHT) {
            int index = scroll + (int) (mouseY - topPos - ROW_TOP) / ROW_HEIGHT;
            if (index < filtered.size()) {
                selected = filtered.get(index).offer().id(); suggestionsDismissed = true; search.setFocused(false);
                if (menu.clientPane == 0) send(UiAction.TRADE, selected);
                updateControls();
            }
            return true;
        }
        return false;
    }
    @Override protected boolean screenDrag(double x, double y) {
        if (!dragging) return false;
        scroll = (int) Math.round(Math.max(0, Math.min(1, (y - topPos - ROW_TOP - 11) / (ROWS * ROW_HEIGHT - 23))) * maxScroll());
        return true;
    }
    @Override protected void endDrag() { dragging = false; }
    @Override protected boolean screenScroll(double x, double y, double delta) {
        if (menu.clientPane == 2 || x < leftPos + 8 || x > leftPos + 185 || y < topPos + 44 || y > topPos + 220) return false;
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta))); return true;
    }
    @Override protected int screenKey(int key, int modifiers) {
        if (search != null && search.isFocused()) {
            if (key == GLFW.GLFW_KEY_ESCAPE) { search.setFocused(false); suggestionsDismissed = true; return 1; }
            if (showSuggestions()) {
                if (key == GLFW.GLFW_KEY_TAB || key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) { complete(suggestion); return 1; }
                if (key == GLFW.GLFW_KEY_DOWN || key == GLFW.GLFW_KEY_UP) { suggestion = Math.floorMod(suggestion + (key == GLFW.GLFW_KEY_DOWN ? 1 : -1), completions.size()); return 1; }
            }
            return 2;
        }
        return 0;
    }
    @Override protected EditBox searchBox() { return search; }
    private void complete(int index) { search.setValue(completions.get(index)); suggestionsDismissed = true; search.setSuggestion(null); }
    private boolean showSuggestions() { return search != null && search.visible && search.isFocused() && !suggestionsDismissed && !completions.isEmpty(); }
    private int maxScroll() { return Math.max(0, filtered.size() - ROWS); }
    private OfferView selectedOffer() { return indexed.stream().map(TradeSearch.Entry::offer).filter(offer -> offer.id() == selected).findFirst().orElse(null); }
    private static String itemName(ItemStack stack) {
        List<String> enchantments = Platform.enchantmentLabels(stack);
        return enchantments.isEmpty() ? stack.getHoverName().getString() : String.join(", ", enchantments);
    }
    private Component professionLabel() {
        String key = menu.state.profession;
        if (key.equals("none") || key.equals("minecraft:none") || !menu.state.hasVillager) return text("villager");
        String[] parts = key.contains(":") ? key.split(":", 2) : new String[]{"minecraft", key};
        return Component.translatable("entity." + parts[0] + ".villager." + parts[1]);
    }
    private Component statusLabel() {
        if (!menu.state.hasVillager) return text("insert_villager");
        if (menu.clientPane == 1 && menu.kind() == MachineKind.ADVANCED_TRADER && !menu.state.canChooseInitial) return text("fresh_villager_required");
        if (menu.kind() == MachineKind.BRIBER) {
            if (menu.state.status.endsWith("no_owner") || menu.state.status.endsWith("reputation_limit") || menu.state.status.endsWith("waiting_emeralds")) return Component.translatable(menu.state.status);
            return text("bribe_status", menu.state.reputation, menu.state.reputationLimit, menu.state.bribeTicks / 20);
        }
        if (!menu.state.status.isBlank()) return Component.translatable(menu.state.status, menu.state.attempts);
        return text(switch (menu.kind()) { case ADVANCED_TRADER -> "advanced_help"; case LEVELER -> "select_target_help"; case DESNEURALIZER -> "desneuralizer_help"; default -> "briber_help"; });
    }
    private int xpProgress() {
        int[] thresholds = {0, 0, 10, 70, 150, 250};
        int level = Math.max(1, Math.min(5, menu.state.level));
        return level == 5 ? 168 : (int) (168 * Math.max(0, Math.min(1, (menu.state.xp - thresholds[level]) / (double) (thresholds[level + 1] - thresholds[level]))));
    }
    private Component trim(Component label, int width) {
        String value = label.getString();
        return font.width(value) <= width ? label : Component.literal(font.plainSubstrByWidth(value, width - font.width("…")) + "…");
    }
    private static void panel(UiCanvas g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, 0xff373737);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xffffffff);
        g.fill(x + 2, y + 2, x + width - 2, y + height - 2, 0xffc6c6c6);
        g.fill(x + 2, y + height - 3, x + width - 1, y + height - 1, 0xff555555);
        g.fill(x + width - 3, y + 2, x + width - 1, y + height - 1, 0xff555555);
    }
    private static void slot(UiCanvas g, int x, int y, int width, int height) {
        g.fill(x, y, x + width, y + height, 0xff373737);
        g.fill(x + 1, y + 1, x + width, y + height, 0xffffffff);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xff8b8b8b);
    }
    private static void arrow(UiCanvas g, int x, int y) {
        g.fill(x, y + 2, x + 13, y + 5, 0xff707070);
        for (int i = 0; i < 6; i++) g.fill(x + 10 + i, y - 1 + i / 2, x + 11 + i, y + 8 - i / 2, 0xff707070);
    }
}
