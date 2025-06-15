package me.mykindos.betterpvp.progression.profession.fishing.legendaries;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.ItemConfig;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.model.ReloadHook;
import me.mykindos.betterpvp.core.utilities.model.item.ItemView;
import me.mykindos.betterpvp.progression.Progression;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@BPvPListener
@CustomLog
@EqualsAndHashCode(callSuper = true)
@Getter
public class Sharkbait extends BaseItem implements ReloadHook {

    private static final ItemStack model;
    
    private final SharkbaitAuraAbility auraAbility;
    private final ItemFactory itemFactory;
    private final Progression progression;

    static {
        model = ItemView.builder()
                .material(Material.FISHING_ROD)
                .customModelData(1)
                .build()
                .get();
    }

    @Inject
    public Sharkbait(Progression progression, 
                     ItemFactory itemFactory,
                     SharkbaitAuraAbility auraAbility) {
        super("Sharkbait", model, ItemGroup.TOOL, ItemRarity.LEGENDARY);
        this.progression = progression;
        this.auraAbility = auraAbility;
        this.itemFactory = itemFactory;
        
        // Add ability container
        addBaseComponent(AbilityContainerComponent.builder()
                .ability(auraAbility)
                .build());
    }

    @Override
    public void reload() {
        ItemConfig config = ItemConfig.of(Progression.class, this);

        double catchSpeedMultiplier = config.getConfig("catchSpeedMultiplier", 0.7, Double.class);
        double radius = config.getConfig("radius", 6.0, Double.class);

        auraAbility.setCatchSpeedMultiplier(catchSpeedMultiplier);
        auraAbility.setRadius(radius);
    }
}
