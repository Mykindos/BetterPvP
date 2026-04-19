package me.mykindos.betterpvp.core.recipe;

import com.google.inject.Singleton;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Singleton
public class RecipeUnlockService {

    private final List<RecipeUnlockProvider> providers = new CopyOnWriteArrayList<>();

    public void register(RecipeUnlockProvider provider) {
        providers.add(provider);
    }

    public void unregister(RecipeUnlockProvider provider) {
        providers.remove(provider);
    }

    public boolean isUnlocked(Player player, Key recipeKey) {
        return providers.stream().anyMatch(p -> p.isUnlocked(player, recipeKey));
    }

    /**
     * Returns the source identifiers of every provider that currently grants this player access.
     * Useful for debugging and skill-tree UI tooltips.
     */
    public List<String> getUnlockingSources(Player player, Key recipeKey) {
        return providers.stream()
                .filter(p -> p.isUnlocked(player, recipeKey))
                .map(RecipeUnlockProvider::getSource)
                .toList();
    }

}
