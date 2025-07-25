package me.mykindos.betterpvp.core.loot;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.lumine.mythic.bukkit.events.MythicMobInteractEvent;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

@Singleton
@BPvPListener
@PluginAdapter("MythicMobs")
public class LootChestListener implements Listener {

    private final Core core;
    private final LootChestManager lootChestManager;
    private final ClientManager clientManager;

    @Inject
    public LootChestListener(Core core, LootChestManager lootChestManager, ClientManager clientManager) {
        this.core = core;
        this.lootChestManager = lootChestManager;
        this.clientManager = clientManager;
    }

    @EventHandler
    public void onChestInteract(MythicMobInteractEvent event) {
        if (event.isCancelled()) return;

        LootChest lootChest = lootChestManager.getLootChest(event.getActiveMob().getEntity().getBukkitEntity());
        if (lootChest != null) {
            lootChest.onOpen(core, event.getPlayer(), clientManager);
            lootChestManager.getLootChests().remove(lootChest);
        }
    }

    @UpdateEvent (delay = 1000)
    public void removeInvalidLootChests() {
        lootChestManager.getLootChests().removeIf(lootChest -> lootChest.getEntity() == null || lootChest.getEntity().isDead());
    }
}
