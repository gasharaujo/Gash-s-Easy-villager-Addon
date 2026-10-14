package net.gasharaujo.easyvillageraddon.client;

import net.gasharaujo.easyvillageraddon.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.glfw.GLFW;
import static net.gasharaujo.easyvillageraddon.client.VanillaUi.*;

/** Each machine has its own compact layout. Only the Advanced Trader has trading slots. */
public final class MachineScreen extends VersionedScreen {
    private final Inventory inventory;
    private final MachineKind screenKind;
    private EditBox search;
    private Button action, choose;
    private Button targetType, matchMode, selectedView, clearTargets;
    private final List<Button> rows = new ArrayList<>(), levels = new ArrayList<>();
    private List<TradeSearch.Entry> indexed = new ArrayList<>(), filtered = List.of();
    private List<String> completions = List.of();
    private Object indexedOffers;
    private int indexedRevision = -1, indexedSize, scroll, selected = -1, suggestion;
    private int indexedTargets = -1;
    private boolean indexedGeneric, indexedSelected;
    private final java.util.Set<net.minecraft.world.item.Item> genericItems = new java.util.HashSet<>();
    private String query = "";
    private boolean dragging, suggestionsDismissed, switchingView;

    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title); this.inventory = inventory; screenKind = menu.kind();
    }
    private static Component text(String key, Object... args) { return Component.translatable("gui.gashs_easy_villager_addon." + key, args); }
    private boolean picker() { return MachineLayout.picker(menu.kind(), menu.clientPane); }
    private boolean trader() { return menu.kind() == MachineKind.ADVANCED_TRADER && menu.clientPane == 0; }
    private int rowTop() { return picker() ? menu.kind() == MachineKind.LEVELER ? 82 : 62 : 18; }
    private int rowCount() { return picker() ? 6 : 7; }

    @Override protected void init() {
        super.init(); rows.clear(); levels.clear();
        if (picker()) {
            search = new EditBox(font, leftPos + 9, topPos + 23, 258, 16, text("search"));
            search.setMaxLength(100); search.setHint(text("search_hint")); search.setValue(query);
            search.setResponder(value -> { query = value; scroll = 0; suggestionsDismissed = false; updateFilter(); });
            addRenderableWidget(search);
            if (menu.kind() == MachineKind.LEVELER) {
                targetType = button(8, 44, 100, 16, text("exact_trade"), () -> { menu.clientGenericTargets = !menu.clientGenericTargets; updateSource(); updateControls(); });
                targetType.setTooltip(Tooltip.create(text("generic_help")));
                matchMode = button(112, 44, 42, 16, text("match_any"), () -> send(UiAction.MATCH_MODE, menu.state.matchAll ? 0 : 1));
                matchMode.setTooltip(Tooltip.create(text("match_help")));
                selectedView = button(158, 44, 110, 16, text("targets_count", 0), () -> { menu.clientSelectedTargets = !menu.clientSelectedTargets; updateSource(); updateControls(); });
                selectedView.setTooltip(Tooltip.create(text("selection_help", TradeTarget.MAX_TARGETS)));
                clearTargets = button(97, imageHeight - 26, 48, 20, text("clear_targets"), () -> send(UiAction.CLEAR_TARGETS, 0));
            }
            if (menu.kind() == MachineKind.LEVELER) for (int i = 1; i <= 5; i++) {
                final int level = i;
                levels.add(button(109 + (i - 1) * 29, 64, 27, 16, Component.literal(Integer.toString(i)), () -> {
                    selected = -1; send(UiAction.LEVEL, level);
                }));
            }
            button(8, imageHeight - 26, 85, 20, text("back"), () -> pane(0));
            choose = button(149, imageHeight - 26, 119, 20, text(menu.kind() == MachineKind.LEVELER ? "done" : "select"), () -> {
                if (menu.kind() == MachineKind.LEVELER) { pane(0); return; }
                if (selected < 0) return;
                send(UiAction.CHOOSE, selected); pane(0);
            });
        } else if (trader()) {
            action = button(105, 5, 20, 18, Component.empty(), () -> { if (menu.state.enabled) send(UiAction.TOGGLE, 0); else pane(1); });
            action.setTooltip(Tooltip.create(text("choose_initial")));
            button(253, 5, 17, 16, Component.literal("…"), () -> pane(2)).setTooltip(Tooltip.create(text("manage")));
        } else if (menu.kind() == MachineKind.LEVELER) {
            button(83, 22, 85, 20, text("choose_target"), () -> pane(1));
            action = button(63, 99, 50, 20, text("start"), () -> send(UiAction.TOGGLE, 0));
        } else if (menu.kind() == MachineKind.ADVANCED_TRADER) button(54, 56, 68, 18, text("back"), () -> pane(0));
        if (picker() || trader()) for (int i = 0; i < rowCount(); i++) {
            final int row = i;
            rows.add(button(picker() ? 8 : 5, rowTop() + i * 20, picker() ? 248 : 88, 20, Component.empty(), () -> {
                updateSource();
                int index = scroll + row;
                if (index >= filtered.size()) return;
                selected = filtered.get(index).offer().id(); suggestionsDismissed = true;
                if (search != null) search.setFocused(false);
                if (picker() && menu.kind() == MachineKind.LEVELER) send(menu.clientSelectedTargets ? UiAction.REMOVE_TARGET : menu.clientGenericTargets ? UiAction.TARGET_GENERIC : UiAction.TARGET_EXACT, selected);
                else if (trader()) send(UiAction.TRADE, selected);
                updateControls();
            }));
        }
        updateSource(); updateControls();
    }
    private Button button(int x, int y, int w, int h, Component label, Runnable run) {
        return addRenderableWidget(Button.builder(label, button -> run.run()).bounds(leftPos + x, topPos + y, w, h).build());
    }
    public void pane(int pane) {
        if (menu.clientPane == pane) return;
        send(UiAction.PANE, pane); menu.setPane(pane); reopen();
    }
    private void reopen() { switchingView = true; Minecraft.getInstance().setScreen(new MachineScreen(menu, inventory, title)); }
    // View transitions retain the same inventory session and carried item.
    @Override public void removed() { if (!switchingView) super.removed(); }
    private void send(int action, int value) { UiNetwork.action(new UiAction(menu.containerId, action, value, action == UiAction.REMOVE_TARGET ? menu.state.targetsRevision : menu.state.revision)); }

    @Override protected void containerTick() {
        super.containerTick();
        if (screenKind != menu.kind()) { reopen(); return; }
        updateSource(); updateControls();
    }
    private void updateSource() {
        boolean changed = false;
        if (picker()) {
            if (indexedRevision != menu.state.revision || indexedSize > menu.catalog.size()
                    || indexedGeneric != menu.clientGenericTargets || indexedSelected != menu.clientSelectedTargets
                    || menu.clientSelectedTargets && indexedTargets != menu.state.targetsRevision) {
                indexed.clear(); genericItems.clear(); indexedSize = 0; indexedRevision = menu.state.revision;
                indexedGeneric = menu.clientGenericTargets; indexedSelected = menu.clientSelectedTargets; indexedTargets = menu.state.targetsRevision;
                selected = -1; scroll = 0; changed = true;
            }
            if (menu.kind() == MachineKind.LEVELER && menu.clientSelectedTargets) {
                if (indexed.isEmpty()) for (int i = 0; i < menu.state.targets.size(); i++) {
                    var goal = menu.state.targets.get(i); indexed.add(TradeSearch.index(new OfferView(i, goal.costA(), goal.costB(), goal.result()))); changed = true;
                }
            } else while (indexedSize < menu.catalog.size()) {
                var offer = menu.catalog.get(indexedSize++);
                if (menu.kind() != MachineKind.LEVELER || !menu.clientGenericTargets) indexed.add(TradeSearch.index(offer));
                else if (genericItems.add(offer.result().getItem())) indexed.add(TradeSearch.index(new OfferView(offer.id(), ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(offer.result().getItem()))));
                changed = true;
            }
        } else if (trader() && indexedOffers != menu.getOffers()) {
            indexed.clear(); indexedOffers = menu.getOffers();
            for (int i = 0; i < menu.getOffers().size(); i++) indexed.add(TradeSearch.index(OfferView.of(i, menu.getOffers().get(i))));
            changed = true;
        }
        if (changed) updateFilter();
    }
    private void updateFilter() {
        filtered = indexed.stream().filter(entry -> TradeSearch.matches(entry, query)).toList();
        completions = TradeSearch.suggest(indexed, query); suggestion = 0; scroll = Math.min(scroll, maxScroll());
        if (search != null) {
            String completion = completions.isEmpty() ? "" : completions.get(0);
            search.setSuggestion(!query.isEmpty() && completion.regionMatches(true, 0, query, 0, query.length()) ? completion.substring(query.length()) : null);
        }
    }
    private void updateControls() {
        for (int i = 0; i < rows.size(); i++) { rows.get(i).visible = scroll + i < filtered.size(); rows.get(i).active = !menu.state.enabled || picker(); }
        if (choose != null) choose.active = menu.kind() == MachineKind.LEVELER || selected >= 0 && menu.state.canChooseInitial;
        for (int i = 0; i < levels.size(); i++) { levels.get(i).active = menu.state.targetLevel != i + 1; levels.get(i).visible = !menu.clientSelectedTargets; }
        if (targetType != null) {
            targetType.setMessage(text(menu.clientGenericTargets ? "generic_item" : "exact_trade")); targetType.active = !menu.clientSelectedTargets;
            matchMode.setMessage(text(menu.state.matchAll ? "match_all" : "match_any"));
            selectedView.setMessage(text(menu.clientSelectedTargets ? "catalog" : "targets_count", menu.state.targets.size()));
            clearTargets.active = !menu.state.targets.isEmpty();
        }
        if (action != null) {
            action.active = menu.kind() == MachineKind.ADVANCED_TRADER ? menu.state.canChooseInitial || menu.state.enabled : menu.state.hasVillager && menu.state.hasWorkstation && !menu.state.target.isEmpty();
            if (menu.kind() == MachineKind.LEVELER) action.setMessage(text(menu.state.enabled ? "pause" : "start"));
            else action.setTooltip(Tooltip.create(text(menu.state.enabled ? "cancel_search" : "choose_initial")));
        }
    }

    @Override protected void drawBackground(UiCanvas g, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        if (trader()) {
            g.texture(TRADER, x, y, 276, 166, 0, 0, 276, 166, 512, 256);
            centered(g, Component.translatable("merchant.trades"), x + 49, y + 6, 90);
            Component name = menu.state.name.isBlank() ? professionLabel() : Component.literal(menu.state.name);
            centered(g, Component.translatable("merchant.title", name, Component.translatable("merchant.level." + Math.max(1, menu.state.level))), x + 187, y + 6, 124);
            g.text(Component.translatable("container.inventory"), x + 107, y + 72, INK);
            g.experienceBar(x + 136, y + 16, xpProgress());
            if (menu.state.enabled) centered(g, text("searching_short"), x + 187, y + 61, 154);
            drawScroller(g); return;
        }
        if (menu.kind() == MachineKind.DESNEURALIZER) {
            blit(g, IO, x, y, 0, 0, 176, 164);
            centered(g, text("input_villagers"), x + 88, y + 9, 160);
            centered(g, text("output_villagers"), x + 88, y + 40, 160);
        } else {
            frame(g, x, y, imageWidth, imageHeight);
            centered(g, picker() ? text(menu.kind() == MachineKind.ADVANCED_TRADER ? "choose_initial" : "choose_target") : Component.literal(menu.kind().title), x + imageWidth / 2, y + 6, imageWidth - 16);
            if (picker()) {
                if (menu.kind() == MachineKind.LEVELER) g.text(text(menu.clientSelectedTargets ? "selected_targets" : "level"), x + 9, y + 68, INK);
                else g.text(text("novice_only"), x + 9, y + 48, INK);
                drawScroller(g); return;
            }
            for (var slot : menu.slots) if (slot.isActive()) slot(g, x + slot.x, y + slot.y);
            if (menu.kind() == MachineKind.BRIBER) {
                g.text(Component.literal("+1"), x + 137, y + 22, INK);
                g.text(Component.literal("20s"), x + 132, y + 49, INK); arrow(g, x + 130, y + 33);
                g.fill(x + 130, y + 65, x + 164, y + 68, 0xff8b8b8b);
                g.fill(x + 130, y + 65, x + 130 + 34 * Math.min(400, menu.state.bribeTicks) / 400, y + 68, 0xff568236);
            } else if (menu.kind() == MachineKind.LEVELER) {
                slot(g, x + 62, y + 24); g.item(menu.state.target, x + 62, y + 24);
                if (menu.state.targets.size() > 1) { String count = Integer.toString(menu.state.targets.size()); g.text(Component.literal(count), x + 78 - font.width(count), y + 33, 0xffffffff); }
                g.text(text("input"), x + 8, y + 53, INK); g.text(text("output"), x + 116, y + 53, INK);
                arrow(g, x + 76, y + 75); centered(g, text(shortStatus()), x + 88, y + 121, 160);
            }
        }
        int ix = MachineLayout.inventoryX(menu.kind(), menu.clientPane), iy = MachineLayout.inventoryY(menu.kind(), menu.clientPane);
        g.text(Component.translatable("container.inventory"), x + ix, y + iy - 11, INK);
    }
    private void drawScroller(UiCanvas g) {
        int x = leftPos + (picker() ? 260 : 94), y = topPos + rowTop(), h = rowCount() * 20 - 1;
        g.fill(x, y, x + 6, y + h, 0xff555555);
        int offset = maxScroll() == 0 ? 0 : (int) ((h - 27) * (double) scroll / maxScroll());
        g.scroller(x, y + offset, maxScroll() > 0);
    }
    @Override protected void drawForeground(UiCanvas g, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        if (trader() || picker()) {
            for (int row = 0; row < rowCount() && scroll + row < filtered.size(); row++) {
                OfferView offer = filtered.get(scroll + row).offer(); int yy = y + rowTop() + row * 20 + 2, xx = x + (picker() ? 13 : 10);
                var rule = ruleFor(offer); boolean generic = rule != null && rule.generic();
                if (generic) {
                    g.item(offer.result(), xx, yy); g.text(trim(text("any_item", offer.result().getHoverName()), 182), x + 36, yy + 4, INK);
                } else {
                    g.item(offer.costA(), xx, yy); g.item(offer.costB(), xx + 30, yy);
                    g.tradeArrow(xx + 50, yy + 3); g.item(offer.result(), xx + 63, yy);
                    if (picker()) g.text(trim(Component.literal(itemName(offer.result())), menu.kind() == MachineKind.LEVELER ? 120 : 145), x + 105, yy + 4, INK);
                }
                if (picker() && menu.kind() == MachineKind.LEVELER) {
                    boolean marked = rule != null && menu.state.targets.stream().anyMatch(rule::sameDefinition);
                    g.text(Component.literal(marked ? "[x]" : "[ ]"), x + 235, yy + 4, INK);
                } else if (picker() && selected == offer.id()) g.fill(x + 9, yy + 1, x + 11, yy + 16, 0xff404040);
            }
            if (filtered.isEmpty()) centered(g, text(!menu.state.hasVillager ? "insert_villager" : picker() && !menu.state.catalogComplete ? "loading" : "no_results"), x + (picker() ? 138 : 49), y + rowTop() + 14, picker() ? 240 : 84);
        }
        if (trader()) {
            if (action != null && action.active) g.texture("easy_villagers:textures/gui/container/arrow_button.png", x + 106, y + 7, 18, 14, 0, 0, 18, 14, 32, 32);
            if (inside(mouseX, mouseY, 136, 16, 102, 5)) g.tooltip(text("level_xp", Component.translatable("merchant.level." + menu.state.level), menu.state.xp), mouseX, mouseY);
        }
        if (showSuggestions()) {
            int yy = y + 40; g.fill(x + 8, yy, x + 268, yy + completions.size() * 14 + 2, 0xff101010);
            for (int i = 0; i < completions.size(); i++) {
                if (i == suggestion) g.fill(x + 9, yy + i * 14 + 1, x + 267, yy + i * 14 + 15, 0xff555555);
                g.text(trim(Component.literal(completions.get(i)), 250), x + 12, yy + i * 14 + 4, 0xffeeeeee);
            }
            return;
        }
        if (trader() || picker()) {
            if (inside(mouseX, mouseY, picker() ? 8 : 5, rowTop(), picker() ? 248 : 88, rowCount() * 20)) {
                int index = scroll + (mouseY - y - rowTop()) / 20;
                if (index < filtered.size()) {
                    var offer = filtered.get(index).offer(); int dx = mouseX - x - (picker() ? 8 : 5);
                    var goal = ruleFor(offer);
                    if (goal != null && goal.generic()) { g.tooltip(text("any_item", goal.result().getHoverName()).copy().append("\n").append(text("target_level", goal.level())), mouseX, mouseY); return; }
                    ItemStack hovered = dx < 25 ? offer.costA() : dx >= 30 && dx < 52 && !offer.costB().isEmpty() ? offer.costB() : offer.result();
                    if (picker() && dx < 25) g.tooltip(hovered.getHoverName().copy().append("\n").append(text("natural_price_help")), mouseX, mouseY);
                    else g.tooltip(hovered, mouseX, mouseY);
                }
            }
        } else {
            if (menu.kind() == MachineKind.LEVELER && inside(mouseX, mouseY, 61, 23, 18, 18)) {
                if (menu.state.target.isEmpty()) g.tooltip(text("choose_target"), mouseX, mouseY); else g.tooltip(targetsTooltip(), mouseX, mouseY);
            }
            if (menu.kind() == MachineKind.BRIBER && inside(mouseX, mouseY, 129, 20, 36, 49)) g.tooltip(text("briber_help").copy().append("\n").append(text("bribe_status", menu.state.reputation, menu.state.reputationLimit, menu.state.bribeTicks / 20)), mouseX, mouseY);
            if (menu.kind() == MachineKind.LEVELER && inside(mouseX, mouseY, 8, 120, 160, 10) && !menu.state.status.isBlank()) g.tooltip(Component.translatable(menu.state.status, menu.state.attempts), mouseX, mouseY);
            for (int i : new int[]{MachineMenu.VILLAGER, MachineMenu.WORKSTATION}) {
                var slot = menu.getSlot(i);
                if (slot.isActive() && !slot.hasItem() && inside(mouseX, mouseY, slot.x, slot.y, 16, 16)) g.tooltip(text(i == MachineMenu.VILLAGER ? "villager" : "workstation"), mouseX, mouseY);
            }
        }
    }
    private boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= leftPos + x && mx < leftPos + x + w && my >= topPos + y && my < topPos + y + h; }
    @Override protected boolean screenMouse(double x, double y, int button) {
        if (button != 0) return false;
        if (showSuggestions() && inside(x, y, 8, 40, 260, completions.size() * 14 + 2)) { complete(Math.min(completions.size() - 1, Math.max(0, (int) (y - topPos - 41) / 14))); return true; }
        if ((trader() || picker()) && inside(x, y, picker() ? 259 : 93, rowTop(), 8, rowCount() * 20)) { dragging = true; screenDrag(x, y); return true; }
        return false;
    }
    @Override protected boolean screenDrag(double x, double y) {
        if (!dragging) return false;
        scroll = (int) Math.round(Math.max(0, Math.min(1, (y - topPos - rowTop() - 13.5) / (rowCount() * 20 - 28))) * maxScroll()); return true;
    }
    @Override protected void endDrag() { dragging = false; }
    @Override protected boolean screenScroll(double x, double y, double delta) {
        if (!(trader() || picker()) || !inside(x, y, 5, rowTop(), picker() ? 264 : 96, rowCount() * 20)) return false;
        scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) Math.signum(delta))); return true;
    }
    @Override protected int screenKey(int key, int modifiers) {
        if (key == GLFW.GLFW_KEY_ESCAPE && menu.clientPane != 0) { pane(0); return 1; }
        if (search != null && search.isFocused()) {
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
    private boolean showSuggestions() { return search != null && search.isFocused() && !suggestionsDismissed && !completions.isEmpty(); }
    private int maxScroll() { return Math.max(0, filtered.size() - rowCount()); }
    private TradeTarget ruleFor(OfferView offer) {
        if (!picker() || menu.kind() != MachineKind.LEVELER) return null;
        if (menu.clientSelectedTargets) return offer.id() >= 0 && offer.id() < menu.state.targets.size() ? menu.state.targets.get(offer.id()) : null;
        return new TradeTarget(menu.state.targetLevel, menu.clientGenericTargets, offer.result(), offer.costA(), offer.costB());
    }
    private Component targetsTooltip() {
        var label = text(menu.state.matchAll ? "match_all_help" : "match_any_help").copy();
        for (var goal : menu.state.targets.stream().limit(8).toList()) label.append("\n").append(goal.generic() ? text("any_item", goal.result().getHoverName()) : Component.literal(itemName(goal.result()))).append(" · ").append(text("target_level", goal.level()));
        if (menu.state.targets.size() > 8) label.append("\n…");
        return label;
    }
    private static String itemName(ItemStack stack) {
        var enchantments = Platform.enchantmentLabels(stack); return enchantments.isEmpty() ? stack.getHoverName().getString() : String.join(", ", enchantments);
    }
    private Component professionLabel() {
        String key = menu.state.profession;
        if (key.equals("none") || key.equals("minecraft:none") || !menu.state.hasVillager) return text("villager");
        String[] parts = key.contains(":") ? key.split(":", 2) : new String[]{"minecraft", key};
        return Component.translatable("entity." + parts[0] + ".villager." + parts[1]);
    }
    private String shortStatus() {
        if (!menu.state.hasVillager) return "insert_short";
        if (menu.state.status.endsWith("target_found")) return "found_short";
        if (menu.state.target.isEmpty()) return "choose_target";
        return menu.state.enabled ? "running_short" : "paused_short";
    }
    private int xpProgress() {
        int[] thresholds = {0, 0, 10, 70, 150, 250}; int level = Math.max(1, Math.min(5, menu.state.level));
        return level == 5 ? 100 : (int) (100 * Math.max(0, Math.min(1, (menu.state.xp - thresholds[level]) / (double) (thresholds[level + 1] - thresholds[level]))));
    }
    private void centered(UiCanvas g, Component label, int centerX, int y, int maxWidth) { Component value = trim(label, maxWidth); g.text(value, centerX - font.width(value) / 2, y, INK); }
    private Component trim(Component label, int width) { return font.width(label) <= width ? label : Component.literal(font.plainSubstrByWidth(label.getString(), width - font.width("…")) + "…"); }
}
