package me.mykindos.betterpvp.core.recipe;

import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;

public interface RecipeUnlockProvider {

    boolean isUnlocked(Player player, Key recipeKey);

    String getSource();

}
