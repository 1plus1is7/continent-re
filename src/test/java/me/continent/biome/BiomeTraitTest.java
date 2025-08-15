package me.continent.biome;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class BiomeTraitTest {
    @Test
    void testValues() {
        BiomeTrait trait = new BiomeTrait(10.0f, 1.2f);
        assertEquals(10.0f, trait.baseTemp());
        assertEquals(1.2f, trait.tradeDeform());
    }
}
