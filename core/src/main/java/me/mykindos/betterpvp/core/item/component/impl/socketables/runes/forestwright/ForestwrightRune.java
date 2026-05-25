package me.mykindos.betterpvp.core.item.component.impl.socketables.runes.forestwright;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.component.impl.socketables.Socketable;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableGroup;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableGroups;
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
public class ForestwrightRune implements Socketable, Reloadable {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "foreswright");

    private final Provider<ForestwrightRuneItem> itemProvider;
    private double percent;

    @Inject
    private ForestwrightRune(Provider<ForestwrightRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    public double getPercent() {
        return percent;
    }

    @Override
    public @NotNull String getDescription() {
        return String.format("Increases woodcutting experience gained by <exp>%s%%</exp>.", UtilFormat.formatNumber(getPercent() * 100));
    }

    @Override
    public @NotNull Collection<@NotNull SocketableGroup> getGroups() {
        return List.of(SocketableGroups.AXE);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of Forestwright";
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.percent = config.getConfig("percent", 0.15, Double.class);
    }
}
