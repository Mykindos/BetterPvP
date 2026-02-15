package me.mykindos.betterpvp.core.client.punishments;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.chat.channels.ChatChannel;
import me.mykindos.betterpvp.core.chat.events.ChatSentEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.framework.CurrentMode;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.ocpsoft.prettytime.PrettyTime;

import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@BPvPListener
@Singleton
@CustomLog
public class PunishmentListener implements Listener {

    private final Core core;
    private final ClientManager clientManager;

    private static final long CHECKDELAY = 10_000;

    private static final UUID MYKINDOS = UUID.fromString("e1f5d06b-685b-46a0-b22c-176d6aefffff");

    @Inject
    public PunishmentListener(Core core, ClientManager clientManager) {
        this.core = core;
        this.clientManager = clientManager;
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onLogin(AsyncPlayerPreLoginEvent event) {
        if (event.getUniqueId().equals(MYKINDOS)) {
            event.allow();
            return;
        }
        //client is loaded in LOWEST
        final Client client = clientManager.search().online(event.getUniqueId()).orElseThrow();

        Optional<Punishment> ban = client.getPunishment(PunishmentTypes.BAN);
        if (ban.isPresent()) {
            Punishment punishment = ban.get();

            String reason = punishment.getReason();
            Component banMessage = Component.text("You are banned from the server!", NamedTextColor.RED)
                    .append(Component.newline())
                    .append(Component.text("Reason: ", NamedTextColor.YELLOW)
                            .append(Component.text((reason == null || reason.isEmpty()) ? "N/A" : reason, NamedTextColor.WHITE)))
                    .appendNewline()
                    .appendNewline();

            if (punishment.getExpiryTime() == -1) {
                banMessage = banMessage.append(Component.text("This ban is permanent.", NamedTextColor.RED));
            } else {
                banMessage = banMessage.append(Component.text("This ban will expire ", NamedTextColor.RED).append(Component.text(new PrettyTime().format(new Date(punishment.getExpiryTime())), NamedTextColor.GREEN)));
            }

            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_BANNED, banMessage);
            log.info("Player {} ({}) is banned and has been kicked.", client.getName(), client.getUniqueId()).submit();
        } else {
            log.info("Player {} ({}) is not banned and has logged in.", client.getName(), client.getUniqueId()).submit();
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onChat(ChatSentEvent event) {
        if (event.isCancelled()) return;
        final Client client = clientManager.search().online(event.getPlayer());

        if (client.getGamer().getChatChannel().getChannel() != ChatChannel.SERVER) return;
        client.getPunishment(PunishmentTypes.MUTE).ifPresent(mute -> {
            UtilMessage.simpleMessage(event.getPlayer(), "Punish", "You are currently muted and cannot send messages!");
            UtilMessage.message(event.getPlayer(), "Punish", mute.getInformation());
            event.cancel("Player is muted");
        });
    }

    @EventHandler
    public void onDamage(DamageEvent event) {
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getDamagee() instanceof Player)) return;
        if (core.getCurrentMode() != CurrentMode.CLANS) return;

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
            UtilMessage.simpleMessage(event.getPlayer(), "Punish", "You are currently Build Locked and cannot break blocks!");
            UtilMessage.message(event.getPlayer(), "Punish", buildLock.getInformation());
            event.setCancelled(true);
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onCanHurt(EntityCanHurtEntityEvent event) {
        if (!event.isAllowed()) return;
        if (core.getCurrentMode() != CurrentMode.CLANS) return;

        if (event.getDamager() instanceof Player damager && event.getDamagee() instanceof Player) {
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
        Bukkit.getOnlinePlayers().forEach(player -> {
            Client client = clientManager.search().online(player);
            client.getPunishments().forEach(punishment -> {
                if (punishment.isRevoked()) return;
                if (punishment.hasExpired() && timeStart - punishment.getExpiryTime() <= CHECKDELAY) {
                    punishment.getType().onExpire(client.getUniqueId(), punishment);
                }
            });
        });
    }

}
