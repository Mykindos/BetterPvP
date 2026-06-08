package me.mykindos.betterpvp.core.item.component.impl.socketables.runes.attraction;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.component.impl.socketables.Socketable;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableGroup;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableGroups;
import me.mykindos.betterpvp.core.item.component.impl.socketables.runes.RuneLore;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Singleton
@EqualsAndHashCode
public class AttractionRune implements Socketable, Reloadable {

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
    public @NotNull Component getDisplayName() {
        return Translations.component("core.item.rune.attraction.name");
    }

    @Override
    public @NotNull List<Component> getDescriptionLines() {
        return Arrays.asList(Translations.componentLines("core.item.rune.attraction.lore",
                RuneLore.val(UtilFormat.formatNumber(getRange()))));
    }

    @Override
    public @NotNull Collection<@NotNull SocketableGroup> getGroups() {
        return List.of(SocketableGroups.ARMOR);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of Attraction";
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.speed = config.getConfig("speed", 4.0, Double.class);
        this.range = config.getConfig("range", 5.0, Double.class);
    }
}
