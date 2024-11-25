package me.mykindos.betterpvp.core.command.commands.admin;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Singleton
public class AdminVanishCommand extends Command {

    private final Set<UUID> vanished = new HashSet<>();
    private final EffectManager effectManager;

    @Inject
    public AdminVanishCommand(EffectManager effectManager){
        this.effectManager = effectManager;

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

    @Override
    public void execute(Player player, Client client, String... args) {
        if (vanished.contains(player.getUniqueId())) { // Is already vanished
            vanished.remove(player.getUniqueId());
            effectManager.removeEffect(player, EffectTypes.VANISH, "commandVanish");
            UtilMessage.message(player, "Vanish", UtilMessage.deserialize("<red>You are no longer vanished.</red>"));
        } else { // Not vanished
            vanished.add(player.getUniqueId());
            effectManager.addEffect(player, player, EffectTypes.VANISH, "commandVanish", 1, 100L, true, true);
            UtilMessage.message(player, "Vanish", UtilMessage.deserialize("<green>You are now vanished.</green>"));
        }
    }
}
