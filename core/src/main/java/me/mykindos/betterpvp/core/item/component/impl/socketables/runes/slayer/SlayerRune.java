package me.mykindos.betterpvp.core.item.component.impl.socketables.runes.slayer;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import lombok.Getter;
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
public class SlayerRune implements Socketable, Reloadable {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "slayer");
    private final Provider<SlayerRuneItem> itemProvider;

    @Getter
    private double damage = 3.0;

    @Inject
    private SlayerRune(Provider<SlayerRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of Slayer";
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("core.item.rune.slayer.name");
    }

    @Override
    public @NotNull String getDescription() {
        return String.format(
                "Increases damage dealt to mobs by <damage>%s</damage>.",
                UtilFormat.formatNumber(getDamage()));
    }

    @Override
    public @NotNull List<Component> getDescriptionLines() {
        return Arrays.asList(Translations.componentLines("core.item.rune.slayer.lore",
                RuneLore.damage(UtilFormat.formatNumber(getDamage()))));
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull Collection <@NotNull SocketableGroup> getGroups() {
        return List.of(SocketableGroups.MELEE_WEAPON);
    }

    @Override
    public void reload() {
        final Config config = Config.item(Core.class, itemProvider.get());
        this.damage = config.getConfig("damage", 3.0, Double.class);
    }
}
