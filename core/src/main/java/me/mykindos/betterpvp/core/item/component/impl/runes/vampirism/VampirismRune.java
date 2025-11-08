package me.mykindos.betterpvp.core.item.component.impl.runes.vampirism;

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
public class VampirismRune implements Rune, ReloadHook {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "vampirism");
    private final Provider<VampirismRuneItem> itemProvider;

    @Getter
    private double healing = 1.5;

    @Inject
    private VampirismRune(Provider<VampirismRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of Vampirism";
    }

    @Override
    public @NotNull String getDescription() {
        return String.format(
                "Restore <health>%s</health> health for every melee attack.",
                UtilFormat.formatNumber(getHealing()));
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
        this.healing = config.getConfig("healing", 1.5, Double.class);
    }
}
