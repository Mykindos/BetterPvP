package me.mykindos.betterpvp.champions.item.bloomrot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import org.bukkit.Bukkit;

import java.util.List;

@Singleton
@EqualsAndHashCode(callSuper = true)
@ItemKey("champions:bloomrot")
public class Bloomrot extends WeaponItem implements Reloadable {

    private final ItemFactory itemFactory;
    private final NectarOfDecay nectarOfDecayAbility;

    @Inject
    private Bloomrot(Champions champions, NectarOfDecay nectarOfDecay, ItemFactory itemFactory) {
        super(champions, "Bloomroot", Item.model("bloomrot"), ItemRarity.EPIC, List.of(Group.RANGED));
        this.itemFactory = itemFactory;

        this.nectarOfDecayAbility = nectarOfDecay;
        Bukkit.getPluginManager().registerEvents(nectarOfDecayAbility, champions);

        // Add abilities to container
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.RIGHT_CLICK, nectarOfDecayAbility)
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