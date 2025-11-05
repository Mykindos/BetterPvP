package me.mykindos.betterpvp.core.item.component.impl.runes.stonecaller;

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
public class StonecallerRune implements Rune, ReloadHook {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "stonecaller");

    private final Provider<StonecallerRuneItem> itemProvider;
    private double percent;

    @Inject
    private StonecallerRune(Provider<StonecallerRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    public double getPercent() {
        return percent;
    }

    @Override
    public @NotNull String getDescription() {
        return String.format("Increases mining experience gained by <exp>%s%%</exp>.", UtilFormat.formatNumber(getPercent() * 100));
    }

    @Override
    public @NotNull Collection<@NotNull RuneGroup> getGroups() {
        return List.of(RuneGroups.PICKAXE);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of the Stonecaller";
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.percent = config.getConfig("percent", 0.15, Double.class);
    }
}
