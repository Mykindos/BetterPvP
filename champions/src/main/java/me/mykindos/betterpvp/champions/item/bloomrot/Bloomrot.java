package me.mykindos.betterpvp.champions.item.bloomrot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.GatorStrokeAbility;
import me.mykindos.betterpvp.champions.item.ability.UnderwaterBreathingAbility;
import me.mykindos.betterpvp.champions.item.ability.WaterDamageAbility;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.energy.EnergyHandler;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.AlligatorScale;
import me.mykindos.betterpvp.core.item.impl.DurakHandle;
import me.mykindos.betterpvp.core.item.impl.FangOfTheDeep;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;

@Singleton
@EqualsAndHashCode(callSuper = true)
public class Bloomrot extends WeaponItem implements ReloadHook {

    private final ItemFactory itemFactory;
    private final NectarOfDecay nectarOfDecayAbility;

    @Inject
    private Bloomrot(Champions champions, NectarOfDecay nectarOfDecay, ItemFactory itemFactory) {
        super(champions, "Bloomroot", Item.model("bloomrot"), ItemRarity.LEGENDARY);
        this.itemFactory = itemFactory;

        this.nectarOfDecayAbility = nectarOfDecay;
        Bukkit.getPluginManager().registerEvents(nectarOfDecayAbility, champions);

        // Add abilities to container
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(nectarOfDecayAbility)
                .build());
    }

    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(Champions.class, this);

        nectarOfDecayAbility.setCooldown(config.getConfig("beamCooldown", 15.0, Double.class));
        nectarOfDecayAbility.setPoisonSeconds(config.getConfig("poisonSeconds", 2.0, Double.class));
        nectarOfDecayAbility.setPoisonAmplifier(config.getConfig("poisonAmplifier", 1, Integer.class));
        nectarOfDecayAbility.setSpeed(config.getConfig("beamSpeed", 30.0, Double.class));
        nectarOfDecayAbility.setHitbox(config.getConfig("beamHitbox", 0.6, Double.class));
        nectarOfDecayAbility.setTravelSeconds(config.getConfig("beamTravelSeconds", 3.0, Double.class));
        nectarOfDecayAbility.setCloudRadius(config.getConfig("cloudRadius", 4.0, Double.class));
        nectarOfDecayAbility.setCloudSeconds(config.getConfig("cloudSeconds", 6.0, Double.class));
        nectarOfDecayAbility.setHealPercent(config.getConfig("healPercent", 0.80, Double.class));
    }
} 