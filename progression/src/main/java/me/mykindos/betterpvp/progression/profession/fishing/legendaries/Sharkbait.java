package me.mykindos.betterpvp.progression.profession.fishing.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.TooltipSpriteComponent;
import me.mykindos.betterpvp.core.item.component.impl.durability.DurabilityComponent;
import me.mykindos.betterpvp.core.item.component.impl.socketables.SocketableContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;
import me.mykindos.betterpvp.progression.Progression;
import org.bukkit.Material;

@Singleton
@BPvPListener
@CustomLog
@EqualsAndHashCode(callSuper = true)
@Getter
@ItemKey("core:sharkbait")
public class Sharkbait extends BaseItem implements Reloadable {
    
    private final SharkbaitAuraAbility auraAbility;
    private final Progression progression;

    @Inject
    public Sharkbait(Progression progression, 
                     ItemFactory itemFactory,
                     SharkbaitAuraAbility auraAbility) {
        super(translatableName("core.item.sharkbait.name"), Item.model(Material.FISHING_ROD, "sharkbait", 1), ItemGroup.TOOL, ItemRarity.LEGENDARY);
        this.progression = progression;
        this.auraAbility = auraAbility;
        
        // Add ability container
        addSerializableComponent(new DurabilityComponent(2000));
        addSerializableComponent(new SocketableContainerComponent(0, 0));
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.PASSIVE, auraAbility)
                .build());
        addBaseComponent(TooltipSpriteComponent.of("\uE00F"));
    }

    @Override
    public void reload() {
        Config config = Config.item(Progression.class, this);

        double catchSpeedMultiplier = config.getConfig("catchSpeedMultiplier", 0.7, Double.class);
        double radius = config.getConfig("radius", 6.0, Double.class);

        auraAbility.setCatchSpeedMultiplier(catchSpeedMultiplier);
        auraAbility.setRadius(radius);

        final int durability = config.getConfig("durability", 2000, Integer.class);
        getComponent(DurabilityComponent.class).ifPresent(durabilityComponent -> {
            durabilityComponent.setMaxDamage(durability);
        });
    }
}
