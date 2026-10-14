package net.gasharaujo.easyvillageraddon;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.MerchantOffer;
import java.util.List;

/** A result item wildcard or an exact offer, at its own unlock level. */
public record TradeTarget(int level, boolean generic, ItemStack result, ItemStack costA, ItemStack costB) {
    public static final int MAX_TARGETS = 32;
    public static final Codec<TradeTarget> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.intRange(1, 5).fieldOf("Level").forGetter(TradeTarget::level),
            Codec.BOOL.fieldOf("Generic").forGetter(TradeTarget::generic),
            ItemStack.CODEC.fieldOf("Result").forGetter(TradeTarget::result),
            ItemStack.CODEC.optionalFieldOf("CostA", ItemStack.EMPTY).forGetter(TradeTarget::costA),
            ItemStack.CODEC.optionalFieldOf("CostB", ItemStack.EMPTY).forGetter(TradeTarget::costB)
    ).apply(i, TradeTarget::new));
    public static final Codec<List<TradeTarget>> LIST_CODEC = CODEC.listOf();

    public static TradeTarget of(int level, boolean generic, MerchantOffer offer) {
        return new TradeTarget(level, generic, generic ? new ItemStack(offer.getResult().getItem()) : offer.getResult().copy(),
                generic ? ItemStack.EMPTY : offer.getBaseCostA().copy(), generic ? ItemStack.EMPTY : offer.getCostB().copy());
    }
    public boolean matches(MerchantOffer offer) {
        if (result.isEmpty()) return false;
        if (generic) return offer.getResult().is(result.getItem());
        return Platform.sameStack(result, offer.getResult()) && Platform.sameStack(costA, offer.getBaseCostA()) && Platform.sameStack(costB, offer.getCostB());
    }
    public boolean sameDefinition(TradeTarget other) {
        return level == other.level && generic == other.generic && Platform.sameStack(result, other.result)
                && Platform.sameStack(costA, other.costA) && Platform.sameStack(costB, other.costB);
    }
    public void write(FriendlyByteBuf b) {
        b.writeVarInt(level); b.writeBoolean(generic); UiNetwork.writeStack(b, result); UiNetwork.writeStack(b, costA); UiNetwork.writeStack(b, costB);
    }
    public static TradeTarget read(FriendlyByteBuf b) {
        int level = b.readVarInt(); boolean generic = b.readBoolean();
        if (level < 1 || level > 5) throw new IllegalArgumentException("Invalid target level");
        return new TradeTarget(level, generic, UiNetwork.readStack(b), UiNetwork.readStack(b), UiNetwork.readStack(b));
    }
}
