package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.item.ability.FeatherFeetAbility;
import me.mykindos.betterpvp.champions.item.ability.WindDashAbility;
import me.mykindos.betterpvp.champions.item.ability.WindSlashAbility;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.AetherCore;
import me.mykindos.betterpvp.core.item.impl.DurakHandle;
import me.mykindos.betterpvp.core.item.impl.FeatherOfZephyr;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;

import java.util.List;

@Singleton
@BPvPListener
@EqualsAndHashCode(callSuper = true)
public class WindBlade extends WeaponItem implements Listener, ReloadHook {

    private final WindDashAbility windDashAbility;
    private final WindSlashAbility windSlashAbility;
    private final FeatherFeetAbility featherFeetAbility;
    private transient boolean registered;

    @Inject
    private WindBlade(Champions champions, ChampionsManager championsManager,
                     CooldownManager cooldownManager, EnergyHandler energyHandler, 
                     FeatherFeetAbility featherFeetAbility) {
        super(champions, "Wind Blade", Item.model("windblade"), ItemRarity.LEGENDARY, List.of(Group.MELEE, Group.RANGED));
        this.featherFeetAbility = featherFeetAbility;
        
        // Create abilities
        this.windDashAbility = new WindDashAbility(championsManager, champions);
        this.windSlashAbility = new WindSlashAbility(cooldownManager, energyHandler, this);
        
        // Add ability container
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(windDashAbility)
                .ability(windSlashAbility)
                .ability(featherFeetAbility)
                .build());
    }

    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(Champions.class, this);
        
        // Wind Dash
        double dashVelocity = config.getConfig("dashVelocity", 1.2, Double.class);
        int dashParticleTicks = config.getConfig("dashParticleTicks", 2, Integer.class);
        int dashEnergyCost = config.getConfig("dashEnergyCost", 24, Integer.class);
        double dashImpactVelocity = config.getConfig("dashImpactVelocity", 1.0, Double.class);
        
        windDashAbility.setDashVelocity(dashVelocity);
        windDashAbility.setDashParticleTicks(dashParticleTicks);
        windDashAbility.setDashEnergyCost(dashEnergyCost);
        windDashAbility.setDashImpactVelocity(dashImpactVelocity);
        
        // Wind Slash
        double slashCooldown = config.getConfig("slashCooldown", 2.5, Double.class);
        double slashHitboxSize = config.getConfig("slashHitboxSize", 0.6, Double.class);
        int slashEnergyCost = config.getConfig("slashEnergyCost", 0, Integer.class);
        double slashDamage = config.getConfig("slashDamage", 5.0, Double.class);
        double slashEnergyRefundPercent = config.getConfig("slashEnergyRefundPercent", 0.2, Double.class);
        double slashVelocity = config.getConfig("slashVelocity", 0.5, Double.class);
        int slashAliveMillis = config.getConfig("slashAliveMillis", 1000, Integer.class);
        double slashSpeed = config.getConfig("slashSpeed", 30.0, Double.class);
        
        windSlashAbility.setSlashCooldown(slashCooldown);
        windSlashAbility.setSlashHitboxSize(slashHitboxSize);
        windSlashAbility.setSlashEnergyCost(slashEnergyCost);
        windSlashAbility.setSlashDamage(slashDamage);
        windSlashAbility.setSlashEnergyRefundPercent(slashEnergyRefundPercent);
        windSlashAbility.setSlashVelocity(slashVelocity);
        windSlashAbility.setSlashAliveMillis(slashAliveMillis);
        windSlashAbility.setSlashSpeed(slashSpeed);
    }

    // Process abilities
    @UpdateEvent
    public void updateSlashes() {
        windSlashAbility.processSlashes();
    }

    @UpdateEvent
    public void updateDashes() {
        windDashAbility.processDashes();
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory,
                                AetherCore aetherCore, FeatherOfZephyr featherOfZephyr,
                                DurakHandle durakHandle) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                "F",
                "A",
                "D"
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('F', new RecipeIngredient(featherOfZephyr, 1));
        builder.setIngredient('A', new RecipeIngredient(aetherCore, 1));
        builder.setIngredient('D', new RecipeIngredient(durakHandle, 1));
        registry.registerRecipe(new NamespacedKey("champions", "windblade"), builder.build());
    }
}