package me.mykindos.betterpvp.clans.weapons.impl.cannon.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.event.CannonAimEvent;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.event.CannonFuseEvent;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.event.CannonPlaceEvent;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.event.CannonReloadEvent;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.event.PreCannonPlaceEvent;
import me.mykindos.betterpvp.clans.weapons.impl.cannon.model.Cannon;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.IronGolem;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.Optional;

@BPvPListener
@Singleton
@PluginAdapter("ModelEngine")
public class ClansCannonListener implements Listener {

    @Inject
    private ClanManager clanManager;

    private boolean canUse(final Player player, final Cannon cannon) {
        final IronGolem backingEntity = cannon.getBackingEntity();
        final PersistentDataContainer pdc = backingEntity.getPersistentDataContainer();
        if (!pdc.has(ClansNamespacedKeys.CANNON_CLAN, PersistentDataType.STRING)) {
            return true; // Non-tagged cannons are free to use
        }

        final String clanName = pdc.getOrDefault(ClansNamespacedKeys.CANNON_CLAN, PersistentDataType.STRING, "");
        final Optional<Clan> clanOpt = clanManager.getClanByName(clanName);
        if (clanOpt.isEmpty()) {
            UtilMessage.message(player, "Clans", "This cannon is not owned by a clan.");
            return false;
        } else {
            final Clan clan = clanOpt.get();
            final Clan otherClan = clanManager.getClanByPlayer(player).orElse(null);
            if (clan != otherClan) {
                UtilMessage.message(player, "Clans", "This cannon is owned by <alt2>%s</alt2>.", clan.getName());
                return false;
            }
        }
        return true;
    }

    @EventHandler
    public void onFuse(CannonFuseEvent event) {
        if (!canUse(event.getPlayer(), event.getCannon())) {
            event.cancel("Not Your Clan");
        }
    }

    @EventHandler
    public void onReload(CannonReloadEvent event) {
        if (!canUse(event.getPlayer(), event.getCannon())) {
            event.cancel("Not Your Clan");
        }
    }

    @EventHandler
    public void onAim(CannonAimEvent event) {
        if (!canUse(event.getPlayer(), event.getCannon())) {
            event.cancel("Not Your Clan");
        }
    }

    @EventHandler
    public void onPrePlace(PreCannonPlaceEvent event) {
        final Optional<Clan> clanByPlayer = clanManager.getClanByPlayer(event.getPlayer());
        if (clanByPlayer.isEmpty()) {
            event.cancel("No Clan");
            UtilMessage.message(event.getPlayer(), "Clans", "You must be in a <alt2>Clan</alt2> to place a <alt2>cannon</alt2>.");
            return;
        }

        final Optional<Clan> clanByLocation = clanManager.getClanByLocation(event.getCannonLocation());
        if (clanByLocation.isEmpty()) {
            return; // They're placing in wilderness
        }

        if (clanByPlayer.get() != clanByLocation.get()) {
            event.cancel("Not Your Clan");
            UtilMessage.message(event.getPlayer(), "Clans", "You cannot place a cannon in another clan's territory.");
        }
    }

    @EventHandler
    public void onPlace(CannonPlaceEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        final IronGolem backingEntity = event.getCannon().getBackingEntity();
        final Clan clan = clanManager.getClanByPlayer(event.getPlayer()).orElseThrow();
        backingEntity.getPersistentDataContainer().set(ClansNamespacedKeys.CANNON_CLAN, PersistentDataType.STRING, clan.getName());
    }

}
