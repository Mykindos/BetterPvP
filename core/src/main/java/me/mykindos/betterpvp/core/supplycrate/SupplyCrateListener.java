package me.mykindos.betterpvp.core.supplycrate;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.ticxo.modelengine.api.events.BaseEntityInteractEvent;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Sound;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;

import java.util.Iterator;
import java.util.Optional;

@BPvPListener
@Singleton
@PluginAdapter("ModelEngine")
public class SupplyCrateListener implements Listener {

    private final SupplyCrateController controller;
    private final Core core;

    @Inject
    public SupplyCrateListener(SupplyCrateController supplyCrateController, Core core) {
        this.controller = supplyCrateController;
        this.core = core;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onRightClick(BaseEntityInteractEvent event) {
        if (event.isSecondary() || event.getSlot() != EquipmentSlot.HAND || event.getAction() != BaseEntityInteractEvent.Action.INTERACT) {
            return;
        }

        final Optional<SupplyCrate> supplyCrateOptional = controller.getSupplyCrates().stream()
                .filter(stored -> stored.getBackingEntity() == event.getBaseEntity())
                .findFirst();
        if (supplyCrateOptional.isEmpty()) {
            return; // Not a supply crate
        }

        final SupplyCrate supplyCrate = supplyCrateOptional.get();
        if (!supplyCrate.isImpacted() || supplyCrate.isMarkForRemoval()) {
            return; // Hasn't hit the ground or is being removed
        }

        if (!tryAwardLoot(supplyCrate)) {
            UtilServer.runTaskLater(core, () -> {
                if (supplyCrate.isMarkForRemoval()) {
                    return;
                }
                supplyCrate.setMarkForRemoval(true);
            }, 20L);
        }
    }

    @UpdateEvent
    public void onTick() {
        final Iterator<SupplyCrate> iterator = this.controller.getSupplyCrates().iterator();
        while (iterator.hasNext()) {
            SupplyCrate supplyCrate = iterator.next();

            // Forcefully removed
            if (supplyCrate.isMarkForRemoval()) {
                supplyCrate.remove();
                iterator.remove();
                continue;
            }

            // Needs to explode with remaining items
            if (supplyCrate.isExpired()) {
                supplyCrate.remove();
                iterator.remove();
                while (supplyCrate.hasLoot()) {
                    tryAwardLoot(supplyCrate);
                }
                continue;
            }

            supplyCrate.tick();
        }
    }

    private boolean tryAwardLoot(SupplyCrate supplyCrate) {
        if (!supplyCrate.hasLoot()) {
            return false;
        }

        supplyCrate.consumeLoot().award(supplyCrate.getLootContext());
        new SoundEffect(Sound.BLOCK_BEEHIVE_EXIT, 0.7f, 1f).play(supplyCrate.getLocation());
        return true;
    }

}
