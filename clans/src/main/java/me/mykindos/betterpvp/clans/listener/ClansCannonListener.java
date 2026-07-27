package me.mykindos.betterpvp.clans.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.framework.adapter.PluginAdapter;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonAimEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonFuseEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonPlaceEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonReloadEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.event.PreCannonPlaceEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.event.CannonBoardEvent;
import me.mykindos.betterpvp.core.item.impl.cannon.model.CannonProp;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.Optional;

@BPvPListener
@Singleton
@PluginAdapter("ModelEngine")
public class ClansCannonListener implements Listener {

    /**
     * Cannon tag holding the name of the owning clan. Stored on the cannon rather than on its backing entity, whose
     * PDC is discarded every time the scene framework rebuilds the body.
     */
    private static final String CLAN_TAG = "clans:owner";

    @Inject
    private ClanManager clanManager;

    private boolean canUse(final Player player, final CannonProp cannon) {
        final String clanName = cannon.getTag(CLAN_TAG);
        if (clanName == null) {
            return true; // Non-tagged cannons are free to use
        }

        final Optional<Clan> clanOpt = clanManager.getClanByName(clanName);
        if (clanOpt.isEmpty()) {
            UtilMessage.message(player, "core.prefix.clans", "clans.cannon.not-owned");
            return false;
        } else {
            final Clan clan = clanOpt.get();
            final Clan otherClan = clanManager.getClanByPlayer(player).orElse(null);
            if (clan != otherClan) {
                UtilMessage.message(player, "core.prefix.clans", "clans.cannon.owned-by", Component.text(clan.getName(), NamedTextColor.YELLOW));
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
        if(event.getPlayer().getWorld().getName().equalsIgnoreCase(BPvPWorld.BOSS_WORLD_NAME)) {
            event.cancel("Cannot place cannons here.");
            UtilMessage.message(event.getPlayer(), "core.prefix.clans", "clans.cannon.place-here-denied");
            return;
        }

        final Optional<Clan> clanByPlayer = clanManager.getClanByPlayer(event.getPlayer());
        if (clanByPlayer.isEmpty()) {
            event.cancel("No Clan");
            UtilMessage.message(event.getPlayer(), "core.prefix.clans", "clans.cannon.must-be-in-clan",
                    Component.text("Clan", NamedTextColor.YELLOW), Component.text("cannon", NamedTextColor.YELLOW));
            return;
        }

        final Optional<Clan> clanByLocation = clanManager.getClanByLocation(event.getCannonLocation());
        if (clanByLocation.isEmpty()) {
            return; // They're placing in wilderness
        }

        if (clanByPlayer.get() != clanByLocation.get()) {
            event.cancel("Not Your Clan");
            UtilMessage.message(event.getPlayer(), "core.prefix.clans", "clans.cannon.place-other-territory-denied");
        }
    }

    @EventHandler
    public void onPlace(CannonPlaceEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        final Clan clan = clanManager.getClanByPlayer(event.getPlayer()).orElseThrow();
        event.getCannon().setTag(CLAN_TAG, clan.getName());
    }

    @EventHandler
    public void onBoard(CannonBoardEvent event) {
        if (!canUse(event.getPlayer(), event.getCannon())) {
            event.cancel("Not Your Clan");
        }
    }

}
