package net.gasharaujo.easyvillageraddon;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;

/** A catalog preview is UI data, never an inventory slot. */
public record OfferView(int id, ItemStack costA, ItemStack costB, ItemStack result) {
    public static OfferView of(int id, MerchantOffer offer) {
        return new OfferView(id, offer.getCostA().copy(), offer.getCostB().copy(), offer.getResult().copy());
    }
    public void write(FriendlyByteBuf buffer) {
        buffer.writeVarInt(id);
        UiNetwork.writeStack(buffer, costA);
        UiNetwork.writeStack(buffer, costB);
        UiNetwork.writeStack(buffer, result);
    }
    public static OfferView read(FriendlyByteBuf buffer) {
        return new OfferView(buffer.readVarInt(), UiNetwork.readStack(buffer), UiNetwork.readStack(buffer), UiNetwork.readStack(buffer));
    }
}
