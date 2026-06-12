package me.mykindos.betterpvp.core.item.component.impl.socketables.runes.moonseer;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.component.impl.socketables.Socketable;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableGroup;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableGroups;
import me.mykindos.betterpvp.core.locale.Translations;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Singleton
@EqualsAndHashCode
public class MoonseerRune implements Socketable {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "moonseer");

    private final Provider<MoonseerRuneItem> itemProvider;

    @Inject
    private MoonseerRune(Provider<MoonseerRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull String getDescription() {
        return "Grants clear vision in darkness while equipped.";
    }

    @Override
    public @NotNull Component getDisplayName() {
        return Translations.component("core.item.rune.moonseer.name");
    }

    @Override
    public @NotNull List<Component> getDescriptionLines() {
        return Arrays.asList(Translations.componentLines("core.item.rune.moonseer.lore"));
    }

    @Override
    public @NotNull Collection<@NotNull SocketableGroup> getGroups() {
        return List.of(SocketableGroups.HELMET);
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of Moonseer";
    }

}
