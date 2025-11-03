package me.mykindos.betterpvp.core.item.component.impl.runes.wanderer;

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
public class WandererRune implements Rune, ReloadHook {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "wanderer");

    private final Provider<WandererRuneItem> itemProvider;
    private double speed = 0.3;

    @Inject
    private WandererRune(Provider<WandererRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    public double getSpeed() {
        return speed;
    }

    @Override
    public @NotNull String getDescription() {
        return String.format("Increases out-of-combat movement speed by <val>%s%%</val> for exploration.", UtilFormat.formatNumber(getSpeed() * 100));
    }

    @Override
    public @NotNull Collection<@NotNull RuneGroup> getGroups() {
        return List.of(RuneGroup.BOOTS);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of the Wanderer";
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.speed = config.getConfig("speed", 0.3, Double.class);
    }
}
