package me.mykindos.betterpvp.progression.profession.mining.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.access.AccessScope;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.blockbreak.component.ToolComponent;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakProperties;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.BlockBreakRule;
import me.mykindos.betterpvp.core.framework.blockbreak.rule.preset.BlockGroups;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.TooltipSpriteComponent;
import me.mykindos.betterpvp.core.item.component.impl.access.RestrictedAccessComponent;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.component.impl.repair.RepairableComponent;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.ElderwoodCore;
import me.mykindos.betterpvp.core.item.impl.MagicEssence;
import me.mykindos.betterpvp.core.item.impl.MagicSeal;
import me.mykindos.betterpvp.core.item.impl.PolariteChunk;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.mining.item.interaction.InstantMineInteraction;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.Set;

@Singleton
@EqualsAndHashCode(callSuper = true)
@ItemKey("progression:runed_pickaxe")
public class RunedPickaxe extends BaseItem implements Reloadable {

    @EqualsAndHashCode.Exclude
    private final InstantMineInteraction instantMineInteraction;

    @EqualsAndHashCode.Exclude
    private final Progression progression;

    private transient boolean registered;

    @Inject
    private RunedPickaxe(Progression progression,
                         CooldownManager cooldownManager,
                         ClientManager clientManager,
                         ItemFactory itemFactory) {
        super("Runed Pickaxe",
                Item.model(Material.DIAMOND_PICKAXE, "runed_pickaxe"),
                ItemGroup.TOOL,
                ItemRarity.EPIC);
        this.progression = progression;
        this.instantMineInteraction = new InstantMineInteraction(cooldownManager, clientManager, itemFactory,
                20.0, 7.0, BlockGroups.STONES);

        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.RIGHT_CLICK, instantMineInteraction)
                .build());
        addBaseComponent(TooltipSpriteComponent.of("\uE00D"));

        addBaseComponent(new ToolComponent()
                .addRule(BlockBreakRule.of(BlockGroups.STONES, BlockBreakProperties.breakable(180))));

        addBaseComponent(new RestrictedAccessComponent(Set.of(AccessScope.CRAFT, AccessScope.USE, AccessScope.DAMAGE)));

        addSerializableComponent(new DurabilityComponent(3584));
        addSerializableComponent(new RepairableComponent());
        addSerializableComponent(new SocketableContainerComponent());
    }

    @Override
    public void reload() {
        final Config parent = Config.item(progression, this);
        getComponent(DurabilityComponent.class).ifPresent(durability -> {
            durability.setMaxDamage(parent.getConfig("durability.max-damage", 2560, Integer.class));
        });

        final Config instantMineConfig = parent.fork("instantMine");
        instantMineInteraction.setCooldown(instantMineConfig.getConfig("cooldown", 20.0, Double.class));
        instantMineInteraction.setDuration(instantMineConfig.getConfig("duration", 7.0, Double.class));
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry,
                                ItemFactory itemFactory,
                                MagicEssence magicEssence,
                                MagicSeal magicSeal,
                                ElderwoodCore elderwoodCore,
                                PolariteChunk polariteChunk) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                "PPP",
                "EDS",
                " D ",
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('P', new RecipeIngredient(polariteChunk, 1));
        builder.setIngredient('E', new RecipeIngredient(magicEssence, 1));
        builder.setIngredient('S', new RecipeIngredient(magicSeal, 1));
        builder.setIngredient('D', new RecipeIngredient(elderwoodCore, 1));
        registry.registerRecipe(new NamespacedKey("progression", "runed_pickaxe"), builder.build());
    }
}
