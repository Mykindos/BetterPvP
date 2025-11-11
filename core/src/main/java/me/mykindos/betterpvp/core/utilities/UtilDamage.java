package me.mykindos.betterpvp.core.utilities;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.listeners.DamageEventProcessor;
import org.bukkit.plugin.java.JavaPlugin;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilDamage {

    /**
     * Processes a damage event through the new unified system
     * @param event the damage event to process
     * @return the processed damage event
     */
    public static DamageEvent doDamage(DamageEvent event) {
        final DamageEventProcessor processor = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(DamageEventProcessor.class);
        processor.processDamageEvent(event);
        return event;
    }
}
