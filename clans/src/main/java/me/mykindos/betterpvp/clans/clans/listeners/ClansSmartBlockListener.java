package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.core.block.SmartBlockInstance;
import me.mykindos.betterpvp.core.block.SmartBlockInteractEvent;
import me.mykindos.betterpvp.core.block.custom.FurnitureBlock;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;

@Singleton
@BPvPListener
public class ClansSmartBlockListener implements Listener {

    private final ClientManager clientManager;
    private final ClanManager clanManager;

    @Inject
    public ClansSmartBlockListener(ClientManager clientManager, ClanManager clanManager) {
        this.clientManager = clientManager;
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    void onInteract(SmartBlockInteractEvent event) {
        if (event.getInstance().getType() instanceof FurnitureBlock) {
            handleFurnitureEvent(event.getPlayer(), Optional.of(event.getInstance()), () -> event.setCancelled(true));
        }
    }

    private void handleFurnitureEvent(Player player, Optional<SmartBlockInstance> from, Runnable cancel) {
        if (from.isEmpty()) {
            return;
        }

        Client client = this.clientManager.search().online(player);
        if (client.isAdministrating()) {
            return;
        }

        SmartBlockInstance smartBlockInstance = from.get();

        final Clan clan = this.clanManager.getClanByPlayer(player).orElse(null);
        final Optional<Clan> locationClanOptional = this.clanManager.getClanByLocation(smartBlockInstance.getLocation());
        locationClanOptional.ifPresent(locationClan -> {
            if (locationClan == clan) {
                return;
            }

            // Foreign furniture is protected unconditionally; pillaging a dead-core enemy is the only exception.
            if (this.clanManager.getPillageHandler().isPillaging(clan, locationClan) && locationClan.getCore().isDead()) {
                return;
            }

            cancel.run();

            final ClanRelation relation = this.clanManager.getRelation(clan, locationClan);
            UtilMessage.message(player, "clans.prefix", "clans.world.use.denied",
                    Component.text(UtilFormat.cleanString(smartBlockInstance.getType().getName()), NamedTextColor.GREEN),
                    Component.text("Clan " + locationClan.getName(), relation.getPrimary()));
        });
    }
}
