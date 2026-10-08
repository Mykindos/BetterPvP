package me.mykindos.betterpvp.clans.world.camp.structure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.construction.StructurePieceUseEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * Right-clicking a fitted upgrade's piece opens that upgrade's page, unless the upgrade already handled the
 * click itself.
 */
@BPvPListener
@Singleton
public class UpgradePieces implements Listener {

    private final CampUpgrades upgrades;

    @Inject
    public UpgradePieces(@NotNull CampUpgrades upgrades) {
        this.upgrades = upgrades;
    }

    @EventHandler
    public void onUse(@NotNull StructurePieceUseEvent event) {
        if (event.isHandled() || !event.getSite().getSiteId().equals(Camps.SITE_ID)
                || !event.getStructure().hasUpgrade(event.getUpgrade().getId())) {
            return;
        }
        upgrades.page(event.getUpgrade().getId()).ifPresent(page -> {
            event.setHandled(true);
            page.open(event.getPlayer(), event.getSite(), event.getStructure(), null);
        });
    }
}
