package me.continent.biome;

import java.util.List;
import java.util.Set;

/**
 * Immutable snapshot of biome modifiers loaded from configuration.
 */
public record BiomeTrait(
        float baseTemp,
        float moveMult,
        float cropRate,
        float cropYieldRate,
        TradeDeform tradeDeform,
        Set<String> tags,
        List<Rule> rules
) {
    public static final BiomeTrait DEFAULT = new BiomeTrait(
            20.0f,
            1.0f,
            1.0f,
            1.0f,
            new TradeDeform(0f, 0f, 0f, 0f),
            Set.of(),
            List.of()
    );

    public record TradeDeform(float food, float metal, float leather, float gem) {
    }

    public record Rule(RuleType type, java.util.Map<String, Object> params) {
    }

    public enum RuleType {
        LOW_LIGHT_SLOW,
        LEATHER_IMMUNITY,
        ELEVATION_TRADE_BONUS
    }
}

