package me.mykindos.betterpvp.core.item.component.impl.runes.scorching;

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
public class ScorchingRune implements Rune, ReloadHook {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "scorching");
    private final Provider<ScorchingRuneItem> itemProvider;
    @Getter
    private double chance = 0.30;
    @Getter
    private double duration = 2.5;

    @Inject
    private ScorchingRune(Provider<ScorchingRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of Scorching";
    }

    @Override
    public @NotNull String getDescription() {
        return String.format("Melee attacks have a <val>%s%%</val> chance to set enemies on fire for <time>%ss</time>.",
                UtilFormat.formatNumber(getChance() * 100),
                UtilFormat.formatNumber(getDuration()));
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
        this.chance = config.getConfig("fire-chance", 0.30, Double.class);
        this.duration = config.getConfig("fire-duration", 2.5, Double.class);
    }
}
