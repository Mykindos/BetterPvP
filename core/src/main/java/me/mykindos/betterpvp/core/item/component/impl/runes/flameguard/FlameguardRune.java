package me.mykindos.betterpvp.core.item.component.impl.runes.flameguard;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.component.impl.runes.Rune;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneGroup;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@Singleton
@EqualsAndHashCode
public class FlameguardRune implements Rune {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "flameguard");

    @Inject
    private FlameguardRune() {
    }

    @Override
    public @NotNull String getDescription() {
        return "Mitigates damage taken from fire and lava sources.";
    }

    @Override
    public @NotNull Collection<@NotNull RuneGroup> getGroups() {
        return List.of(RuneGroup.ARMOR);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull String getName() {
        return "Flameguard";
    }

    /**
     * Gets the mitigation percentage of this rune. Multiple items with
     * this rune will stack its mitigation percentage, additively.
     *
     * <p>
     *     For example, if the mitigation percentage is 10% (0.1) and a player
     *     has two items with this rune, the damage mitigated will be 20% (0.2).
     * </p>
     *
     * @return The mitigation percentage.
     */
    public double getMitigation() {
        return 0.2;
    }
}
