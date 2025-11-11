package me.mykindos.betterpvp.core.item.component.impl.runes.slayer;

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
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@Singleton
@EqualsAndHashCode
public class SlayerRune implements Rune, Reloadable {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "slayer");
    private final Provider<SlayerRuneItem> itemProvider;

    @Getter
    private double damage = 3.0;

    @Inject
    private SlayerRune(Provider<SlayerRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of Slayer";
    }

    @Override
    public @NotNull String getDescription() {
        return String.format(
                "Increases damage dealt to mobs by <damage>%s</damage>.",
                UtilFormat.formatNumber(getDamage()));
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
        this.damage = config.getConfig("damage", 3.0, Double.class);
    }
}
