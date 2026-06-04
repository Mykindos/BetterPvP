package me.mykindos.betterpvp.clans.world.resource;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DegradeChainTest {

    private static DegradeChain copper() {
        return DegradeChain.ofMaterials(List.of("copper_ore", "stone", "cobblestone", "air"));
    }

    @Test
    @DisplayName("next steps through the chain")
    void nextSteps() {
        final DegradeChain chain = copper();
        assertEquals("stone", chain.next("copper_ore").orElse(null));
        assertEquals("cobblestone", chain.next("stone").orElse(null));
        assertEquals("air", chain.next("cobblestone").orElse(null));
    }

    @Test
    @DisplayName("next is empty for terminal and unknown stages")
    void nextTerminalAndUnknown() {
        final DegradeChain chain = copper();
        assertTrue(chain.next("air").isEmpty());
        assertTrue(chain.next("diamond_block").isEmpty());
    }

    @Test
    @DisplayName("material names normalise (namespace stripped, case-insensitive)")
    void normalises() {
        final DegradeChain chain = copper();
        assertEquals("stone", chain.next("minecraft:COPPER_ORE").orElse(null));
        assertEquals("cobblestone", chain.next("STONE").orElse(null));
    }

    @Test
    @DisplayName("isTerminal and first behave")
    void terminalAndFirst() {
        final DegradeChain chain = copper();
        assertFalse(chain.isTerminal("copper_ore"));
        assertTrue(chain.isTerminal("air"));
        assertTrue(chain.isTerminal("not_in_chain"));
        assertEquals("copper_ore", chain.first());
    }

    @Test
    @DisplayName("an empty chain is rejected")
    void emptyRejected() {
        assertThrows(IllegalArgumentException.class, () -> DegradeChain.ofMaterials(List.of()));
    }

    @Test
    @DisplayName("per-stage loot and unbreakable are read from the matching stage")
    void perStageProperties() {
        final DegradeChain chain = DegradeChain.of(List.of(
                new DegradeChain.Stage("copper_ore", "copper_mine", false),
                new DegradeChain.Stage("stone", null, false),
                new DegradeChain.Stage("deepslate", null, true)));

        assertEquals("copper_mine", chain.stageOf("copper_ore").orElseThrow().lootTable());
        assertFalse(chain.stageOf("copper_ore").orElseThrow().unbreakable());
        assertNull(chain.stageOf("stone").orElseThrow().lootTable());
        assertTrue(chain.stageOf("minecraft:DEEPSLATE").orElseThrow().unbreakable());
        assertTrue(chain.stageOf("not_in_chain").isEmpty());
    }
}
