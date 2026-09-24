package me.mykindos.betterpvp.clans.world.camp.structure;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.world.camp.Camps;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.world.construction.StructurePieceUseEvent;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/** A member right-clicking a fitted upgrade's piece opens that upgrade's page. */
@BPvPListener
@Singleton
public class UpgradePieces implements Listener {

    private final Camps camps;
    private final CampUpgrades upgrades;

    @Inject
    public UpgradePieces(@NotNull Camps camps, @NotNull CampUpgrades upgrades) {
        this.camps = camps;
        this.upgrades = upgrades;
    }

    @EventHandler
    public void onUse(@NotNull StructurePieceUseEvent event) {
        if (!event.getSite().getSiteId().equals(Camps.SITE_ID)
                || !event.getStructure().hasUpgrade(event.getUpgrade().getId())
                || !camps.isMember(event.getPlayer(), event.getPlayer().getWorld())) {
            return;
        }
        upgrades.page(event.getUpgrade().getId()).ifPresent(page -> {
            event.setHandled(true);
            page.open(event.getPlayer(), event.getSite(), event.getStructure(), null);
        });
    }
}
