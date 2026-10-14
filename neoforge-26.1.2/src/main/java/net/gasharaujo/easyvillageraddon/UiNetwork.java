package net.gasharaujo.easyvillageraddon;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
public final class UiNetwork {
    public static void register(IEventBus bus) { bus.addListener(UiNetwork::registerPayloads); }
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("4");
        registrar.playToClient(StatePayload.TYPE, StatePayload.CODEC, (message, context) ->
                context.enqueueWork(() -> net.gasharaujo.easyvillageraddon.client.ClientUi.receive(message.state())));
        registrar.playToServer(ActionPayload.TYPE, ActionPayload.CODEC, (message, context) ->
                context.enqueueWork(() -> { if (context.player() instanceof ServerPlayer player) message.action().handle(player); }));
    }
    public static void send(ServerPlayer player, UiSnapshot state) {
        net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(player, new StatePayload(state));
    }
    public static void action(UiAction action) {
        net.neoforged.neoforge.client.network.ClientPacketDistributor.sendToServer(new ActionPayload(action));
    }
    public static void writeStack(FriendlyByteBuf buffer, ItemStack stack) { ItemStack.OPTIONAL_STREAM_CODEC.encode((RegistryFriendlyByteBuf) buffer, stack); }
    public static ItemStack readStack(FriendlyByteBuf buffer) { return ItemStack.OPTIONAL_STREAM_CODEC.decode((RegistryFriendlyByteBuf) buffer); }
    public record StatePayload(UiSnapshot state) implements CustomPacketPayload {
        static final Type<StatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(EasyVillagerAddon.MOD_ID, "ui_state"));
        static final StreamCodec<RegistryFriendlyByteBuf, StatePayload> CODEC = StreamCodec.of((buffer, payload) -> payload.state.write(buffer), buffer -> new StatePayload(UiSnapshot.read(buffer)));
        @Override public Type<StatePayload> type() { return TYPE; }
    }
    public record ActionPayload(UiAction action) implements CustomPacketPayload {
        static final Type<ActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath(EasyVillagerAddon.MOD_ID, "ui_action"));
        static final StreamCodec<RegistryFriendlyByteBuf, ActionPayload> CODEC = StreamCodec.of((buffer, payload) -> payload.action.write(buffer), buffer -> new ActionPayload(UiAction.read(buffer)));
        @Override public Type<ActionPayload> type() { return TYPE; }
    }
}
