package me.mykindos.betterpvp.champions.champions.roles.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.papermc.paper.event.entity.EntityEquipmentChangedEvent;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.champions.roles.events.RoleChangeCause;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;

/**
 * Keeps {@link RoleManager} in step with what entities are actually wearing.
 */
@Singleton
@BPvPListener
public class RoleArmorListener implements Listener {

    private final Champions champions;
    private final RoleManager roleManager;

    @Inject
    private RoleArmorListener(Champions champions, RoleManager roleManager) {
        this.champions = champions;
        this.roleManager = roleManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onEquipmentChanged(EntityEquipmentChangedEvent event) {
        if (event.getEquipmentChanges().keySet().stream().noneMatch(EquipmentSlot::isArmor)) {
            return; // Held items can't carry a role
        }

        final LivingEntity entity = event.getEntity();
        // A tick later so we read the equipment after every handler of this change has had its say
        UtilServer.runTask(champions, () -> {
            if (entity.isValid()) {
                roleManager.refreshRole(entity, RoleChangeCause.ARMOR);
            }
        });
    }

}
