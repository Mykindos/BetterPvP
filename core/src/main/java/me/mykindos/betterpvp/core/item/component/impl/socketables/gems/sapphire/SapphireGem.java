package me.mykindos.betterpvp.core.item.component.impl.socketables.gems.sapphire;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.component.impl.socketables.Socketable;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableGroup;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableGroups;
import me.mykindos.betterpvp.core.item.component.impl.stat.ItemStat;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatTypes;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@Singleton
public class SapphireGem implements Socketable, Reloadable {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "sapphire_gem");
    private final Provider<SapphireGemItem> itemProvider;

    @Getter
    private double energyModifier = 25.0;

    @Inject
    public SapphireGem(Provider<SapphireGemItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull String getName() {
        return "Flawless Sapphire";
    }

    @Override
    public @NotNull String getDescription() {
        return String.format("Increases energy pool by <green>%s</green>.", UtilFormat.formatNumber(energyModifier));
    }

    @Override
    public @NotNull Collection<@NotNull SocketableGroup> getGroups() {
        return List.of(SocketableGroups.ARMOR);
    }

    @Override
    public @NotNull Collection<ItemStat<?>> getStats() {
        return List.of(new ItemStat<>(StatTypes.ENERGY, energyModifier));
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.energyModifier = config.getConfig("energyModifier", 25.0, Double.class);
    }
}
