package me.mykindos.betterpvp.core.item.component.impl.runes.namelesscloak;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.component.impl.runes.Rune;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneGroup;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneGroups;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;

@Singleton
@EqualsAndHashCode
public class NamelessCloakRune implements Rune {
    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "nameless_cloak");

    private final Provider<NamelessCloakRuneItem> itemProvider;

    @Inject
    public NamelessCloakRune(Provider<NamelessCloakRuneItem> itemProvider) {
        this.itemProvider = itemProvider;
    }

    @Override
    public @NotNull String getDescription() {
        return "Nametags and skins are concealed while out of combat.";
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of the Nameless Cloak";
    }

    @Override
    public @NotNull Collection<@NotNull RuneGroup> getGroups() {
        return List.of(RuneGroups.CHESTPLATE);
    }

}
