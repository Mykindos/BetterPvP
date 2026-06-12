package me.mykindos.betterpvp.core.item.component.impl.socketables.runes.essence;

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
public class EssenceRune implements Socketable, Reloadable {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "essence");
    private final Provider<EssenceRuneItem> itemProvider;
    @Getter
    private double energy = 10.0;

    @Inject
    private EssenceRune(Provider<EssenceRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of Essence";
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("core.item.rune.essence.name");
    }

    @Override
    public @NotNull String getDescription() {
        return String.format("Damage dealt to enemies will restore <mana>%s</mana> energy.",
                UtilFormat.formatNumber(getEnergy()));
    }

    @Override
    public @NotNull List<Component> getDescriptionLines() {
        return Arrays.asList(Translations.componentLines("core.item.rune.essence.lore",
                RuneLore.mana(UtilFormat.formatNumber(getEnergy()))));
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
        this.energy = config.getConfig("energy", 10.0, Double.class);
    }
}
