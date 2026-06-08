package me.mykindos.betterpvp.core.item.component.impl.socketables.runes.momentum;

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
public class MomentumRune implements Socketable, Reloadable {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "momentum");
    private final Provider<MomentumRuneItem> itemProvider;

    @Getter
    private double scalar = 0.15;

    @Getter
    private double maximumReduction = 0.75;

    @Getter
    private double expirySeconds = 1.0;

    @Inject
    private MomentumRune(Provider<MomentumRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of Momentum";
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("core.item.rune.momentum.name");
    }

    @Override
    public @NotNull List<Component> getDescriptionLines() {
        return Arrays.asList(Translations.componentLines("core.item.rune.momentum.lore",
                RuneLore.damage(UtilFormat.formatNumber(getScalar() * 100) + "%"),
                RuneLore.damage(UtilFormat.formatNumber(getMaximumReduction() * 100) + "%"),
                RuneLore.time(UtilFormat.formatNumber(getExpirySeconds()))));
    }

    @Override
    public @NotNull String getDescription() {
        return String.format(
                "Melee attacks reduce the next attack's delay on that target by <damage>%s%%</damage>, stacking additively, up to a maximum of <damage>%s%%</damage>. Resets after not attacking for <time>%s</time> seconds.",
                UtilFormat.formatNumber(getScalar() * 100),
                UtilFormat.formatNumber(getMaximumReduction() * 100),
                UtilFormat.formatNumber(getExpirySeconds()));
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
        this.scalar = config.getConfig("scalar", 0.15, Double.class);
        this.maximumReduction = config.getConfig("maximumReduction", 0.75, Double.class);
        this.expirySeconds = config.getConfig("expirySeconds", 1.0, Double.class);
    }
}
