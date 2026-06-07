package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.HealingNovaAbility;
import me.mykindos.betterpvp.champions.item.ability.MeridianBeamAbility;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.TooltipSpriteComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.DurakHandle;
import me.mykindos.betterpvp.core.item.impl.MeridianOrb;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.NamespacedKey;
import org.bukkit.event.Listener;

import java.util.List;

@Singleton
@BPvPListener
@EqualsAndHashCode(callSuper = true)
@ItemKey("champions:meridian_scepter")
public class MeridianScepter extends WeaponItem implements Listener, Reloadable {

    private transient boolean registered;
    private final HealingNovaAbility healingNovaAbility;
    private final MeridianBeamAbility meridianBeamAbility;

    @Inject
    private MeridianScepter(Champions champions,
                           HealingNovaAbility healingNovaAbility,
                           MeridianBeamAbility meridianBeamAbility) {
        super(champions, "Meridian Scepter", Item.model("meridian_scepter"), ItemRarity.LEGENDARY, List.of(Group.RANGED));
        this.healingNovaAbility = healingNovaAbility;
        this.meridianBeamAbility = meridianBeamAbility;

        // Add abilities to container
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.RIGHT_CLICK, healingNovaAbility)
                .root(InteractionInputs.LEFT_CLICK, meridianBeamAbility)
                .build());
        addBaseComponent(TooltipSpriteComponent.of("\uE00A"));
    }

    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(Champions.class, this);

        // Healing Nova configuration
        healingNovaAbility.setRadius(config.getConfig("healingNovaRadius", 1.5, Double.class));
        healingNovaAbility.setSpeed(config.getConfig("healingNovaSpeed", 3.0, Double.class));
        healingNovaAbility.setHitbox(config.getConfig("healingNovaHitbox", 0.5, Double.class));
        healingNovaAbility.setHealRadius(config.getConfig("healingNovaHealRadius", 7.0, Double.class));
        healingNovaAbility.setAliveSeconds(config.getConfig("healingNovaAliveSeconds", 1.3, Double.class));
        healingNovaAbility.setExpandSeconds(config.getConfig("healingNovaExpandSeconds", 0.75, Double.class));
        healingNovaAbility.setTravelSeconds(config.getConfig("healingNovaTravelSeconds", 2.0, Double.class));
        healingNovaAbility.setCooldown(config.getConfig("healingNovaCooldown", 10.0, Double.class));
        healingNovaAbility.setHealAmount(config.getConfig("healingNovaHealAmount", 12.0, Double.class));

        // Meridian Beam configuration
        meridianBeamAbility.setCooldown(config.getConfig("beamCooldown", 1.0, Double.class));
        meridianBeamAbility.setDamage(config.getConfig("beamDamage", 4.0, Double.class));
        meridianBeamAbility.setSpeed(config.getConfig("beamSpeed", 4.0, Double.class));
        meridianBeamAbility.setHitbox(config.getConfig("beamHitbox", 0.5, Double.class));
        meridianBeamAbility.setTravelSeconds(config.getConfig("beamTravelSeconds", 0.3, Double.class));
    }

    @UpdateEvent(priority = 100)
    public void processNovas() {
        healingNovaAbility.processNovas();
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
