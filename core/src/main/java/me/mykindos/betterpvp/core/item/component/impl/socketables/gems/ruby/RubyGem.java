package me.mykindos.betterpvp.core.item.component.impl.socketables.gems.ruby;

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
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@Singleton
public class RubyGem implements Socketable, Reloadable {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "flawless_ruby");
    private final Provider<RubyGemItem> itemProvider;

    @Getter
    private double healthModifier = 5.0;

    @Inject
    public RubyGem(Provider<RubyGemItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull String getName() {
        return "Flawless Ruby";
    }

    @Override
    public @NotNull String getDescription() {
        return String.format("Increases Health by <green>%.0f</green>.", healthModifier);
    }

    @Override
    public @NotNull Collection<@NotNull SocketableGroup> getGroups() {
        return List.of(SocketableGroups.ARMOR);
    }

    @Override
    public @NotNull Collection<ItemStat<?>> getStats() {
        return List.of(new ItemStat<>(StatTypes.HEALTH, healthModifier));
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.healthModifier = config.getConfig("healthModifier", 5.0, Double.class);
    }
}
