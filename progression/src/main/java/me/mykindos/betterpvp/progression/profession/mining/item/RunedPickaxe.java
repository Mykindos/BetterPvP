package me.mykindos.betterpvp.progression.profession.mining.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.progression.Progression;
import me.mykindos.betterpvp.progression.profession.mining.item.interaction.InstantMineInteraction;
import org.bukkit.Material;

@Singleton
@EqualsAndHashCode(callSuper = true)
@ItemKey("progression:runed_pickaxe")
public class RunedPickaxe extends BaseItem implements Reloadable {

    private final InstantMineInteraction instantMineInteraction;

    @EqualsAndHashCode.Exclude
    private final Progression progression;

    @Inject
    private RunedPickaxe(Progression progression,
                         CooldownManager cooldownManager,
                         ClientManager clientManager) {
        super("Runed Pickaxe",
                Item.model(Material.DIAMOND_PICKAXE, "runed_pickaxe"),
                ItemGroup.TOOL,
                ItemRarity.EPIC);
        this.progression = progression;
        this.instantMineInteraction = new InstantMineInteraction(cooldownManager, clientManager, 20.0, 7.0);

        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.RIGHT_CLICK, instantMineInteraction)
                .build());
    }

    @Override
    public void reload() {
        final Config instantMineConfig = Config.item(progression, this).fork("instantMine");
        instantMineInteraction.setCooldown(instantMineConfig.getConfig("cooldown", 20.0, Double.class));
        instantMineInteraction.setDuration(instantMineConfig.getConfig("duration", 7.0, Double.class));
    }
}
