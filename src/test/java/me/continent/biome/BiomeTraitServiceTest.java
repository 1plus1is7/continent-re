package me.continent.biome;

import org.bukkit.block.Biome;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BiomeTraitServiceTest {
    @Test
    void nullBiomeReturnsDefault() {
        BiomeTrait trait = BiomeTraitService.get((Biome) null);
        assertSame(BiomeTrait.DEFAULT, trait);
    }
}
