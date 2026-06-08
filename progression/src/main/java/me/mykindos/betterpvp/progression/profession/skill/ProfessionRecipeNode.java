package me.mykindos.betterpvp.progression.profession.skill;

import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.recipe.Recipe;
import me.mykindos.betterpvp.core.recipe.RecipeRegistries;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.progression.Progression;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class ProfessionRecipeNode extends ProfessionNode {

    private final Set<Key> recipes;

    public ProfessionRecipeNode(String id, Set<Key> recipes) {
        super(id);
        this.recipes = recipes;
    }

    @Override
    public boolean isGlowing() {
        return true;
    }

    @Override
    public net.kyori.adventure.text.Component[] getDescription(int level) {
        final RecipeRegistries registries = JavaPlugin.getPlugin(Progression.class).getInjector().getInstance(RecipeRegistries.class);
        List<net.kyori.adventure.text.Component> desc = new ArrayList<>();
        desc.add(Translations.component("progression.recipe.unlock-following"));
        for (Key key : recipes) {
            final Optional<Recipe<?>> recipeOpt = registries.getRecipe(NamespacedKey.fromString(key.asString()));
            if (recipeOpt.isEmpty()) {
                continue; // not found
            }

            final Recipe<?> recipe = recipeOpt.get();
            final Component text;
            if (recipe.previewResult() instanceof ItemInstance instance) {
                text = instance.getView().getName();
            } else {
                text = Component.text(recipe.previewResult().toString());
            }

            desc.add(net.kyori.adventure.text.Component.text(" - ").color(NamedTextColor.GRAY)
                    .append(text.colorIfAbsent(NamedTextColor.GREEN)));
        }
        return desc.toArray(new net.kyori.adventure.text.Component[0]);
    }

    public Set<Key> getRecipes() {
        return Collections.unmodifiableSet(recipes);
    }

    public boolean hasRecipe(Key recipe) {
        return recipes.contains(recipe);
    }

    /**
     * Produces a human-readable requirement lore line for this node,
     * e.g. {@code "Requires Woodcutting Lvl. 30"}.
     *
     * <p>The profession name is taken from {@link #getProgressionTree()}, which is set during
     * {@link #initialize(String)}. The level is taken from
     * {@link ProfessionNodeDependency#getRequiredLevel()} if non-zero; otherwise the lore falls
     * back to a generic description without a numeric level.</p>
     */
    public Component getRequirementLore() {
        String professionName = getProgressionTree();
        if (professionName == null || professionName.isBlank()) {
            return Translations.component("progression.recipe.requires-generic");
        }

        // Localized profession name when available, else a capitalized fallback.
        final String professionKey = "progression.profession." + professionName.toLowerCase() + ".name";
        final String capitalized = professionName.substring(0, 1).toUpperCase() + professionName.substring(1).toLowerCase();
        final Component professionComponent = (Translations.hasTranslation(professionKey)
                ? Translations.component(professionKey)
                : Component.text(capitalized)).color(NamedTextColor.GREEN);

        ProfessionNodeDependency dep = getDependencies();
        int level = dep != null ? dep.getRequiredLevel() : 0;
        if (level > 0) {
            return Translations.component("progression.recipe.unlocked-with",
                    professionComponent,
                    Component.text(level, NamedTextColor.GREEN));
        }

        return Translations.component("progression.recipe.requires", professionComponent);
    }

}
