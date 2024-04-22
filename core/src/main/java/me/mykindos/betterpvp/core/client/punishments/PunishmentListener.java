package me.mykindos.betterpvp.core.client.punishments;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
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

        Optional<Punishment> mute = client.getPunishment(PunishmentTypes.MUTE);
        if (mute.isPresent()) {
            UtilMessage.simpleMessage(event.getPlayer(), "Punish", "You are currently muted and cannot send messages!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getDamagee() instanceof Player)) return;

        final Client client = clientManager.search().online(damager);

        Optional<Punishment> pvpLock = client.getPunishment(PunishmentTypes.PVP_LOCK);
        if (pvpLock.isPresent()) {
            UtilMessage.simpleMessage(damager, "Punish", "You are currently PvP Locked and cannot deal damage to other players!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPlace(BlockPlaceEvent event) {
        final Client client = clientManager.search().online(event.getPlayer());

        Optional<Punishment> buildLock = client.getPunishment(PunishmentTypes.BUILD_LOCK);
        if (buildLock.isPresent()) {
            UtilMessage.simpleMessage(event.getPlayer(), "Punish", "You are currently Build Locked and cannot place blocks!");
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onBreak(BlockBreakEvent event) {
        final Client client = clientManager.search().online(event.getPlayer());

        Optional<Punishment> buildLock = client.getPunishment(PunishmentTypes.BUILD_LOCK);
        if (buildLock.isPresent()) {
            UtilMessage.simpleMessage(event.getPlayer(), "Punish", "You are currently Build Locked and cannot break blocks!");
            event.setCancelled(true);
        }
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

}
