package me.mykindos.betterpvp.core.item.component.impl.socketables.gems.diamond;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.Getter;
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
public class DiamondGem implements Socketable, Reloadable {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "diamond_gem");
    private final Provider<DiamondGemItem> itemProvider;

    @Getter
    private double cooldownReduction = 33.0;

    @Inject
    public DiamondGem(Provider<DiamondGemItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull String getName() {
        return "Flawless Diamond";
    }

    @Override
    public @NotNull String getDescription() {
        return String.format("Grants <green>%s%%</green> cooldown reduction.", UtilFormat.formatNumber(cooldownReduction));
    }

    @Override
    public @NotNull Collection<@NotNull SocketableGroup> getGroups() {
        return List.of(SocketableGroups.MELEE_WEAPON, SocketableGroups.RANGED_WEAPON);
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.cooldownReduction = config.getConfig("cooldownReduction", 33.0, Double.class);
    }
}
