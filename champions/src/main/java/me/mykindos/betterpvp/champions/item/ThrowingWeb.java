package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.item.ability.ThrowingWebAbility;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("champions:throwing_web")
@EqualsAndHashCode(callSuper = false)
public class ThrowingWeb extends BaseItem implements Reloadable {

    private transient boolean registered;
    private final ThrowingWebAbility throwingWebAbility;

    @Inject
    private ThrowingWeb(ChampionsManager championsManager, WorldBlockHandler blockHandler, CooldownManager cooldownManager) {
        super(translatableName("champions.item.throwing-web.name"), ItemStack.of(Material.COBWEB), ItemGroup.WEAPON, ItemRarity.UNCOMMON);
        this.throwingWebAbility = new ThrowingWebAbility(championsManager, blockHandler, cooldownManager);
        throwingWebAbility.setConsumesItem(true);
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.LEFT_CLICK, throwingWebAbility)
                .build());
    }

    @Override
    public void reload() {
        final Config config = Config.item(Champions.class, this);
        double duration = config.getConfig("duration", 2.5, Double.class);
        double throwableExpiry = config.getConfig("throwableExpiry", 10.0, Double.class);
        double cooldown = config.getConfig("cooldown", 10.0, Double.class);
        throwingWebAbility.setDuration(duration);
        throwingWebAbility.setThrowableExpiry(throwableExpiry);
        throwingWebAbility.setCooldown(cooldown);
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory) {
        if (registered) return;
        registered = true;
        final BaseItem string = itemFactory.getFallbackItem(Material.STRING);

        String[] pattern = new String[] {
                "S S",
                " S ",
                "S S"
        };

        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('S', new RecipeIngredient(string, 1));
        registry.registerRecipe(new NamespacedKey("champions", "throwing_web"), builder.build());
    }
}
