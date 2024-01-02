package me.mykindos.betterpvp.core.recipes;

import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.key.Keyed;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.Recipe;
import org.jetbrains.annotations.NotNull;

import java.util.Iterator;

@Slf4j
public class RecipeHandler {
    public void loadConfig(@NotNull ExtendedYamlConfiguration config, String namespace) {
        String path = "recipe.";
        Iterator<Recipe> recipeIterator = Bukkit.recipeIterator();
        while (recipeIterator.hasNext()) {
            Recipe recipe = recipeIterator.next();
            if (recipe instanceof Keyed keyedRecipe) {
                Key key = keyedRecipe.key();
                if (!key.namespace().equalsIgnoreCase(namespace)) continue;
                if (!config.getBoolean(path + key.namespace() + '.' + key.value(), true)) {
                    Bukkit.removeRecipe((NamespacedKey) key);
                    log.info("Disabling recipe: " + key.asString());
                }
            }
        }
    }
}
