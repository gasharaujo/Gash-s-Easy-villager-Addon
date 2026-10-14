package net.gasharaujo.easyvillageraddon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
public final class UiNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(EasyVillagerAddon.MOD_ID, "ui"), () -> "3", "3"::equals, "3"::equals);
    public static void register(IEventBus bus) {
        CHANNEL.messageBuilder(UiSnapshot.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(UiSnapshot::write).decoder(UiSnapshot::read)
                .consumerMainThread((state, context) -> net.gasharaujo.easyvillageraddon.client.ClientUi.receive(state)).add();
        CHANNEL.messageBuilder(UiAction.class, 1, NetworkDirection.PLAY_TO_SERVER)
                .encoder(UiAction::write).decoder(UiAction::read)
                .consumerMainThread((action, context) -> { var player = context.get().getSender(); if (player != null) action.handle(player); }).add();
    }
    public static void send(ServerPlayer player, UiSnapshot state) { CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), state); }
    public static void action(UiAction action) { CHANNEL.sendToServer(action); }
    public static void writeStack(FriendlyByteBuf buffer, ItemStack stack) { buffer.writeItem(stack); }
    public static ItemStack readStack(FriendlyByteBuf buffer) { return buffer.readItem(); }
}
