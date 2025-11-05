package me.mykindos.betterpvp.progression.profession.fishing.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import me.mykindos.betterpvp.progression.Progression;
import org.bukkit.Material;

@Singleton
@BPvPListener
@CustomLog
@EqualsAndHashCode(callSuper = true)
@Getter
public class Sharkbait extends BaseItem implements ReloadHook {
    
    private final SharkbaitAuraAbility auraAbility;
    private final Progression progression;

    @Inject
    public Sharkbait(Progression progression, 
                     ItemFactory itemFactory,
                     SharkbaitAuraAbility auraAbility) {
        super("Sharkbait", Item.model(Material.FISHING_ROD, "sharkbait", 1), ItemGroup.TOOL, ItemRarity.LEGENDARY);
        this.progression = progression;
        this.auraAbility = auraAbility;
        
        // Add ability container
        addSerializableComponent(new RuneContainerComponent(3));
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(auraAbility)
                .build());
    }

    @Override
    public void reload() {
        Config config = Config.item(Progression.class, this);

        double catchSpeedMultiplier = config.getConfig("catchSpeedMultiplier", 0.7, Double.class);
        double radius = config.getConfig("radius", 6.0, Double.class);

        auraAbility.setCatchSpeedMultiplier(catchSpeedMultiplier);
        auraAbility.setRadius(radius);
    }
}
