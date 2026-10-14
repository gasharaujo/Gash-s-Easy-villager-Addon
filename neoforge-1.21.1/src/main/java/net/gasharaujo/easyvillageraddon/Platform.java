package net.gasharaujo.easyvillageraddon;

import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.util.RandomSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class Platform {
    public static String profession(EasyVillagerEntity v) { return BuiltInRegistries.VILLAGER_PROFESSION.getKey(v.getVillagerData().getProfession()).toString(); }
    public static boolean sameStack(ItemStack a, ItemStack b) { return ItemStack.isSameItemSameComponents(a, b); }
    static void name(ItemStack stack, String text) { stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(text)); }
    static int villagerLevel(EasyVillagerEntity v) { return v.getVillagerData().getLevel(); }
    static boolean unemployed(EasyVillagerEntity v) { return v.getVillagerData().getProfession() == VillagerProfession.NONE; }
    static void clearProfession(EasyVillagerEntity v) {
        v.setVillagerXp(0);
        v.setVillagerData(v.getVillagerData().setProfession(VillagerProfession.NONE).setLevel(1));
        v.setOffers(new MerchantOffers());
    }
    static void assignProfession(EasyVillagerEntity v, Object profession) {
        v.setVillagerData(v.getVillagerData().setProfession((VillagerProfession) profession));

    }
    static boolean canLevelUp(EasyVillagerEntity v) {
        return villagerLevel(v) < 5 && v.getVillagerXp() >= VillagerData.getMaxXpPerLevel(villagerLevel(v));
    }
    static void levelUp(EasyVillagerEntity v) {
        v.setVillagerData(v.getVillagerData().setLevel(villagerLevel(v) + 1));
        EasyVillagerEntity scratch = new EasyVillagerEntity(EntityType.VILLAGER, v.level());
        scratch.setVillagerData(v.getVillagerData());
        scratch.setPos(v.position());
        v.getOffers().addAll(scratch.getOffers());
    }
    static MerchantOffer unlimited(MerchantOffer offer, EasyVillagerEntity villager) {
        var ops = villager.registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var tag = (net.minecraft.nbt.CompoundTag) MerchantOffer.CODEC.encodeStart(ops, offer).getOrThrow();
        tag.putInt("maxUses", Integer.MAX_VALUE); tag.putInt("uses", 0);
        return MerchantOffer.CODEC.parse(ops, tag).getOrThrow();
    }
    public static List<String> enchantmentLabels(ItemStack stack) { return EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet().stream().map(entry -> net.minecraft.world.item.enchantment.Enchantment.getFullname(entry.getKey(), entry.getIntValue()).getString()).toList(); }
    public static String searchText(ItemStack stack) {
        if (stack.isEmpty()) return "";
        StringBuilder text = new StringBuilder(stack.getHoverName().getString()).append(' ').append(BuiltInRegistries.ITEM.getKey(stack.getItem()));
        for (var entry : EnchantmentHelper.getEnchantmentsForCrafting(stack).entrySet()) {
            text.append(' ').append(net.minecraft.world.item.enchantment.Enchantment.getFullname(entry.getKey(), entry.getIntValue()).getString());
            entry.getKey().unwrapKey().ifPresent(key -> text.append(' ').append(key.location()).append(' ').append(SearchAliases.forKey(key.location().toString())));
        }
        return text.toString();
    }
    static List<Function<RandomSource, MerchantOffer>> tradeFactories(EasyVillagerEntity source, int targetLevel) {
        EasyVillagerEntity scratch = new EasyVillagerEntity(EntityType.VILLAGER, source.level());
        scratch.setVillagerData(source.getVillagerData().setLevel(targetLevel));
        scratch.setPos(source.position());
        List<Function<RandomSource, MerchantOffer>> factories = new ArrayList<>();
        var profession = scratch.getVillagerData().getProfession();
        var map = net.minecraft.world.entity.npc.VillagerTrades.TRADES.get(profession);
        if (source.level().enabledFeatures().contains(net.minecraft.world.flag.FeatureFlags.TRADE_REBALANCE)) {
            var experimental = net.minecraft.world.entity.npc.VillagerTrades.EXPERIMENTAL_TRADES.get(profession);
            if (experimental != null) map = experimental;
        }
        if (map != null && map.get(targetLevel) != null) {
            for (var listing : map.get(targetLevel)) factories.add(random -> listing.getOffer(scratch, random));
        }
        return factories;
    }
}
