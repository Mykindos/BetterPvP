package me.mykindos.betterpvp.core.item.component.impl.blueprint;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.experimental.Delegate;
import me.mykindos.betterpvp.core.interaction.AbstractInteraction;
import me.mykindos.betterpvp.core.interaction.InteractionResult;
import me.mykindos.betterpvp.core.interaction.actor.InteractionActor;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.context.InteractionContext;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.renderer.LoreComponentRenderer;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Optional;

/**
 * Represents a blueprint item that can be used to unlock recipes in a workbench.
 * Blueprints are tied to specific recipes and can be stored in a workbench.
 */
@Getter
@Singleton
@ItemKey("core:blueprint")
public class BlueprintItem extends BaseItem {

    @Delegate
    private final BlueprintComponent blueprint;

    @Inject
    private BlueprintItem(LoreComponentRenderer loreRenderer) {
        super(Item.model("blueprint"), ItemGroup.MISC, BlueprintItem::getRarity, new LoreComponentRenderer(), BlueprintItem::createBlueprintName);
        this.blueprint = new BlueprintComponent(new ArrayList<>());
        addSerializableComponent(blueprint);
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.NONE, new BlueprintAbility())
                .build());
    }

    private static Optional<ItemInstance> getHighestRarityResult(ItemInstance blueprint) {
        return blueprint.getComponent(BlueprintComponent.class)
                .orElseThrow(() -> new IllegalArgumentException("Item is not a blueprint"))
                .getCraftingRecipes()
                .stream()
                .map(CraftingRecipe::createPrimaryResult)
                .max(Comparator.comparingInt(r -> r.getRarity().getImportance()));
    }

    private static ItemRarity getRarity(ItemInstance itemInstance) {
        return getHighestRarityResult(itemInstance)
                .map(ItemInstance::getRarity)
                .orElse(ItemRarity.COMMON);
    }

    /**
     * Creates the display name for a blueprint based on the recipe's result.
     *
     * @param itemInstance The item instance representing the blueprint
     * @return The blueprint display name
     */
    private static Component createBlueprintName(ItemInstance itemInstance) {
        ItemRarity rarity = getRarity(itemInstance);
        return Component.text("Blueprint: ", TextColor.color(60, 125, 222))
                .append(Component.text(rarity.getName()).color(rarity.getColor()));
    }

    private static class BlueprintAbility extends AbstractInteraction {

        public BlueprintAbility() {
            super("Add Blueprint", "Use this on a Workbench to add the blueprint");
        }

        @Override
        protected @NotNull InteractionResult doExecute(@NotNull InteractionActor actor, @NotNull InteractionContext context,
                                                        @Nullable ItemInstance itemInstance, @Nullable ItemStack itemStack) {
            return new InteractionResult.Fail(InteractionResult.FailReason.CONDITIONS); // Ignore, this is just a marker ability
        }
    }

}