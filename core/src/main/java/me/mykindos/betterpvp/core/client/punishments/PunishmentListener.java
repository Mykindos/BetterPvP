package me.mykindos.betterpvp.core.client.punishments;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.Optional;

@BPvPListener
@Singleton
public class PunishmentListener implements Listener {

    private final ClientManager clientManager;

    private final long CHECKDELAY = 10_000;

    private final PrettyTime prettyTime = new PrettyTime();

    @Inject
    public PunishmentListener(ClientManager clientManager) {
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLogin(PlayerLoginEvent event) {
        final Client client = clientManager.search().online(event.getPlayer());

        Optional<Punishment> ban = client.getPunishment(PunishmentTypes.BAN);
        if (ban.isPresent()) {
            Punishment punishment = ban.get();

            Component banMessage = Component.text("You are banned from the server!", NamedTextColor.RED).append(Component.newline())
                    .append(Component.text("Reason: ", NamedTextColor.YELLOW).append(Component.text(punishment.getReason(), NamedTextColor.WHITE))).appendNewline().appendNewline();

            if (punishment.getExpiryTime() == -1) {
                banMessage = banMessage.append(Component.text("This ban is permanent.", NamedTextColor.RED));
            } else {
                banMessage = banMessage.append(Component.text("This ban will expire ", NamedTextColor.RED).append(Component.text(new PrettyTime().format(new Date(punishment.getExpiryTime())), NamedTextColor.GREEN)));
            }

            event.disallow(PlayerLoginEvent.Result.KICK_BANNED, banMessage);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(ChatSentEvent event) {
        if (event.isCancelled()) return;
        final Client client = clientManager.search().online(event.getPlayer());

        client.getPunishment(PunishmentTypes.MUTE).ifPresent(mute -> {
            UtilMessage.simpleMessage(event.getPlayer(), "Punish", "You are currently muted and cannot send messages!");
            UtilMessage.message(event.getPlayer(), "Punish", mute.getInformation());
            event.cancel("Player is muted");
        });
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getDamagee() instanceof Player)) return;

        final Client client = clientManager.search().online(damager);

        client.getPunishment(PunishmentTypes.PVP_LOCK).ifPresent(pvpLock -> {
            UtilMessage.simpleMessage(damager, "Punish", "You are currently PvP Locked and cannot deal damage to other players!");
            UtilMessage.message(damager, "Punish", pvpLock.getInformation());
            event.setCancelled(true);
        });

    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        final Client client = clientManager.search().online(event.getPlayer());

        client.getPunishment(PunishmentTypes.BUILD_LOCK).ifPresent(buildLock -> {
            UtilMessage.simpleMessage(event.getPlayer(), "Punish", "You are currently Build Locked and cannot place blocks!");
            UtilMessage.message(event.getPlayer(), "Punish", buildLock.getInformation());
            event.setCancelled(true);
        });
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        final Client client = clientManager.search().online(event.getPlayer());

        client.getPunishment(PunishmentTypes.BUILD_LOCK).ifPresent(buildLock -> {
            UtilMessage.simpleMessage(event.getPlayer(), "Punish", "You are currently Build Locked and cannot place blocks!");
            UtilMessage.message(event.getPlayer(), "Punish", buildLock.getInformation());
            event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCanHurt(EntityCanHurtEntityEvent event) {
        if(!event.isAllowed()) return;

        if (event.getDamager() instanceof Player damager) {
            final Client client = clientManager.search().online(damager);

            Optional<Punishment> pvpLock = client.getPunishment(PunishmentTypes.PVP_LOCK);
            if (pvpLock.isPresent()) {
                event.setResult(Event.Result.DENY);
            }
        }
    }

    @UpdateEvent(delay = CHECKDELAY, isAsync = true)
    public void onExpiration() {
        long timeStart = System.currentTimeMillis();
        clientManager.getOnline().forEach(client -> {
            client.getPunishments().forEach(punishment -> {
                if (punishment.isRevoked()) return;
                if (punishment.hasExpired() && timeStart - punishment.getExpiryTime() <= CHECKDELAY) {
                    punishment.getType().onExpire(client, punishment);
                }
            });
        });
    }

}
