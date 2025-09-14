package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.MagnetismAbility;
import me.mykindos.betterpvp.champions.item.ability.ReverseKnockbackAbility;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.DurakHandle;
import me.mykindos.betterpvp.core.item.impl.MagneticShard;
import me.mykindos.betterpvp.core.item.impl.PolariteChunk;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;

@Singleton
@EqualsAndHashCode(callSuper = true)
public class MagneticMaul extends WeaponItem implements ReloadHook {

    private final MagnetismAbility magnetismAbility;
    private final ReverseKnockbackAbility reverseKnockbackAbility;

    @Inject
    private MagneticMaul(Champions champions,
                         MagnetismAbility magnetismAbility,
                         ItemFactory itemFactory) {
        super(champions, "Magnetic Maul", Item.model("magnetic_maul"), ItemRarity.LEGENDARY);
        this.magnetismAbility = magnetismAbility;
        this.reverseKnockbackAbility = new ReverseKnockbackAbility(champions, itemFactory, this);
        
        // Add abilities to container
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(magnetismAbility)
                .ability(reverseKnockbackAbility)
                .build());
    }

    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(Champions.class, this);
        
        // Configure Magnetism ability
        magnetismAbility.setPullRange(config.getConfig("pullRange", 10.0, Double.class));
        magnetismAbility.setPullFov(config.getConfig("pullFov", 80.3, Double.class));
        magnetismAbility.setEnergyPerTick(config.getConfig("energyPerTick", 2.0, Double.class));
        
        // Configure Reverse Knockback ability
        reverseKnockbackAbility.setKnockbackMultiplier(config.getConfig("knockbackMultiplier", -1.0, Double.class));
        reverseKnockbackAbility.setBypassMinimum(config.getConfig("bypassMinimumKnockback", true, Boolean.class));
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory,
                                MagneticShard magneticShard, PolariteChunk polariteChunk, DurakHandle durakHandle) {
        String[] pattern = new String[] {
                "PPP",
                "PMP",
                " D "
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('P', new RecipeIngredient(polariteChunk, 1));
        builder.setIngredient('M', new RecipeIngredient(magneticShard, 1));
        builder.setIngredient('D', new RecipeIngredient(durakHandle, 1));
        registry.registerRecipe(new NamespacedKey("champions", "magnetic_maul"), builder.build());
    }
} 