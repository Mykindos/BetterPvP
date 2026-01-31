package me.mykindos.betterpvp.champions.item.scythe;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.UseEffects;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.LifestealAbility;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.BurntRemnant;
import me.mykindos.betterpvp.core.item.impl.DurakHandle;
import me.mykindos.betterpvp.core.item.impl.ReapersEdge;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Singleton
@CustomLog
@EqualsAndHashCode(callSuper = true)
@Getter
@ItemKey("champions:scythe_of_the_fallen_lord")
public class ScytheOfTheFallenLord extends WeaponItem implements Reloadable {

    private static final ItemStack model;

    static {
        model = Item.model("scythe_of_the_fallen_lord", 1);
        model.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
                .consumeSeconds(Float.MAX_VALUE)
                .animation(ItemUseAnimation.NONE)
                .build());
        model.setData(DataComponentTypes.USE_EFFECTS, UseEffects.useEffects()
                .canSprint(true)
                .speedMultiplier(1f)
                .build());
    }

    private transient boolean registered;

    private final LifestealAbility lifestealAbility;
    private final SoulHarvestAbility soulHarvestAbility;
    private final ItemFactory itemFactory;

    private double baseHeal;
    private double healPerSoul;

    @Inject
    private ScytheOfTheFallenLord(Champions champions,
                                  ItemFactory itemFactory,
                                  SoulHarvestAbility soulHarvestAbility) {
        super(champions, "Scythe", model, ItemRarity.LEGENDARY);
        this.lifestealAbility = new LifestealAbility(champions, itemFactory, this, this::getHeal);
        this.soulHarvestAbility = soulHarvestAbility;
        this.itemFactory = itemFactory;


        // Link this scythe to the SoulHarvestAbility
        this.soulHarvestAbility.setScythe(this);
        
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.PASSIVE, lifestealAbility)
                .root(InteractionInputs.HOLD_RIGHT_CLICK, soulHarvestAbility)
                .build());
    }

    private double getHeal(Player player) {
        // Calculate heal based on base heal and souls
        double soulCount = soulHarvestAbility.getPlayerData().get(player.getUniqueId()).getSoulCount();
        return baseHeal + (soulCount * healPerSoul);
    }
    
    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(Champions.class, this);
        
        // Configure Lifesteal ability
        this.baseHeal = config.getConfig("baseHeal", 1.0, Double.class);
        this.healPerSoul = config.getConfig("healPerSoul", 1.0, Double.class);
        
        // Configure SoulHarvest ability
        soulHarvestAbility.setMaxSoulsDamage(config.getConfig("maxSoulsDamage", 4.0, Double.class));
        soulHarvestAbility.setMaxSouls(config.getConfig("maxSouls", 3, Integer.class));
        soulHarvestAbility.setSoulHarvestSeconds(config.getConfig("soulHarvestSeconds", 1.5, Double.class));
        soulHarvestAbility.setSoulExpirySeconds(config.getConfig("soulExpirySeconds", 10.0, Double.class));
        soulHarvestAbility.setSoulExpiryPerSecond(config.getConfig("soulExpiryPerSecond", 0.3, Double.class));
        soulHarvestAbility.setSoulDespawnSeconds(config.getConfig("soulDespawnSeconds", 7.5, Double.class));
        soulHarvestAbility.setSoulViewDistanceBlocks(config.getConfig("soulViewDistanceBlocks", 60, Integer.class));
        soulHarvestAbility.setSoulsPerPlayer(config.getConfig("soulsPerPlayer", 1.0, Double.class));
        soulHarvestAbility.setSoulsPerMob(config.getConfig("soulsPerMob", 1.0, Double.class));
        soulHarvestAbility.setSummonPlayerSoulChance(config.getConfig("summonPlayerSoulChance", 1.0, Double.class));
        soulHarvestAbility.setSummonMobSoulChance(config.getConfig("summonMobSoulChance", 0.4, Double.class));
        soulHarvestAbility.setSpeedAmplifierPerSoul(config.getConfig("speedAmplifierPerSoul", 1.0, Double.class));
    }

    public boolean isScythe(ItemStack item) {
        return item != null && itemFactory.fromItemStack(item)
                .map(itemInstance -> itemInstance.getBaseItem() instanceof ScytheOfTheFallenLord)
                .orElse(false);
    }

    public ItemInstance getScytheInstance(Player player) {
        Preconditions.checkNotNull(player, "Player cannot be null");
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        ItemStack offHandItem = player.getInventory().getItemInOffHand();

        if (isScythe(mainHandItem)) {
            return itemFactory.fromItemStack(mainHandItem).orElse(null);
        } else if (isScythe(offHandItem)) {
            return itemFactory.fromItemStack(offHandItem).orElse(null);
        }
        return null;
    }

    public boolean isHoldingWeapon(Player player) {
        Preconditions.checkNotNull(player, "Player cannot be null");
        return getScytheInstance(player) != null;
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory,
                                BurntRemnant burntRemnant, ReapersEdge reapersEdge, DurakHandle durakHandle) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                "BRR",
                "D",
                "D"
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('B', new RecipeIngredient(burntRemnant, 1));
        builder.setIngredient('R', new RecipeIngredient(reapersEdge, 1));
        builder.setIngredient('D', new RecipeIngredient(durakHandle, 1));
        registry.registerRecipe(new NamespacedKey("champions", "scythe_of_the_fallen_lord"), builder.build());
    }
}