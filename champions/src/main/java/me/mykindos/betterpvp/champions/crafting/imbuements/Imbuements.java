package me.mykindos.betterpvp.champions.crafting.imbuements;

import lombok.Getter;
import org.bukkit.NamespacedKey;

@Getter
public enum Imbuements {

    SKILL_HASTE(new NamespacedKey("champions", "imbuement-haste")),
    VIGOR(new NamespacedKey("champions", "imbuement-vigor")),
    FORTIFICATION(new NamespacedKey("champions", "imbuement-fortification")),
    INSIGHT(new NamespacedKey("champions", "imbuement-insight")),
    FORTUNE(new NamespacedKey("champions", "imbuement-fortune"));

    private final NamespacedKey key;

    Imbuements(NamespacedKey key) {
        this.key = key;
    }
}
