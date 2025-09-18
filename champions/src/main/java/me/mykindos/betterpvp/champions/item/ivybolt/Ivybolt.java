package me.mykindos.betterpvp.champions.item.ivybolt;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.item.bloomrot.NectarOfDecay;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.impl.ability.EndlessQuiverAbility;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.recipe.crafting.CraftingRecipeRegistry;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInputEvent;

@Singleton
@EqualsAndHashCode(callSuper = true)
public class Ivybolt extends WeaponItem implements ReloadHook {

    private final ItemFactory itemFactory;
    private final CooldownManager cooldownManager;
    private final EndlessQuiverAbility endlessQuiverAbility;

    @Inject
    private Ivybolt(Champions champions, EndlessQuiverAbility endlessQuiverAbility, ItemFactory itemFactory, CooldownManager cooldownManager) {
        super(champions, "Ivybolt", Item.model(Material.BOW, "ivybolt"), ItemRarity.LEGENDARY);
        this.itemFactory = itemFactory;
        this.endlessQuiverAbility = endlessQuiverAbility;
        this.cooldownManager = cooldownManager;

        // Add abilities to container
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(endlessQuiverAbility)
                .build());
    }

    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(Champions.class, this);
        final Double reloadCooldown = config.getConfig("cooldown", null, Double.class);
        if (reloadCooldown != null) {
            this.endlessQuiverAbility.setUseFunction(livingEntity -> {
                if (livingEntity instanceof Player player) {
                    cooldownManager.use(player,
                            "Ivybolt",
                            reloadCooldown,
                            true,
                            true,
                            false,
                            (BaseItem) null);
                }
            });

            this.endlessQuiverAbility.setUseCheck(livingEntity -> {
                return !(livingEntity instanceof Player player) || !cooldownManager.hasCooldown(player, "Ivybolt");
            });
        } else {
            this.endlessQuiverAbility.setUseFunction(null);
            this.endlessQuiverAbility.setUseCheck(null);
        }
    }
} 