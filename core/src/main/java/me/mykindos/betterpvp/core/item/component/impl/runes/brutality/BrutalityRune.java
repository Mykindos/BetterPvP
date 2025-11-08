package me.mykindos.betterpvp.core.item.component.impl.runes.brutality;

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
public class BrutalityRune implements Rune, ReloadHook {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "brutality");
    private final Provider<BrutalityRuneItem> itemProvider;

    @Getter
    private double scalar = 0.3;

    @Getter
    private double chance = 0.2;

    @Inject
    private BrutalityRune(Provider<BrutalityRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of Brutality";
    }

    @Override
    public @NotNull String getDescription() {
        return String.format(
                "Damage dealt has a <val>%s%%</val> chance to be increased by <damage>%s%%</damage>.",
                UtilFormat.formatNumber(getChance() * 100),
                UtilFormat.formatNumber(getScalar() * 100));
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull Collection <@NotNull RuneGroup> getGroups() {
        return List.of(RuneGroups.MELEE_WEAPON, RuneGroups.BOW);
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.chance = config.getConfig("chance", 0.2, Double.class);
        this.scalar = config.getConfig("scalar", 0.3, Double.class);
    }
}
