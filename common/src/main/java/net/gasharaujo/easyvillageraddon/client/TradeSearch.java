package net.gasharaujo.easyvillageraddon.client;

import net.gasharaujo.easyvillageraddon.OfferView;
import net.gasharaujo.easyvillageraddon.Platform;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

public final class TradeSearch {
    public record Entry(OfferView offer, String searchable, List<String> completions) { }
    public static String normalize(String text) {
        return Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT).trim();
    }
    public static Entry index(OfferView offer) {
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        suggestions.add(offer.result().getHoverName().getString());
        suggestions.addAll(Platform.enchantmentLabels(offer.result()));
        if (!offer.costA().isEmpty()) suggestions.add(offer.costA().getHoverName().getString());
        if (!offer.costB().isEmpty()) suggestions.add(offer.costB().getHoverName().getString());
        String searchable = normalize(Platform.searchText(offer.result()) + " " + Platform.searchText(offer.costA()) + " " + Platform.searchText(offer.costB()));
        return new Entry(offer, searchable, List.copyOf(suggestions));
    }
    public static boolean matches(Entry entry, String query) {
        for (String token : normalize(query).split("\\s+")) if (!entry.searchable.contains(token)) return false;
        return true;
    }
    public static List<String> suggest(List<Entry> entries, String query) {
        String prefix = normalize(query);
        if (prefix.isBlank()) return List.of();
        LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        for (Entry entry : entries) for (String completion : entry.completions) {
            if (normalize(completion).startsWith(prefix) && !normalize(completion).equals(prefix)) suggestions.add(completion);
        }
        if (suggestions.size() < 4) for (Entry entry : entries) for (String completion : entry.completions) {
            if (normalize(completion).contains(prefix) && !normalize(completion).equals(prefix)) suggestions.add(completion);
        }
        return suggestions.stream().limit(4).toList();
    }
}
