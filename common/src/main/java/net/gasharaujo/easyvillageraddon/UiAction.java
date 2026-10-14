package net.gasharaujo.easyvillageraddon;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

public record UiAction(int menuId, int action, int value, int revision) {
    public static final int TOGGLE = 0, LEVEL = 1, CHOOSE = 2, TRADE = 3, RESET = 4, REFRESH = 5, PANE = 6;
    public void write(FriendlyByteBuf b) {
        b.writeVarInt(menuId); b.writeVarInt(action); b.writeVarInt(value); b.writeVarInt(revision);
    }
    public static UiAction read(FriendlyByteBuf b) {
        return new UiAction(b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt());
    }
    public void handle(ServerPlayer player) {
        if (player.containerMenu instanceof MachineMenu menu && menu.containerId == menuId && menu.stillValid(player)) menu.act(player, this);
    }
}
