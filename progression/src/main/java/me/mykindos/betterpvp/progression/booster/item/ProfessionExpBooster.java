package me.mykindos.betterpvp.progression.booster.item;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.interaction.component.InteractionContainerComponent;
import me.mykindos.betterpvp.core.interaction.input.InteractionInputs;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemGroup;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.progression.booster.BoosterManager;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

@Singleton
@ItemKey("progression:profession_exp_booster")
public class ProfessionExpBooster extends BaseItem {

    @Inject
    public ProfessionExpBooster(BoosterManager boosterManager) {
        super("Profession Experience Booster", ItemStack.of(Material.EXPERIENCE_BOTTLE), ItemGroup.CONSUMABLE, ItemRarity.LEGENDARY);
        
        // 24 hours = 24 * 60 * 60 * 1000
        long durationMillis = 24L * 60L * 60L * 1000L;
        
        addBaseComponent(InteractionContainerComponent.builder()
                .root(InteractionInputs.RIGHT_CLICK, new BoosterInteraction(boosterManager, durationMillis))
                .build());
    }
}
