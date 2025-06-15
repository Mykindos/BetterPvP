package me.mykindos.betterpvp.core.item.component.impl.blueprint;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.recipe.Recipe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A component that associates a blueprint with a recipe.
 * When applied to an item, it makes the item function as a blueprint.
 */
@Getter
public class BlueprintComponent extends AbstractItemComponent implements LoreComponent {

    private final List<Recipe> recipes;

    /**
     * Creates a new blueprint component.
     *
     * @param recipes The recipes this blueprint is for
     */
    public BlueprintComponent(@NotNull List<Recipe> recipes) {
        super("blueprint");
        this.recipes = recipes;
    }

    @Contract(mutates = "this", value = "_ -> this")
    public BlueprintComponent withRecipes(@NotNull List<Recipe> recipes) {
        this.recipes.clear();
        this.recipes.addAll(recipes);
        return this;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new BlueprintComponent(recipes);
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        return recipes.stream()
                .map(Recipe::createPrimaryResult)
                .sorted((r1, r2) -> Integer.compare(r2.getRarity().getImportance(), r1.getRarity().getImportance()))
                .map(result -> Component.text("● ", NamedTextColor.GRAY).append(result.getView().getName()))
                .distinct()
                .map(Component::asComponent)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    @Override
    public int getRenderPriority() {
        return 0;
    }
}