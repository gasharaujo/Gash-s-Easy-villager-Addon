package net.gasharaujo.easyvillageraddon;

import java.util.Map;

/** Also accepts common Portuguese names when the dedicated server uses English. */
final class SearchAliases {
    private static final Map<String, String> NAMES = Map.ofEntries(
        Map.entry("mending", "remendo reparacao reparação"),
        Map.entry("unbreaking", "inquebravel inquebrável durabilidade"),
        Map.entry("efficiency", "eficiencia eficiência"),
        Map.entry("fortune", "fortuna"), Map.entry("silk_touch", "toque suave toque de seda"),
        Map.entry("sharpness", "afiacao afiação"), Map.entry("smite", "julgamento"),
        Map.entry("bane_of_arthropods", "ruina ruína dos artropodes artrópodes"),
        Map.entry("looting", "pilhagem saque"), Map.entry("fire_aspect", "aspecto flamejante"),
        Map.entry("knockback", "repulsao repulsão"), Map.entry("sweeping_edge", "alcance"),
        Map.entry("protection", "protecao proteção"), Map.entry("fire_protection", "protecao proteção contra fogo"),
        Map.entry("blast_protection", "protecao proteção contra explosoes explosões"),
        Map.entry("projectile_protection", "protecao proteção contra projeteis projéteis"),
        Map.entry("feather_falling", "peso pena queda suave"), Map.entry("respiration", "respiracao respiração"),
        Map.entry("aqua_affinity", "afinidade aquatica aquática"), Map.entry("thorns", "espinhos"),
        Map.entry("depth_strider", "passos profundos afinidade aquatica aquática"),
        Map.entry("frost_walker", "passos gelados"), Map.entry("soul_speed", "velocidade das almas"),
        Map.entry("swift_sneak", "furtividade veloz"), Map.entry("power", "forca força poder"),
        Map.entry("punch", "impacto"), Map.entry("flame", "chama"), Map.entry("infinity", "infinidade"),
        Map.entry("luck_of_the_sea", "sorte no mar"), Map.entry("lure", "isca"),
        Map.entry("loyalty", "lealdade"), Map.entry("impaling", "penetracao penetração empalamento"),
        Map.entry("riptide", "correnteza"), Map.entry("channeling", "canalizacao canalização"),
        Map.entry("multishot", "tiro multiplo múltiplo rajada"), Map.entry("quick_charge", "carga rapida rápida"),
        Map.entry("piercing", "perfuracao perfuração"), Map.entry("binding_curse", "maldicao maldição da ligacao ligação"),
        Map.entry("vanishing_curse", "maldicao maldição do desaparecimento"),
        Map.entry("density", "densidade"), Map.entry("breach", "ruptura brecha"),
        Map.entry("wind_burst", "rajada de vento explosao explosão eolica eólica")
    );
    static String forKey(String key) { return NAMES.getOrDefault(key.substring(key.indexOf(':') + 1), ""); }
}
