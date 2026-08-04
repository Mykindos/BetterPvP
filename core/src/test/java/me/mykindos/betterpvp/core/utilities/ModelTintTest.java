package me.mykindos.betterpvp.core.utilities;

import org.bukkit.Color;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("ModelTint")
class ModelTintTest {

    @Test
    @DisplayName("a bare hex colour tints the whole model")
    void bareHex() {
        final ModelTint tint = ModelTint.parse("ff8800").orElseThrow();
        assertEquals(Color.fromRGB(0xFF8800), tint.getOverall());
        assertTrue(tint.getByBone().isEmpty());
    }

    @Test
    @DisplayName("a leading # is optional")
    void hashIsOptional() {
        assertEquals(ModelTint.parse("ff8800"), ModelTint.parse("#ff8800"));
    }

    @Test
    @DisplayName("vanilla colour names resolve")
    void colourName() {
        assertEquals(Color.fromRGB(0x000000), ModelTint.parse("black").orElseThrow().getOverall());
        assertEquals(Color.fromRGB(0xFF5555), ModelTint.parse("red").orElseThrow().getOverall());
        assertEquals(Color.fromRGB(0xFF55FF), ModelTint.parse("light_purple").orElseThrow().getOverall());
    }

    @Test
    @DisplayName("black is a tint like any other, not an absent one")
    void blackIsATint() {
        final ModelTint tint = ModelTint.parse("000000").orElseThrow();
        assertEquals(Color.fromRGB(0x000000), tint.getOverall());
    }

    @Test
    @DisplayName("bone=colour entries tint only that bone")
    void perBone() {
        final ModelTint tint = ModelTint.parse("flame=ffdd66,frame=black").orElseThrow();
        assertNull(tint.getOverall());
        assertEquals(Color.fromRGB(0xFFDD66), tint.getByBone().get("flame"));
        assertEquals(Color.fromRGB(0x000000), tint.getByBone().get("frame"));
    }

    @Test
    @DisplayName("a bare colour and per-bone overrides combine")
    void overallPlusPerBone() {
        final ModelTint tint = ModelTint.parse("66aa88, flame = ffdd66").orElseThrow();
        assertEquals(Color.fromRGB(0x66AA88), tint.getOverall());
        assertEquals(Color.fromRGB(0xFFDD66), tint.getByBone().get("flame"));
    }

    @Test
    @DisplayName("bone names are matched case-insensitively")
    void boneNamesLowerCased() {
        assertEquals(Color.fromRGB(0x000000), ModelTint.parse("Flame=black").orElseThrow().getByBone().get("flame"));
    }

    @Test
    @DisplayName("blank and malformed specs parse to nothing rather than to a default colour")
    void rejectsGarbage() {
        assertEquals(Optional.empty(), ModelTint.parse(""));
        assertEquals(Optional.empty(), ModelTint.parse("   "));
        assertEquals(Optional.empty(), ModelTint.parse("puce"));
        assertEquals(Optional.empty(), ModelTint.parse("ff88"));
        assertEquals(Optional.empty(), ModelTint.parse("gggggg"));
        assertEquals(Optional.empty(), ModelTint.parse("flame="));
        assertEquals(Optional.empty(), ModelTint.parse("ff8800,flame=nonsense"));
    }
}
