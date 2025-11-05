package me.mykindos.betterpvp.core.item.component.impl.runes.flameguard;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.component.impl.runes.Rune;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneGroup;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneGroups;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@Singleton
@EqualsAndHashCode
public class FlameguardRune implements Rune, ReloadHook {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "flameguard");

    private final Provider<FlameguardRuneItem> itemProvider;
    private double mitigation = 0.2;

    @Inject
    private FlameguardRune(Provider<FlameguardRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
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
        return mitigation;
    }

    @Override
    public @NotNull String getDescription() {
        return String.format("Mitigates damage taken from fire sources by <health>%s%%</health>, stacking additively.", UtilFormat.formatNumber(getMitigation() * 100));
    }

    @Override
    public @NotNull Collection<@NotNull RuneGroup> getGroups() {
        return List.of(RuneGroups.ARMOR);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of Flameguard";
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.mitigation = config.getConfig("mitigation", 0.2, Double.class);
    }
}
