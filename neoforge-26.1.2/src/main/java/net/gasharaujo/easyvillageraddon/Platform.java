package net.gasharaujo.easyvillageraddon;

import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
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
    public static String profession(EasyVillagerEntity v) { return v.getVillagerData().profession().unwrapKey().map(key -> key.identifier().toString()).orElse("none"); }
    public static boolean sameStack(ItemStack a, ItemStack b) { return ItemStack.isSameItemSameComponents(a, b); }
    static void name(ItemStack stack, String text) { stack.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME, Component.literal(text)); }
    static int villagerLevel(EasyVillagerEntity v) { return v.getVillagerData().level(); }
    static boolean unemployed(EasyVillagerEntity v) { return v.getVillagerData().profession().is(VillagerProfession.NONE); }
    static void clearProfession(EasyVillagerEntity v) {
        v.setVillagerXp(0);
        v.setVillagerData(v.getVillagerData().withProfession(BuiltInRegistries.VILLAGER_PROFESSION.get(VillagerProfession.NONE).orElseThrow()).withLevel(1));
        v.setOffers(new MerchantOffers());
    }
    static void assignProfession(EasyVillagerEntity v, Object profession) {
        v.setVillagerData(v.getVillagerData().withProfession((net.minecraft.core.Holder<VillagerProfession>) profession));

    }
    static boolean canLevelUp(EasyVillagerEntity v) {
        return villagerLevel(v) < 5 && v.getVillagerXp() >= VillagerData.getMaxXpPerLevel(villagerLevel(v));
    }
    static void levelUp(EasyVillagerEntity v) {
        v.setVillagerData(v.getVillagerData().withLevel(villagerLevel(v) + 1));
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
            entry.getKey().unwrapKey().ifPresent(key -> text.append(' ').append(key.identifier()).append(' ').append(SearchAliases.forKey(key.identifier().toString())));
        }
        return text.toString();
    }
    static List<Function<RandomSource, MerchantOffer>> tradeFactories(EasyVillagerEntity source, int targetLevel) {
        EasyVillagerEntity scratch = new EasyVillagerEntity(EntityType.VILLAGER, source.level());
        scratch.setVillagerData(source.getVillagerData().withLevel(targetLevel));
        scratch.setPos(source.position());
        List<Function<RandomSource, MerchantOffer>> factories = new ArrayList<>();
        if (!(source.level() instanceof net.minecraft.server.level.ServerLevel server)) return factories;
        var tradeKey = scratch.getVillagerData().profession().value().getTrades(targetLevel);
        if (tradeKey == null) return factories;
        var set = server.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TRADE_SET).getOptional(tradeKey);
        if (set.isEmpty()) return factories;
        var params = new net.minecraft.world.level.storage.loot.LootParams.Builder(server)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN, scratch.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY, scratch)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ADDITIONAL_COST_COMPONENT_ALLOWED, net.minecraft.util.Unit.INSTANCE)
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.VILLAGER_TRADE);
        for (var trade : set.get().getTrades()) {
            factories.add(random -> {
                var context = new net.minecraft.world.level.storage.loot.LootContext.Builder(params).withOptionalRandomSource(random).create(Optional.empty());
                return set.get().calculateNumberOfTrades(context) <= 0 ? null : trade.value().getOffer(context);
            });
        }
        return factories;
    }
}
