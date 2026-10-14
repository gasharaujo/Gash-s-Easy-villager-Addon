package net.gasharaujo.easyvillageraddon;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Vanilla text-entry protocol: typing searches without custom client packets or XP cost. */
final class SearchMenu extends AnvilMenu {
    private final MachineEntity machine;
    private String query;

    SearchMenu(int id, Inventory inventory, MachineEntity machine, String previous) {
        super(id, inventory);
        this.machine = machine;
        query = previous;
        ItemStack paper = new ItemStack(Items.PAPER);
        Platform.name(paper, previous);
        inputSlots.setItem(0, paper);
        createResult();
    }

    @Override public boolean setItemName(String name) {
        if (machine == null || name == null || name.length() > 50) return false;
        query = name;
        machine.lastSearch.put(player.getUUID(), query);
        createResult();
        return true;
    }

    @Override public void createResult() {
        if (resultSlots == null) return;
        ItemStack apply = new ItemStack(Items.COMPASS);
        Platform.name(apply, "Aplicar pesquisa (sem custo)");
        resultSlots.setItem(0, apply);
        broadcastChanges();
    }

    @Override public boolean stillValid(Player player) { return machine != null && machine.accessible(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) { return ItemStack.EMPTY; }
    @Override public void clicked(int index, int button, ClickType click, Player player) {
        if (index == 2 && click == ClickType.PICKUP && button == 0 && stillValid(player)) machine.open(player, query);
    }
    @Override public void removed(Player player) {
        inputSlots.clearContent();
        resultSlots.clearContent();
        super.removed(player);
    }
}
