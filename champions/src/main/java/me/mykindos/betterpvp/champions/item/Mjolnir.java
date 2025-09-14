package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.Heavensplitter;
import me.mykindos.betterpvp.champions.item.ability.SkyforgedAscent;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.DurakHandle;
import me.mykindos.betterpvp.core.item.impl.Rope;
import me.mykindos.betterpvp.core.item.impl.StormInABottle;
import me.mykindos.betterpvp.core.item.impl.VoidglassCore;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.item.adapter.nexo.NexoItem;
import me.mykindos.betterpvp.core.metal.Runesteel;
import me.mykindos.betterpvp.core.recipe.RecipeIngredient;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.recipe.crafting.ShapedCraftingRecipe;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.jetbrains.annotations.NotNull;

@Singleton
@EqualsAndHashCode(callSuper = true)
public class Mjolnir extends WeaponItem implements ReloadHook, NexoItem {

    @EqualsAndHashCode.Exclude
    private final SkyforgedAscent skyforgedAscent;
    @EqualsAndHashCode.Exclude
    private final Heavensplitter heavensplitter;
    @EqualsAndHashCode.Exclude
    private final Champions champions;

    @Inject
    private Mjolnir(Champions champions, CooldownManager cooldownManager, ItemFactory itemFactory, EffectManager effectManager, ClientManager clientManager) {
        super(champions, "Mjolnir", Item.model(Material.TRIDENT, "mjolnir"), ItemRarity.MYTHICAL);
        this.champions = champions;
        this.skyforgedAscent = new SkyforgedAscent(effectManager, cooldownManager, this, itemFactory, clientManager);
        this.heavensplitter = new Heavensplitter(this, itemFactory, clientManager);

        // Create and add the tilling tremor ability
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(skyforgedAscent)
                .ability(heavensplitter)
                .build());
    }

    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(champions.getClass(), this);

        // Update the abilities with the new config values
        skyforgedAscent.setVelocity(config.getConfig("skyforged-ascent.velocity", 1.0, Double.class));
        skyforgedAscent.setCooldown(config.getConfig("skyforged-ascent.cooldown", 10.0, Double.class));
        skyforgedAscent.setSpeedAmplifier(config.getConfig("skyforged-ascent.speed-amplifier", 1, Integer.class));
        skyforgedAscent.setSpeedDuration(config.getConfig("skyforged-ascent.speed-duration", 10.0, Double.class));
        heavensplitter.setDamage(config.getConfig("heavensplitter.damage", 10.0, Double.class));
        heavensplitter.setHitbox((float) (double) config.getConfig("heavensplitter.hitbox", 1.6, Double.class));
        heavensplitter.setAirTime(config.getConfig("heavensplitter.air-time", 3.0, Double.class));
        heavensplitter.setVelocity(config.getConfig("heavensplitter.velocity", 1.0, Double.class));
        heavensplitter.setImpactVelocity(config.getConfig("heavensplitter.impact-velocity", 1.0, Double.class));
    }

    @Override
    public @NotNull String getId() {
        return "mjolnir";
    }

    @Inject
    private void registerRecipe(CraftingRecipeRegistry registry, ItemFactory itemFactory,
                                Runesteel.BlockItem runesteelBlock, StormInABottle stormInABottle,
                                VoidglassCore voidglassCore, Rope rope, DurakHandle durakHandle) {
        String[] pattern = new String[] {
                "BBB",
                "BRB",
                "SDV"
        };
        final ShapedCraftingRecipe.Builder builder = new ShapedCraftingRecipe.Builder(this, pattern, itemFactory);
        builder.setIngredient('B', new RecipeIngredient(runesteelBlock, 1));
        builder.setIngredient('R', new RecipeIngredient(rope, 1));
        builder.setIngredient('S', new RecipeIngredient(stormInABottle, 1));
        builder.setIngredient('D', new RecipeIngredient(durakHandle, 1));
        builder.setIngredient('V', new RecipeIngredient(voidglassCore, 1));
        registry.registerRecipe(new NamespacedKey("champions", "mjolnir"), builder.build());
    }
}
