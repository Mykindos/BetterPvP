package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

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

}
