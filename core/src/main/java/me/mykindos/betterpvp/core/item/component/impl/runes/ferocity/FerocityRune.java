package me.mykindos.betterpvp.core.item.component.impl.runes.ferocity;

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
public class FerocityRune implements Rune, ReloadHook {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "ferocity");

    private final Provider<FerocityRuneItem> itemProvider;

    @Getter
    private double chance = 0.3;

    @Getter
    private double delayReduction = 0.6;

    @Inject
    private FerocityRune(Provider<FerocityRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull String getDescription() {
        return String.format(
                "Melee attacks have a <val>%s%%</val> chance to reduce the next attack's delay by <val>%s%%</val>.",
                UtilFormat.formatNumber(chance * 100),
                UtilFormat.formatNumber(delayReduction * 100)
        );
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
        return "Rune of Ferocity";
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.chance = config.getConfig("chance", 0.3, Double.class);
        this.delayReduction = config.getConfig("delayReduction", 0.6, Double.class);
    }
}
