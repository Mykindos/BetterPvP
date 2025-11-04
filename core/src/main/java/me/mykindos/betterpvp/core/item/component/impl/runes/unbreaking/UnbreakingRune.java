package me.mykindos.betterpvp.core.item.component.impl.runes.unbreaking;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.component.impl.runes.Rune;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneGroup;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

@Singleton
@EqualsAndHashCode
public class UnbreakingRune implements Rune {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "unbreaking");

    @Inject
    private UnbreakingRune() {
    }

    @Override
    public @NotNull String getDescription() {
        return "Prevents items from losing durability when used.";
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public @NotNull String getName() {
        return "Rune of Unbreaking";
    }

    @Override
    public @NotNull Collection<@NotNull RuneGroup> getGroups() {
        return Collections.singleton(RuneGroup.ALL);
    }

    @Override
    public boolean canApply(@NotNull Item item) {
        return Rune.super.canApply(item) && Rune.isDamageable(item);
    }
}
