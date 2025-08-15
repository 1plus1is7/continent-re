package me.continent.biome;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class BiomeTraitTest {
    @Test
    void testValues() {
        BiomeTrait.TradeDeform deform = new BiomeTrait.TradeDeform(0.1f, -0.2f, 0f, 0.3f);
        BiomeTrait trait = new BiomeTrait(10.0f, 1.1f, 1.2f, 1.3f, deform, Set.of("temperate"), List.of());
        assertEquals(10.0f, trait.baseTemp());
        assertEquals(1.1f, trait.moveMult());
        assertEquals(1.2f, trait.cropRate());
        assertEquals(0.1f, trait.tradeDeform().food());
        assertTrue(trait.tags().contains("temperate"));
    }
}
