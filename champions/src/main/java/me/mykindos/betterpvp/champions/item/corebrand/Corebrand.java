package me.mykindos.betterpvp.champions.item.corebrand;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.TooltipSpriteComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.AetherCore;
import me.mykindos.betterpvp.core.item.impl.DurakHandle;
import me.mykindos.betterpvp.core.item.impl.ReapersEdge;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import net.kyori.adventure.key.Key;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

/**
 * A greatsword built around a living core. It banks combat pressure as fuel, and spends it in Cascade — a
 * short window where the blade wakes, every hit lands empowered, and Fission becomes available.
 */
@Singleton
@CustomLog
@EqualsAndHashCode(callSuper = true)
@Getter
@ItemKey("champions:corebrand")
public class Corebrand extends WeaponItem implements Reloadable {

    private static final ItemStack model = Item.model("corebrand");

    private transient boolean registered;

    private final CascadeAbility cascadeAbility;
    private final FissionLunge fissionLunge;
    private final ItemFactory itemFactory;

    /**
     * The model the item actually carries. The charged look is rendered over the top of this one, per holder,
     * so the stored item is always the dormant blade.
     */
    private final Key dormantModel;
    private Key chargedModel;

    @Inject
    private Corebrand(Champions champions, ItemFactory itemFactory, CascadeAbility cascadeAbility, FissionLunge fissionLunge) {
        super(champions, translatableName("champions.item.corebrand.name"), model, ItemRarity.LEGENDARY);
        this.itemFactory = itemFactory;
        this.cascadeAbility = cascadeAbility;
        this.fissionLunge = fissionLunge;
        this.dormantModel = model.getData(DataComponentTypes.ITEM_MODEL);

        cascadeAbility.setCorebrand(this);
        addBaseComponent(TooltipSpriteComponent.of("\uE014"));

        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.SWAP_HAND, cascadeAbility)
                .root(InteractionInputs.RIGHT_CLICK, fissionLunge)
                .build());
    }

    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(Champions.class, this);

        this.chargedModel = modelKey(config.getConfig("models.charged", "corebrand_charged", String.class));

        cascadeAbility.setFuelPerDamageDealt(config.getConfig("fuel.gain-per-damage-dealt", 0.008, Double.class));
        cascadeAbility.setFuelPerDamageTaken(config.getConfig("fuel.gain-per-damage-taken", 0.005, Double.class));
        cascadeAbility.setFuelDecayPerSecond(config.getConfig("fuel.decay-per-second", 0.02, Double.class));
        cascadeAbility.setDuration(config.getConfig("cascade.duration", 12.0, Double.class));
        cascadeAbility.setCascadeDecayPerSecond(config.getConfig("cascade.decay-per-second", 0.06, Double.class));
        cascadeAbility.setCascadeDecayGrace(config.getConfig("cascade.decay-grace", 2.0, Double.class));
        cascadeAbility.setHitCost(config.getConfig("cascade.hit-cost", 1.0 / 12.0, Double.class));
        cascadeAbility.setHitBonusDamage(config.getConfig("cascade.hit-bonus-damage", 6.0, Double.class));

        fissionLunge.setCost(config.getConfig("fission.cost", 0.2, Double.class));
        fissionLunge.setVelocity(config.getConfig("fission.velocity", 2.2, Double.class));
        fissionLunge.setDuration(config.getConfig("fission.duration", 0.5, Double.class));
        fissionLunge.setCooldown(config.getConfig("fission.cooldown", 0.6, Double.class));
        fissionLunge.setSlamRadius(config.getConfig("fission.slam-radius", 5.0, Double.class));
        fissionLunge.setSlamDamage(config.getConfig("fission.slam-damage", 7.0, Double.class));
        fissionLunge.setImpactVelocity(config.getConfig("fission.impact-velocity", 1.4, Double.class));
        fissionLunge.setMinimumLift(config.getConfig("fission.minimum-lift", 0.35, Double.class));
    }

    private Key modelKey(String name) {
        return Key.key(dormantModel.namespace(), "item/" + name);
    }

    public boolean isCorebrand(ItemStack item) {
        return item != null && itemFactory.fromItemStack(item)
                .map(itemInstance -> itemInstance.getBaseItem() instanceof Corebrand)
                .orElse(false);
    }

    public ItemInstance getCorebrandInstance(Player player) {
        Preconditions.checkNotNull(player, "Player cannot be null");
        final ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        final ItemStack offHandItem = player.getInventory().getItemInOffHand();

        if (isCorebrand(mainHandItem)) {
            return itemFactory.fromItemStack(mainHandItem).orElse(null);
        } else if (isCorebrand(offHandItem)) {
            return itemFactory.fromItemStack(offHandItem).orElse(null);
        }
        return null;
    }

    public boolean isHoldingWeapon(Player player) {
        Preconditions.checkNotNull(player, "Player cannot be null");
        return getCorebrandInstance(player) != null;
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory,
                                ReapersEdge reapersEdge, AetherCore aetherCore, DurakHandle durakHandle) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                "R",
                "A",
                "D"
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('R', new RecipeIngredient(reapersEdge, 1));
        builder.setIngredient('A', new RecipeIngredient(aetherCore, 1));
        builder.setIngredient('D', new RecipeIngredient(durakHandle, 1));
        registry.registerRecipe(new NamespacedKey("champions", "corebrand"), builder.build());
    }
}
