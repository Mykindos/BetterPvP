package me.mykindos.betterpvp.core.items.listener;

import com.google.inject.Inject;
import me.mykindos.betterpvp.core.combat.events.KillContributionEvent;
import me.mykindos.betterpvp.core.combat.stats.model.Contribution;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.items.logger.UUIDItem;
import me.mykindos.betterpvp.core.items.logger.UUIDManager;
import me.mykindos.betterpvp.core.items.logger.UuidLogger;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDropItemEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@BPvPListener
public class UuidListener implements Listener {

    @Inject
    UUIDManager uuidManager;

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDPickup(EntityPickupItemEvent event) {
        Optional<UUIDItem> uuidItemOptional = getUUIDItem(event.getItem().getItemStack());
        if (uuidItemOptional.isEmpty()) {
            return;
        }
        UUIDItem uuidItem = uuidItemOptional.get();
        int logID = UuidLogger.logID("%s picked up %s at %s", uuidItem.getUuid(), event.getEntity().getName(), event.getEntity().getLocation());
        if (event.getEntity() instanceof Player player) {
            UuidLogger.AddUUIDMetaInfo(logID, uuidItem.getUuid(), UuidLogger.UuidLogType.PICKUP, player.getUniqueId());
        } else {
            UuidLogger.AddUUIDMetaInfo(logID, uuidItem.getUuid(), UuidLogger.UuidLogType.PICKUP, null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDDrop(EntityDropItemEvent event) {
        Optional<UUIDItem> uuidItemOptional = getUUIDItem(event.getItemDrop().getItemStack());
        if (uuidItemOptional.isEmpty()) {
            return;
        }
        UUIDItem uuidItem = uuidItemOptional.get();
        int logID = UuidLogger.logID("%s dropped %s at %s", uuidItem.getUuid(), event.getEntity().getName(), event.getEntity().getLocation());
        if (event.getEntity() instanceof Player player) {
            UuidLogger.AddUUIDMetaInfo(logID, uuidItem.getUuid(), UuidLogger.UuidLogType.PICKUP, player.getUniqueId());
        } else {
            UuidLogger.AddUUIDMetaInfo(logID, uuidItem.getUuid(), UuidLogger.UuidLogType.PICKUP, null);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUUIDItemUserKilled(KillContributionEvent event) {
        final Player victim = event.getVictim();
        List<UUIDItem> uuidItemsList = getUUIDItems(victim);
        if (uuidItemsList.isEmpty()) return;
        final Player killer = event.getKiller();
        final Map<Player, Contribution> contributions = event.getContributions();


        StringBuilder contributors = new StringBuilder();
        contributors.append("");
        for (Player player : contributions.keySet()) {
            contributors.append(player.getName()).append(", ");
        }
        for (UUIDItem item : uuidItemsList) {
            int logID = UuidLogger.logID("%s was killed while using %s by %s at %s, contributed by %s", victim.getName(), killer.getName(), victim.getLocation().toString(), contributors);
            if (logID < 0) {
                continue;
            }
            UuidLogger.AddUUIDMetaInfo(logID, item.getUuid(), UuidLogger.UuidLogType.DEATH, victim.getUniqueId());
            UuidLogger.AddUUIDMetaInfo(logID, item.getUuid(), UuidLogger.UuidLogType.KILL, killer.getUniqueId());
            for (Player player : contributions.keySet()) {
                UuidLogger.AddUUIDMetaInfo(logID, item.getUuid(), UuidLogger.UuidLogType.CONTRIBUTOR, player.getUniqueId());
            }
        }


    }

    private List<UUIDItem> getUUIDItems(Player player) {
        List<UUIDItem> uuidItemList = new ArrayList<>();
        player.getInventory().forEach(itemStack -> {
           Optional<UUIDItem> uuidItemOptional = getUUIDItem(itemStack);
           if (uuidItemOptional.isPresent()) {
                uuidItemList.add(getUUIDItem(itemStack).orElseThrow());
            }
        });
        return uuidItemList;
    }

    private Optional<UUIDItem> getUUIDItem(ItemStack itemStack) {
        PersistentDataContainer pdc = itemStack.getItemMeta().getPersistentDataContainer();
        if (pdc.has(CoreNamespaceKeys.UUID_KEY)) {
            return uuidManager.getObject(UUID.fromString(Objects.requireNonNull(pdc.get(CoreNamespaceKeys.UUID_KEY, PersistentDataType.STRING))));
        }
        return Optional.empty();
    }
}


