package me.mykindos.betterpvp.core.item.component.impl.socketables.runes.wanderer;

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
public class WandererRune implements Socketable, Reloadable {

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
    public @NotNull Component getDisplayName() {
        return Translations.component("core.item.rune.wanderer.name");
    }

    @Override
    public @NotNull List<Component> getDescriptionLines() {
        return Arrays.asList(Translations.componentLines("core.item.rune.wanderer.lore",
                RuneLore.val(UtilFormat.formatNumber(getSpeed() * 100) + "%")));
    }

    @Override
    public @NotNull Collection<@NotNull SocketableGroup> getGroups() {
        return List.of(SocketableGroups.BOOTS);
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
