package me.mykindos.betterpvp.core.item.component.impl.runes.attraction;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.component.impl.runes.Rune;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneGroup;
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
public class AttractionRune implements Rune, ReloadHook {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "attraction");

    private final Provider<AttractionRuneItem> itemProvider;
    private double speed = 4.0;
    private double range = 5.0;

    @Inject
    private AttractionRune(Provider<AttractionRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    public double getSpeed() {
        return speed;
    }

    public double getRange() {
        return range;
    }

    @Override
    public @NotNull String getDescription() {
        return String.format("Pulls nearby dropped items within <val>%s</val> blocks toward the wearer. Pull speed stacks additively.", UtilFormat.formatNumber(getRange()));
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
        return "Attraction";
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.speed = config.getConfig("speed", 4.0, Double.class);
        this.range = config.getConfig("range", 5.0, Double.class);
    }
}
