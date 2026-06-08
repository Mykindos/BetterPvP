package me.mykindos.betterpvp.core.item.component.impl.socketables.runes.recovery;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.component.impl.socketables.Socketable;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableGroup;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableGroups;
import me.mykindos.betterpvp.core.item.component.impl.socketables.runes.RuneLore;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Singleton
@EqualsAndHashCode
public class RecoveryRune implements Socketable, Reloadable {

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
    public @NotNull Component getDisplayName() {
        return Translations.component("core.item.rune.recovery.name");
    }

    @Override
    public @NotNull List<Component> getDescriptionLines() {
        return Arrays.asList(Translations.componentLines("core.item.rune.recovery.lore",
                RuneLore.health(UtilFormat.formatNumber(getIncrement()))));
    }

    @Override
    public @NotNull Collection<@NotNull SocketableGroup> getGroups() {
        return List.of(SocketableGroups.ARMOR);
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
