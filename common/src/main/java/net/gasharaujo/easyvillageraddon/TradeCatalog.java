package net.gasharaujo.easyvillageraddon;

import de.maxhenkel.easyvillagers.entity.EasyVillagerEntity;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.trading.MerchantOffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/** Runs in small batches on the server thread so pack callbacks stay on that thread. */
final class TradeCatalog {
    private final List<Function<RandomSource, MerchantOffer>> factories;
    final List<MerchantOffer> offers = new ArrayList<>();
    private int factoryIndex;
    private int variant;
    private int neededVariants = 1;
    private static final int VARIANTS = 4096;
    private static final int MAX_ENTRIES = 4096;

    TradeCatalog(EasyVillagerEntity villager, int targetLevel) {
        factories = Platform.tradeFactories(villager, targetLevel);
    }

    boolean complete() { return factoryIndex >= factories.size(); }

    void step() {
        for (int budget = 0; budget < 128 && !complete(); budget++) {
            CatalogRandom random = new CatalogRandom(variant);
            MerchantOffer offer = factories.get(factoryIndex).apply(random);
            neededVariants = Math.max(neededVariants, random.combinations());
            if (offer != null && !offer.getResult().isEmpty() && offers.size() < MAX_ENTRIES
                    && offers.stream().noneMatch(existing -> sameTrade(existing, offer))) {
                offers.add(offer);
            }
            if (++variant >= Math.min(VARIANTS, neededVariants)) {
                variant = 0;
                neededVariants = 1;
                factoryIndex++;
            }
        }
    }

    static boolean sameTrade(MerchantOffer a, MerchantOffer b) {
        return Platform.sameStack(a.getResult(), b.getResult())
                && Platform.sameStack(a.getBaseCostA(), b.getBaseCostA())
                && Platform.sameStack(a.getCostB(), b.getCostB());
    }
}
