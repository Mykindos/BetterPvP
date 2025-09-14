package me.mykindos.betterpvp.core.recipe;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.recipe.resolver.RecipeResolver;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
@CustomLog
public class RecipeRegistries implements RecipeRegistry<Recipe<?, ?>> {

    private final Map<NamespacedKey, RecipeRegistry<?>> registries = new HashMap<>();
    private final RecipeResolver<Recipe<?, ?>> resolver = new RecipeResolver<>(this);

    public void register(NamespacedKey key, RecipeRegistry<?> registry) {
        registries.put(key, registry);
        log.info("Registered recipe registry: {}", registry.getClass().getSimpleName()).submit();
    }

    public Set<RecipeRegistry<?>> getChildren() {
        return Set.copyOf(registries.values());
    }

    @Override
    public void registerRecipe(NamespacedKey key, Recipe<?, ?> recipe) {
        throw new UnsupportedOperationException("Cannot register recipes to this registry");
    }

    @Override
    public Collection<Recipe<?, ?>> getRecipes() {
        List<Recipe<?, ?>> recipes = new ArrayList<>();
        for (RecipeRegistry<?> registry : registries.values()) {
            recipes.addAll(registry.getRecipes());
        }
        return Collections.unmodifiableList(recipes);
    }

    @Override
    public RecipeResolver<Recipe<?, ?>> getResolver() {
        return resolver;
    }
}
