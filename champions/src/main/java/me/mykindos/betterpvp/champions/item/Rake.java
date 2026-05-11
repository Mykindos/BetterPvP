package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.TillingTremorAbility;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;

@Singleton
@EqualsAndHashCode(callSuper = true)
@ItemKey("champions:rake")
public class Rake extends WeaponItem implements Reloadable {

    private final TillingTremorAbility ability;

    @EqualsAndHashCode.Exclude
    private final Champions champions;

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

}
