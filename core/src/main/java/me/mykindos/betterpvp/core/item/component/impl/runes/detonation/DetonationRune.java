package me.mykindos.betterpvp.core.item.component.impl.runes.detonation;

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
public class DetonationRune implements Rune, ReloadHook {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "detonation");

    private final Provider<DetonationRuneItem> itemProvider;

    @Getter
    private double damage = 5.0;

    @Getter
    private double radius = 3.0;

    @Inject
    private DetonationRune(Provider<DetonationRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull String getDescription() {
        return String.format(
                "Slain enemies explode, dealing <damage>%s</damage> damage to others within <val>%s</val> blocks.",
                UtilFormat.formatNumber(damage),
                UtilFormat.formatNumber(radius));
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
        return "Rune of Detonation";
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.damage = config.getConfig("damage", 5.0, Double.class);
        this.radius = config.getConfig("radius", 3.0, Double.class);
    }
}
