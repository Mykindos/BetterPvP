package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.TillingTremorAbility;
import me.mykindos.betterpvp.core.imbuement.ImbuementRecipeRegistry;
import me.mykindos.betterpvp.core.imbuement.StandardImbuementRecipe;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.MagicSeal;
import me.mykindos.betterpvp.core.item.impl.OverchargedCrystal;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;

import java.util.Map;

@Singleton
@EqualsAndHashCode(callSuper = true)
@ItemKey("champions:rake")
public class Rake extends WeaponItem implements Reloadable {

    private final TillingTremorAbility ability;

    @EqualsAndHashCode.Exclude
    private final Champions champions;
    private transient boolean registered;

    @Inject
    public Rake(Champions champions, TillingTremorAbility tillingTremorAbility) {
        super(champions, "Rake", Item.model("rake"), ItemRarity.LEGENDARY);
        this.champions = champions;

        // Create and add the tilling tremor ability
        this.ability = tillingTremorAbility;
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.RIGHT_CLICK, ability)
                .build());
    }

    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(champions.getClass(), this);
        double cooldown = config.getConfig("tremorCooldown", 3.0, Double.class);
        double damage = config.getConfig("tremorDamage", 5.0, Double.class);

        ability.setCooldown(cooldown);
        ability.setDamage(damage);
    }

    @Inject
    private void registerRecipe(ImbuementRecipeRegistry registry, ItemFactory itemFactory,
                                MagicSeal magicSeal, OverchargedCrystal overchargedCrystal) {
        if (registered) return;
        registered = true;
        final BaseItem diamondHoe = itemFactory.getFallbackItem(Material.DIAMOND_HOE);
        final Map<BaseItem, Integer> ingredients = Map.of(
                magicSeal, 1,
                overchargedCrystal, 2,
                diamondHoe, 1
        );
        final StandardImbuementRecipe recipe = new StandardImbuementRecipe(ingredients, this, itemFactory);
        registry.registerRecipe(new NamespacedKey("champions", "rake"), recipe);
    }
}
