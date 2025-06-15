package me.mykindos.betterpvp.champions.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.ability.TillingTremorAbility;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.ItemConfig;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@EqualsAndHashCode(callSuper = true)
public class Rake extends WeaponItem implements ReloadHook {

    private static final ItemStack model = ItemView.builder()
            .material(Material.LEATHER_HORSE_ARMOR)
            .itemModel(Material.MUSIC_DISC_WARD.key())
            .customModelData(2)
            .build().get();

    private final TillingTremorAbility ability;

    @EqualsAndHashCode.Exclude
    private final Champions champions;

    @Inject
    public Rake(Champions champions, TillingTremorAbility tillingTremorAbility) {
        super(champions, "Rake", model, ItemRarity.LEGENDARY);
        this.champions = champions;

        // Create and add the tilling tremor ability
        this.ability = tillingTremorAbility;
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(ability)
                .build());
    }

    @Override
    public void reload() {
        super.reload();
        final ItemConfig config = ItemConfig.of(champions.getClass(), this);
        double cooldown = config.getConfig("tremorCooldown", 3.0, Double.class);
        double damage = config.getConfig("tremorDamage", 5.0, Double.class);

        ability.setCooldown(cooldown);
        ability.setDamage(damage);
    }
}
