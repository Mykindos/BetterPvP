package me.mykindos.betterpvp.core.item.component.impl.runes.pickpocket;

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
public class GreedRune implements Rune, ReloadHook {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "pickpocket");

    private final Provider<GreedRuneItem> itemProvider;
    private double percentageBonus = 0.10;

    @Inject
    private GreedRune(Provider<GreedRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    public double getPercentageBonus() {
        return percentageBonus;
    }

    @Override
    public @NotNull String getDescription() {
        return String.format("Slain enemies drop <coins>%s%%</coins> more coins on death.", UtilFormat.formatNumber(percentageBonus * 100));
    }

    @Override
    public @NotNull Collection<@NotNull RuneGroup> getGroups() {
        return List.of(RuneGroups.MELEE_WEAPON);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of Greed";
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.percentageBonus = config.getConfig("percentageBonus", 0.10, Double.class);
    }
}
