package me.mykindos.betterpvp.clans.combat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.components.champions.events.PlayerCanUseSkillEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.Optional;

/**
 * Provide changes to certain skills from champions if loaded and necessary
 */
@Singleton
@BPvPListener
public class ClansSkillListener implements Listener {

    private final ClanManager clanManager;

    @Inject
    public ClansSkillListener(ClanManager clanManager) {
        this.clanManager = clanManager;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onChampionsSkill(PlayerUseSkillEvent event) {
        if (!clanManager.canCast(event.getPlayer())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onFetchNearbyEntity(FetchNearbyEntityEvent<?> event) {
        if (!(event.getSource() instanceof Player player)) return;
        event.getEntities().forEach(entity -> {
            if (!(entity.getKey() instanceof Player target)) return;
            boolean canHurt = clanManager.canHurt(player, target);

            entity.setValue(canHurt ? EntityProperty.ENEMY : EntityProperty.FRIENDLY);
        });

        event.getEntities().removeIf(entity -> {
            if (entity.getKey() instanceof Player target) {
                if (target.getGameMode() == GameMode.CREATIVE || target.getGameMode() == GameMode.SPECTATOR) {
                    return true;
                }

                if (event.getEntityProperty() != EntityProperty.ALL) {
                    return entity.getValue() != event.getEntityProperty();
                }
            }
            return false;
        });
    }

    @EventHandler
    public void disableLongshot(PlayerCanUseSkillEvent event) {
        if (!event.getSkill().getName().equals("Longshot")) return;
        Player player = event.getPlayer();

        Optional<Clan> playerClanOptional = clanManager.getClanByPlayer(player);
        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(player.getLocation());
        if (playerClanOptional.isPresent() && locationClanOptional.isPresent()) {
            Clan playerClan = playerClanOptional.get();
            Clan locationClan = locationClanOptional.get();

            if(playerClan.equals(locationClan) || playerClan.isAllied(locationClan)) {
                event.cancel("Cannot use Longshot in own or allied territory");
            }
        }
    }
}
