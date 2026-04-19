package me.mykindos.betterpvp.progression.profession.skill;

import net.kyori.adventure.key.Key;

import java.util.Collections;
import java.util.Set;

public class ProfessionRecipeNode extends ProfessionNode {

    private final Set<Key> recipes;

    public ProfessionRecipeNode(String id, Set<Key> recipes) {
        super(id);
        this.recipes = recipes;
    }

    public Set<Key> getRecipes() {
        return Collections.unmodifiableSet(recipes);
    }

    public boolean hasRecipe(Key recipe) {
        return recipes.contains(recipe);
    }

}
