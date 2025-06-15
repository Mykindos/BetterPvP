package me.mykindos.betterpvp.core.item.component.impl.runes.scorching;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.runes.Rune;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@Singleton
@EqualsAndHashCode
public class ScorchingRune implements Rune, ReloadHook {

    public static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "scorching");

    @Inject
    private ScorchingRune() {
    }

    @Override
    public @NotNull String getName() {
        return "Scorching";
    }

    @Override
    public @NotNull String getDescription() {
        return "Melee attacks have a chance to set enemies on fire for a short duration.";
    }

    @Override
    public @NotNull NamespacedKey getKey() {
        return KEY;
    }

    @Override
    public boolean canApply(@NotNull Item item) {
        return item instanceof WeaponItem || (item instanceof ItemInstance instance && canApply(instance.getBaseItem()));
    }

    @Override
    public void reload() {

    }
}
