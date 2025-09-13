package me.mykindos.betterpvp.core.recipe;

import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.recipe.resolver.RecipeResolver;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
@CustomLog
public class RecipeRegistries implements RecipeRegistry<Recipe<?, ?>> {

    private final Set<RecipeRegistry<?>> registries = new HashSet<>();
    private final RecipeResolver<Recipe<?, ?>> resolver = new RecipeResolver<>(this);

    public void register(RecipeRegistry<?> registry) {
        registries.add(registry);
        log.info("Registered recipe registry: {}", registry.getClass().getSimpleName()).submit();
    }

    public Set<RecipeRegistry<?>> getChildren() {
        return Collections.unmodifiableSet(registries);
    }

    @Override
    public void registerRecipe(Recipe<?, ?> recipe) {
        throw new UnsupportedOperationException("Cannot register recipes to this registry");
    }

    @Override
    public Collection<Recipe<?, ?>> getRecipes() {
        List<Recipe<?, ?>> recipes = new ArrayList<>();
        for (RecipeRegistry<?> registry : registries) {
            recipes.addAll(registry.getRecipes());
        }
        return Collections.unmodifiableList(recipes);
    }

    @Override
    public RecipeResolver<Recipe<?, ?>> getResolver() {
        return resolver;
    }
}
