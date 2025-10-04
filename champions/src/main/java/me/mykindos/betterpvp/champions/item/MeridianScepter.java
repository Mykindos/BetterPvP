package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.BlackHoleAbility;
import me.mykindos.betterpvp.champions.item.ability.MeridianBeamAbility;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.DurakHandle;
import me.mykindos.betterpvp.core.item.impl.MeridianOrb;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
@EqualsAndHashCode(callSuper = true)
public class MeridianScepter extends WeaponItem implements Listener, ReloadHook {

    private transient boolean registered;
    private final BlackHoleAbility blackHoleAbility;
    private final MeridianBeamAbility meridianBeamAbility;

    @Inject
    private MeridianScepter(Champions champions, 
                           BlackHoleAbility blackHoleAbility, 
                           MeridianBeamAbility meridianBeamAbility) {
        super(champions, "Meridian Scepter", Item.model("meridian_scepter"), ItemRarity.LEGENDARY);
        this.blackHoleAbility = blackHoleAbility;
        this.meridianBeamAbility = meridianBeamAbility;
        
        // Add abilities to container
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(blackHoleAbility)
                .ability(meridianBeamAbility)
                .build());
    }

    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(Champions.class, this);
        
        // Black Hole configuration
        blackHoleAbility.setRadius(config.getConfig("blackHoleRadius", 0.5, Double.class));
        blackHoleAbility.setSpeed(config.getConfig("blackHoleSpeed", 3.0, Double.class));
        blackHoleAbility.setHitbox(config.getConfig("blackHoleHitbox", 0.5, Double.class));
        blackHoleAbility.setPullStrength(config.getConfig("blackHolePullStrength", 0.12, Double.class));
        blackHoleAbility.setPullRadius(config.getConfig("blackHolePullRadius", 3.5, Double.class));
        blackHoleAbility.setAliveSeconds(config.getConfig("blackHoleAliveSeconds", 1.3, Double.class));
        blackHoleAbility.setExpandSeconds(config.getConfig("blackHoleExpandSeconds", 0.75, Double.class));
        blackHoleAbility.setTravelSeconds(config.getConfig("blackHoleTravelSeconds", 2.0, Double.class));
        blackHoleAbility.setCooldown(config.getConfig("blackHoleCooldown", 10.0, Double.class));
        
        // Meridian Beam configuration
        meridianBeamAbility.setCooldown(config.getConfig("beamCooldown", 1.0, Double.class));
        meridianBeamAbility.setDamage(config.getConfig("beamDamage", 4.0, Double.class));
        meridianBeamAbility.setSpeed(config.getConfig("beamSpeed", 4.0, Double.class));
        meridianBeamAbility.setHitbox(config.getConfig("beamHitbox", 0.5, Double.class));
        meridianBeamAbility.setTravelSeconds(config.getConfig("beamTravelSeconds", 0.3, Double.class));
    }

    @UpdateEvent(priority = 100)
    public void processBlackHoles() {
        blackHoleAbility.processBlackHoles();
    }

    @UpdateEvent
    public void processBeams() {
        meridianBeamAbility.processBeams();
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory,
                                MeridianOrb orb, DurakHandle durakHandle) {
        if (registered) return;
        registered = true;
        String[] pattern = new String[] {
                "O",
                "D",
                "D"
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('O', new RecipeIngredient(orb, 1));
        builder.setIngredient('D', new RecipeIngredient(durakHandle, 1));
        registry.registerRecipe(new NamespacedKey("champions", "meridian_scepter"), builder.build());
    }
} 