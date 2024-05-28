package me.mykindos.betterpvp.core.combat.combatlog;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.events.ClientQuitEvent;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.combatlog.events.PlayerCombatLogEvent;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.inventory.EquipmentSlot;

import java.util.List;

@Singleton
@BPvPListener
public class CombatLogListener implements Listener {

    private final CombatLogManager combatLogManager;
    private final ClientManager clientManager;

    @Inject
    @Config(path = "combatlog.valuable-items", defaultValue = "TNT,MUSIC_DISC_PIGSTEP,MUSIC_DISC_WAIT,MUSIC_DISC_13")
    private List<String> valuableItems;

    @Inject
    public CombatLogListener(CombatLogManager combatLogManager, ClientManager clientManager) {
        this.combatLogManager = combatLogManager;
        this.clientManager = clientManager;
    }

    @EventHandler
    public void onClientQuit(ClientQuitEvent event) {
        PlayerCombatLogEvent combatLogEvent = UtilServer.callEvent(new PlayerCombatLogEvent(event.getClient(), event.getPlayer()));

        event.setQuitMessage(event.getQuitMessage().append(UtilMessage.deserialize(" <gray>(" + (combatLogEvent.isSafe() ? "<green>Safe" : "<red>Unsafe") + "<gray>)")));
        if (!combatLogEvent.isSafe()) {
            combatLogManager.createCombatLog(event.getPlayer(), System.currentTimeMillis() + combatLogEvent.getDuration());
        }
    }

    // Always make sure admins or people in creative / spectator are safe logged
    @EventHandler(priority = EventPriority.MONITOR)
    public void onCombatLog(PlayerCombatLogEvent event) {
        if (event.getPlayer().getGameMode() == GameMode.CREATIVE || event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            event.setSafe(true);
            return;
        }

        if (event.getClient().hasRank(Rank.ADMIN)) {
            event.setSafe(true);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCombatLogInCombat(PlayerCombatLogEvent event) {
        Gamer gamer = event.getClient().getGamer();
        if (!UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
            event.setSafe(false);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCombatLogWithValuables(PlayerCombatLogEvent event) {
        for (String item : valuableItems) {
            Material material = Material.valueOf(item);
            if (event.getPlayer().getInventory().contains(material)) {
                event.setSafe(false);
                event.setDuration(System.currentTimeMillis());
                return;
            }
        }
    }

    @EventHandler (ignoreCancelled = true)
    public void onCombatLogDamage(CustomDamageEvent event) {
        if(event.getDamagee().getType() != EntityType.SHEEP) return;
        combatLogManager.getCombatLogBySheep(event.getDamagee()).ifPresent(combatLog -> {
            event.cancel("Combat log sheep cannot be damaged");
        });
    }

    @EventHandler
    public void onInteractCombatLog(PlayerInteractEntityEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) return;
        if (event.getRightClicked() instanceof LivingEntity entity) {
            combatLogManager.getCombatLogBySheep(entity).ifPresent(combatLog -> {
                combatLog.onClicked(event.getPlayer());
                combatLogManager.removeObject(combatLog.getOwner().toString());
            });
        }
    }

    @UpdateEvent(delay = 500)
    public void onCombatLogExpire() {
        combatLogManager.getObjects().entrySet().removeIf(combatLog -> {
            if (combatLog.getValue().hasExpired()) {
                combatLog.getValue().getCombatLogSheep().remove();
                return true;
            }
            return false;
        });
    }

    @EventHandler
    public void onLoggerReturn(PlayerLoginEvent event) {
        combatLogManager.getObject(event.getPlayer().getUniqueId()).ifPresent(combatLog -> {
            combatLog.getCombatLogSheep().remove();
        });

        combatLogManager.removeObject(event.getPlayer().getUniqueId().toString());
    }

}
