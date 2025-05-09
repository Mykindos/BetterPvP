package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.effects.events.EffectExpireEvent;
import me.mykindos.betterpvp.core.effects.events.EffectReceiveEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@BPvPListener
@CustomLog
public class AdminVanishCommand extends Command implements Listener {

    private final Set<UUID> vanished = new HashSet<>();
    private final EffectManager effectManager;
    private final ClientManager clientManager;
    private final String effectName;

    @Inject
    public AdminVanishCommand(EffectManager effectManager, ClientManager clientManager){
        this.effectManager = effectManager;
        this.clientManager = clientManager;
        this.effectName = "commandVanish";

        aliases.add("v");
    }

    @Override
    public String getName() {
        return "vanish";
    }

    @Override
    public String getDescription() {
        return "Become invisible and removes you from the tab list and auto-completions.";
    }

    private Component getFakeLeaveMessage(Player player) {
        return Component.text("Leave> ", NamedTextColor.RED)
                .append(Component.text(player.getName(), NamedTextColor.GRAY))
                .append(UtilMessage.deserialize(" <gray>(<green>Safe<gray>)"));
    }

    private Component getFakeJoinMessage(Player player) {
        return Component.text("Join> ", NamedTextColor.GREEN)
                .append(Component.text(player.getName(), NamedTextColor.GRAY));
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (vanished.contains(player.getUniqueId())) { // Is already vanished
            vanished.remove(player.getUniqueId());
            effectManager.removeEffect(player, EffectTypes.VANISH, effectName);
            UtilMessage.message(player, "Vanish", UtilMessage.deserialize("<red>You are no longer vanished.</red>"));
            UtilMessage.broadcast(getFakeJoinMessage(player));
        } else { // Not vanished
            vanished.add(player.getUniqueId());
            effectManager.addEffect(player, player, EffectTypes.VANISH, effectName, 1, 100L, true, true, false, null);
            UtilMessage.message(player, "Vanish", UtilMessage.deserialize("<green>You are now vanished.</green>"));
            UtilMessage.broadcast(getFakeLeaveMessage(player));
        }
    }


    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommandVanish(EffectReceiveEvent event) {
        if (event.getEffect().getEffectType() != EffectTypes.VANISH) return;
        if (!effectName.equals(event.getEffect().getName())) return;
        if (!(event.getTarget() instanceof Player target)) return;
        Bukkit.getOnlinePlayers().forEach(viewer -> {
            unlistPlayer(target, viewer);
        });
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    private void onPlayerJoin(PlayerJoinEvent event) {
        if (effectManager.hasEffect(event.getPlayer(), EffectTypes.VANISH, effectName)) {
            Bukkit.getOnlinePlayers().forEach(viewer -> {
                unlistPlayer(event.getPlayer(), viewer);
            });
        }

        effectManager.getAllEntitiesWithEffects().stream()
                .filter(Player.class::isInstance)
                .map(Player.class::cast)
                .filter(target -> effectManager.hasEffect(target, EffectTypes.VANISH, effectName))
                .forEach(target -> unlistPlayer(target, event.getPlayer()));
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCommandVanish(EffectExpireEvent event) {
        if (event.getEffect().getEffectType() != EffectTypes.VANISH) return;
        if (!effectName.equals(event.getEffect().getName())) return;
        if (!(event.getTarget() instanceof Player target)) return;
        //listing a player throws an error if the viewer cannot see the target
        //vanish handles showing the target, so we have to run this after
        UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () -> {
            Bukkit.getOnlinePlayers().forEach(viewer -> {
                listPlayer(target, viewer);
            });
        }, 1L);
    }

    private void unlistPlayer(Player target, Player viewer) {
        if (clientManager.search().online(viewer).hasRank(Rank.HELPER)) {
            //explicitly show staff commandVanished players
            UtilServer.runTaskLater(JavaPlugin.getPlugin(Core.class), () -> {
                viewer.showPlayer(JavaPlugin.getPlugin(Core.class), target);
            }, 1L);
            return;
        }
        viewer.unlistPlayer(target);
    }

    private void listPlayer(Player target, Player viewer) {
        viewer.listPlayer(target);
    }
}
