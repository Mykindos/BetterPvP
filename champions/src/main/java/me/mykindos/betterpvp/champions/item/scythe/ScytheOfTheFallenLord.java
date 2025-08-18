package me.mykindos.betterpvp.champions.item.scythe;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.LifestealAbility;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@Singleton
@CustomLog
@EqualsAndHashCode(callSuper = true)
@Getter
public class ScytheOfTheFallenLord extends WeaponItem implements ReloadHook {

    private final LifestealAbility lifestealAbility;
    private final SoulHarvestAbility soulHarvestAbility;
    private final ItemFactory itemFactory;

    private double baseHeal;
    private double healPerSoul;

    @Inject
    private ScytheOfTheFallenLord(Champions champions,
                                  ItemFactory itemFactory,
                                  SoulHarvestAbility soulHarvestAbility) {
        super(champions, "Scythe", Item.model("scythe_of_the_fallen_lord", 1), ItemRarity.LEGENDARY);
        this.lifestealAbility = new LifestealAbility(champions, itemFactory, this, this::getHeal);
        this.soulHarvestAbility = soulHarvestAbility;
        this.itemFactory = itemFactory;
        
        // Link this scythe to the SoulHarvestAbility
        this.soulHarvestAbility.setScythe(this);
        
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(lifestealAbility)
                .ability(soulHarvestAbility)
                .build());
    }

    private double getHeal(Player player) {
        // Calculate heal based on base heal and souls
        double soulCount = soulHarvestAbility.getPlayerData().get(player).getSoulCount();
        return baseHeal + (soulCount * healPerSoul);
    }
    
    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(Champions.class, this);
        
        // Configure Lifesteal ability
        this.baseHeal = config.getConfig("baseHeal", 0.7, Double.class);
        this.healPerSoul = config.getConfig("healPerSoul", 0.1, Double.class);
        
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
}