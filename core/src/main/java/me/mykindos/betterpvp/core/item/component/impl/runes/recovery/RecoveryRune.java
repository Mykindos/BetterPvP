package me.mykindos.betterpvp.core.item.component.impl.runes.recovery;

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
public class RecoveryRune implements Rune, ReloadHook {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "recovery");

    private final Provider<RecoveryRuneItem> itemProvider;
    private double increment = 0.3;

    @Inject
    private RecoveryRune(Provider<RecoveryRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    public double getIncrement() {
        return increment;
    }

    @Override
    public @NotNull String getDescription() {
        return String.format("Increases natural health regeneration by <health>%s</health>, stacking additively.", UtilFormat.formatNumber(getIncrement()));
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
        return "Rune of Recovery";
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.increment = config.getConfig("increment", 0.3, Double.class);
    }
}
