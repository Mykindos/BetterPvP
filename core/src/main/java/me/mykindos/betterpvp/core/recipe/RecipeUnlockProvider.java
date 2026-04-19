package me.mykindos.betterpvp.core.recipe;

import org.bukkit.entity.Player;

public interface RecipeUnlockProvider {

    boolean isUnlocked(Player player, String recipeKey);

    String getSource();

}
