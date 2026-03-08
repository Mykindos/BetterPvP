package me.mykindos.betterpvp.core.item.component.impl.purity;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.runes.RuneContainerComponent;
import me.mykindos.betterpvp.core.item.component.impl.stat.StatContainerComponent;
import me.mykindos.betterpvp.core.item.purity.distribution.PurityDistributionRegistry;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.Listener;

/**
 * Automatically assigns purity to items that support it during creation.
 * Items with RuneContainerComponent or StatContainerComponent automatically receive purity.
 */
@Singleton
@BPvPListener
@CustomLog
public class PurityAssignmentBuilder implements Listener {

    @Inject
    public PurityAssignmentBuilder(ItemFactory itemFactory, PurityDistributionRegistry distributionRegistry) {
        // Register as a default builder that runs on all item creation
        itemFactory.registerDefaultBuilder(instance -> {
            // Only apply purity to items that should have it
            if (shouldApplyPurity(instance)) {
                // Roll for purity using the default distribution
                // For custom item distributions, a selector could be implemented here
                ItemPurity purity = distributionRegistry.getDefaultDistribution().roll();

                // Add purity component to the item
                return instance.withComponent(new PurityComponent(purity));
            }

            return instance;
        });

        log.info("PurityAssignmentBuilder registered").submit();
    }

    /**
     * Determines if purity should be applied to this item.
     * Currently applies to items with RuneContainerComponent or StatContainerComponent.
     *
     * @param instance The item instance to check
     * @return true if purity should be applied
     */
    private boolean shouldApplyPurity(ItemInstance instance) {
        // Check if item already has purity (avoid overwriting)
        if (instance.getComponent(PurityComponent.class).isPresent()) {
            return false;
        }

        // Apply to items with rune sockets
        if (instance.getComponent(RuneContainerComponent.class).isPresent()) {
            return true;
        }

        // Apply to items with stats
        if (instance.getComponent(StatContainerComponent.class).isPresent()) {
            return true;
        }

        // Can be extended to check other criteria
        return false;
    }
}
