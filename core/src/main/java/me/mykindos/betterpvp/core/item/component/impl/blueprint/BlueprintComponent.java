package me.mykindos.betterpvp.core.item.component.impl.blueprint;

import lombok.Getter;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
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

    private final List<CraftingRecipe> craftingRecipes;

    /**
     * Creates a new blueprint component.
     *
     * @param craftingRecipes The recipes this blueprint is for
     */
    public BlueprintComponent(@NotNull List<CraftingRecipe> craftingRecipes) {
        super("blueprint");
        this.craftingRecipes = craftingRecipes;
    }

    @Contract(mutates = "this", value = "_ -> this")
    public BlueprintComponent withCraftingRecipes(@NotNull List<CraftingRecipe> craftingRecipes) {
        this.craftingRecipes.clear();
        this.craftingRecipes.addAll(craftingRecipes);
        return this;
    }

    @Override
    public @NotNull ItemComponent copy() {
        return new BlueprintComponent(craftingRecipes);
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        return craftingRecipes.stream()
                .map(CraftingRecipe::createPrimaryResult)
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