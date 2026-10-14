package net.gasharaujo.easyvillageraddon.client;
import net.gasharaujo.easyvillageraddon.MachineMenu;
import net.gasharaujo.easyvillageraddon.UiSnapshot;
import net.minecraft.client.Minecraft;
public final class ClientUi {
    public static void receive(UiSnapshot state) {
        var player = Minecraft.getInstance().player;
        if (player != null && player.containerMenu instanceof MachineMenu menu && menu.containerId == state.menuId) menu.receive(state);
    }
}
