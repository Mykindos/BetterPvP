package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.deathwish;

import lombok.Getter;
import org.bukkit.Color;

public enum DeathwishThreshold {
    NONE(0, Color.GRAY),
    DAMAGE(1, Color.YELLOW),
    ATTACK_SPEED(2, Color.RED);

    DeathwishThreshold(int level, Color color) {
        this.level = level;
        this.color = color;
    }

    @Getter
    private final int level;

    @Getter
    private final Color color;
}
