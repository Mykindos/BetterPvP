package me.mykindos.betterpvp.core.item.component.impl.runes.momentum;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import lombok.Getter;
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
public class MomentumRune implements Rune, ReloadHook {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "momentum");
    private final Provider<MomentumRuneItem> itemProvider;

    @Getter
    private double scalar = 0.15;

    @Getter
    private double expirySeconds = 1.0;

    @Inject
    private MomentumRune(Provider<MomentumRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of Momentum";
    }

    @Override
    public @NotNull String getDescription() {
        return String.format(
                "Melee attacks reduce the next attack's delay on that target by <damage>%s%%</damage>, stacking additively. Resets after not attacking for <time>%s</time> seconds.",
                UtilFormat.formatNumber(getScalar() * 100),
                UtilFormat.formatNumber(getExpirySeconds()));
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull Collection <@NotNull RuneGroup> getGroups() {
        return List.of(RuneGroups.MELEE_WEAPON);
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.scalar = config.getConfig("scalar", 0.15, Double.class);
        this.expirySeconds = config.getConfig("expirySeconds", 1.0, Double.class);
    }
}
