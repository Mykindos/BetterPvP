package me.mykindos.betterpvp.core.item.component.impl.blueprint;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.experimental.Delegate;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerType;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.item.renderer.LoreComponentRenderer;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Represents a blueprint item that can be used to unlock recipes in a workbench.
 * Blueprints are tied to specific recipes and can be stored in a workbench.
 */
@Getter
@Singleton
public class BlueprintItem extends BaseItem {

    public static final ItemStack model = ItemView.builder()
            .material(Material.LEATHER_HORSE_ARMOR)
            .itemModel(Key.key("betterpvp", "blueprint"))
            .build().get();

    @Delegate
    private final BlueprintComponent blueprint;

    @Inject
    private BlueprintItem(LoreComponentRenderer loreRenderer) {
        super(model, ItemGroup.MISC, BlueprintItem::getRarity, new LoreComponentRenderer(), BlueprintItem::createBlueprintName);
        this.blueprint = new BlueprintComponent(new ArrayList<>());
        addSerializableComponent(blueprint);
        addBaseComponent(new AbilityContainerComponent(List.of(new BlueprintAbility())));
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

    private static class BlueprintAbility extends ItemAbility {

        private static final NamespacedKey KEY = new NamespacedKey(JavaPlugin.getPlugin(Core.class), "add_blueprint");

        public BlueprintAbility() {
            super(KEY, "Add Blueprint", "Use this on a Workbench to add the blueprint", TriggerTypes.RIGHT_CLICK);
        }

        @Override
        public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
            return false; // Ignore, this is just a marker ability
        }
    }

}