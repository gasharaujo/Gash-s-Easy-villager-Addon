package net.gasharaujo.easyvillageraddon;

import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Uses synchronized vanilla inventory menus; all control slots remain server owned. */
final class MachineMenu extends ChestMenu {
    final MachineEntity machine;
    private final SimpleContainer view;
    private final String filter;
    private List<MerchantOffer> visible = new ArrayList<>();
    private int page;
    private long lastRefresh = -1;

    MachineMenu(int id, Inventory playerInventory, MachineEntity machine, String filter) {
        this(id, playerInventory, machine, filter, new MachineView(machine));
    }

    private MachineMenu(int id, Inventory playerInventory, MachineEntity machine, String filter, SimpleContainer view) {
        super(MenuType.GENERIC_9x6, id, playerInventory, view, 6);
        this.machine = machine;
        this.view = view;
        this.filter = filter;
        for (int i = 0; i < 54; i++) {
            final int index = i;
            Slot slot = new Slot(view, i, 8 + (i % 9) * 18, 18 + (i / 9) * 18) {
                @Override public boolean mayPlace(ItemStack stack) { return index >= 9 && index < 18; }
                @Override public boolean mayPickup(Player player) { return index >= 9 && index < 27; }
            };
            slot.index = i;
            slots.set(i, slot);
        }
        machine.rebuildCatalog();
        refresh();
    }

    private static ItemStack icon(net.minecraft.world.item.Item item, String label) {
        ItemStack stack = new ItemStack(item);
        Platform.name(stack, label);
        return stack;
    }

    private void refresh() {
        view.setItem(0, machine.hasVillager() ? machine.getVillager().copy() : icon(Items.VILLAGER_SPAWN_EGG, "Coloque/retire o villager aqui"));
        view.setItem(1, machine.hasWorkstation() ? new ItemStack(machine.getWorkstation()) : icon(Items.LECTERN, "Coloque/retire a estacao de trabalho aqui"));
        view.setItem(2, icon(Items.COMPASS, "Pesquisar: " + filter));
        view.setItem(3, icon(Items.ARROW, "Pagina anterior"));
        view.setItem(4, icon(Items.ARROW, "Proxima pagina (" + (page + 1) + ")"));
        view.setItem(5, icon(Items.EXPERIENCE_BOTTLE, "Nivel alvo: " + (machine.kind() == MachineKind.ADVANCED_TRADER ? 1 : machine.targetLevel)));
        String action = switch (machine.kind()) {
            case ADVANCED_TRADER -> "Abrir trocas sem esgotamento";
            case LEVELER -> machine.enabled ? "Pausar Leveler" : "Iniciar Leveler";
            case DESNEURALIZER -> "Remover profissao e zerar nivel/XP";
            case BRIBER -> "32 esmeraldas / 20s = +1 reputacao";
        };
        view.setItem(6, icon(machine.enabled ? Items.REDSTONE_TORCH : Items.LEVER, action));
        view.setItem(7, machine.targetResult.isEmpty() ? icon(Items.PAPER, "Linha 2: entrada | linha 3: saida") : machine.targetResult.copy());
        String status = machine.status;
        if (machine.catalog != null && !machine.catalog.complete()) status = "Carregando catalogo... " + machine.catalog.offers.size();
        view.setItem(8, icon(Items.CLOCK, status.isEmpty() ? "Coloque um villager e uma estacao de trabalho" : status));
        visible = new ArrayList<>();
        if (machine.catalog != null && (machine.kind() == MachineKind.ADVANCED_TRADER || machine.kind() == MachineKind.LEVELER)) {
            for (MerchantOffer offer : machine.catalog.offers) {
                if (normalize(Platform.searchText(offer.getResult()) + " " + Platform.searchText(offer.getBaseCostA())
                        + " " + Platform.searchText(offer.getCostB())).contains(normalize(filter))) visible.add(offer);
            }
        }
        page = Math.min(page, Math.max(0, (visible.size()-1)/27));
        for (int i = 0; i < 27; i++) {
            int index = page * 27 + i;
            ItemStack display = ItemStack.EMPTY;
            if (index < visible.size()) {
                MerchantOffer offer = visible.get(index);
                display = offer.getResult().copy();
                String cost = offer.getCostA().getCount() + "x " + offer.getCostA().getHoverName().getString();
                if (!offer.getCostB().isEmpty()) cost += " + " + offer.getCostB().getCount() + "x " + offer.getCostB().getHoverName().getString();
                Platform.name(display, display.getHoverName().getString() + " <- " + cost);
            }
            view.setItem(27+i, display);
        }
    }

    static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    @Override public void broadcastChanges() {
        if (machine != null && machine.getLevel().getGameTime() / 10 != lastRefresh) {
            lastRefresh = machine.getLevel().getGameTime() / 10;
            refresh();
        }
        super.broadcastChanges();
    }

    @Override public boolean stillValid(Player player) { return machine.accessible(player); }

    @Override public void clicked(int index, int button, ClickType click, Player player) {
        if (!stillValid(player)) return;
        if (index >= 0 && index < 54 && !(index >= 9 && index < 27)) {
            if (click != ClickType.PICKUP || button != 0) return;
            switch (index) {
                case 0 -> machine.takeVillager(player);
                case 1 -> {
                    if (machine.kind() == MachineKind.ADVANCED_TRADER || machine.kind() == MachineKind.LEVELER) machine.workstation(player);
                }
                case 2 -> {
                    if (!getCarried().isEmpty()) return;
                    player.openMenu(new SimpleMenuProvider((id, inventory, p) -> new SearchMenu(id, inventory, machine, filter),
                            Component.literal("Pesquisar; Esc e reabra o bloco")));
                    return;
                }
                case 3 -> page = Math.max(0, page-1);
                case 4 -> page = Math.min(page+1, Math.max(0, (visible.size()-1)/27));
                case 5 -> {
                    if (machine.kind() == MachineKind.LEVELER) {
                        machine.targetLevel = machine.targetLevel % 5 + 1;
                        machine.enabled = false;
                        machine.targetResult = ItemStack.EMPTY;
                        machine.rebuildCatalog();
                    }
                }
                case 6 -> {
                    if (!getCarried().isEmpty()) return;
                    if (machine.kind() == MachineKind.ADVANCED_TRADER) {
                        player.closeContainer(); machine.openTradingGUI(player); return;
                    }
                    if (machine.kind() == MachineKind.DESNEURALIZER) machine.resetProfession(false);
                    if (machine.kind() == MachineKind.LEVELER && !machine.targetResult.isEmpty()) machine.enabled = !machine.enabled;
                }
                default -> {
                    int choice = page * 27 + index - 27;
                    if (index >= 27 && choice >= 0 && choice < visible.size()) machine.select(visible.get(choice));
                }
            }
            machine.setChanged();
            refresh(); broadcastChanges();
            return;
        }
        // Double-click collection must never collect the preview copies.
        if (click == ClickType.PICKUP_ALL || click == ClickType.CLONE) return;
        super.clicked(index, button, click, player);
        machine.setChanged();
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        boolean moved;
        if (index >= 54) moved = moveItemStackTo(source, 9, 18, false);
        else if (index >= 9 && index < 27) moved = moveItemStackTo(source, 54, slots.size(), true);
        else return ItemStack.EMPTY;
        if (!moved) return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        machine.setChanged();
        return original;
    }

    private static final class MachineView extends SimpleContainer {
        private final MachineEntity machine;
        MachineView(MachineEntity machine) { super(54); this.machine = machine; }
        private boolean real(int index) { return index >= 9 && index < 27; }
        @Override public ItemStack getItem(int index) { return real(index) ? machine.inventory.get(index) : super.getItem(index); }
        @Override public void setItem(int index, ItemStack stack) {
            if (real(index)) { machine.inventory.set(index, stack); machine.setChanged(); }
            else super.setItem(index, stack);
        }
        @Override public ItemStack removeItem(int index, int amount) {
            if (!real(index)) return ItemStack.EMPTY;
            ItemStack result = machine.inventory.get(index).split(amount); machine.setChanged(); return result;
        }
        @Override public ItemStack removeItemNoUpdate(int index) {
            if (!real(index)) return ItemStack.EMPTY;
            ItemStack result = machine.inventory.get(index); machine.inventory.set(index, ItemStack.EMPTY); machine.setChanged(); return result;
        }
        @Override public void setChanged() { if (machine != null) machine.setChanged(); }
    }
}
