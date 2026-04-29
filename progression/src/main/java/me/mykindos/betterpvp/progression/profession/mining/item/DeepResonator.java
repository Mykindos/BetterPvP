package me.mykindos.betterpvp.progression.profession.mining.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.blockbreak.component.ToolComponent;
import me.mykindos.betterpvp.core.framework.blockbreak.global.GlobalBlockBreakRules;
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
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.DivineAmulet;
import me.mykindos.betterpvp.core.item.impl.DurakHandle;
import me.mykindos.betterpvp.core.item.impl.OverchargedCrystal;
import me.mykindos.betterpvp.core.item.impl.VoidSphere;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.mining.item.interaction.VeinEchoInteraction;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

@Singleton
@EqualsAndHashCode(callSuper = true)
@ItemKey("progression:deep_resonator")
public class DeepResonator extends BaseItem implements Reloadable {

    @EqualsAndHashCode.Exclude
    private final VeinEchoInteraction veinEchoInteraction;

    @EqualsAndHashCode.Exclude
    private final Progression progression;
    private transient boolean registered;

    @Inject
    private DeepResonator(Progression progression, GlobalBlockBreakRules globalRules) {
        super("Deep Resonator",
                Item.model(Material.DIAMOND_PICKAXE, "deep_resonator"),
                ItemGroup.TOOL,
                ItemRarity.EPIC);
        this.progression = progression;
        this.veinEchoInteraction = new VeinEchoInteraction(progression, globalRules,
                10.0, // duration seconds
                0.50, // ore-respawn chance per ore mine
                1,    // bonus stacks granted on a successful respawn
                30,   // max stacks
                30    // framework-scaled speed bonus per stack (1× diamond tier ≈ +0.5 diamond per 15)
        );

        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.BLOCK_BREAK, veinEchoInteraction)
                .build());

        addBaseComponent(new ToolComponent()
                .addRule(BlockBreakRule.of(BlockGroups.STONES, BlockBreakProperties.breakable(180))));
        addSerializableComponent(new DurabilityComponent(2560));
    }

    @Override
    public void reload() {
        final Config parent = Config.item(progression, this);
        getComponent(DurabilityComponent.class).ifPresent(durability -> {
            durability.setMaxDamage(parent.getConfig("durability.max-damage", 2560, Integer.class));
        });

        final Config veinEchoConfig = parent.fork("vein-echo");
        veinEchoInteraction.setDurationSeconds(veinEchoConfig.getConfig("duration", 10.0, Double.class));
        veinEchoInteraction.setOreRespawnChance(veinEchoConfig.getConfig("ore-respawn-chance", 0.50, Double.class));
        veinEchoInteraction.setOreRespawnBonusStacks(veinEchoConfig.getConfig("ore-respawn-bonus-stacks", 1, Integer.class));
        veinEchoInteraction.setMaxStacks(veinEchoConfig.getConfig("max-stacks", 20, Integer.class));
        veinEchoInteraction.setSpeedPerStack(veinEchoConfig.getConfig("speed-per-stack", 30, Integer.class));
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry,
                                ItemFactory itemFactory,
                                DivineAmulet divineAmulet,
                                VoidSphere voidSphere,
                                DurakHandle durakHandle,
                                OverchargedCrystal overchargedCrystal) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                "VVV",
                "ADO",
                " D ",
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('V', new RecipeIngredient(voidSphere, 1));
        builder.setIngredient('A', new RecipeIngredient(divineAmulet, 1));
        builder.setIngredient('O', new RecipeIngredient(overchargedCrystal, 1));
        builder.setIngredient('D', new RecipeIngredient(durakHandle, 1));
        registry.registerRecipe(new NamespacedKey("progression", "deep_resonator"), builder.build());
    }
}
