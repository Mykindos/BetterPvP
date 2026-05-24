package me.mykindos.betterpvp.core.item.component.impl.socketables.runes.brutality;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.component.impl.socketables.Socketable;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableGroup;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableGroups;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@Singleton
@EqualsAndHashCode
public class BrutalityRune implements Socketable, Reloadable {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "brutality");
    private final Provider<BrutalityRuneItem> itemProvider;

    @Getter
    private double damageIncrease = 1.0;

    @Inject
    private BrutalityRune(Provider<BrutalityRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of Brutality";
    }

    @Override
    public @NotNull String getDescription() {
        return String.format(
                "Increases damage dealt by <damage>%.1f</damage>.", damageIncrease
                );
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull Collection <@NotNull SocketableGroup> getGroups() {
        return List.of(SocketableGroups.MELEE_WEAPON, SocketableGroups.BOW);
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.damageIncrease = config.getConfig("damageIncrease", 1.0, Double.class);
    }
}
