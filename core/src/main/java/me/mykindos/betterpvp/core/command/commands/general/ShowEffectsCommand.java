package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

@Singleton
public class ShowEffectsCommand extends Command {

    private final EffectManager effectManager;

    @Inject
    public ShowEffectsCommand(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @Override
    public String getName() {
        return "showeffects";
    }

    @Override
    public String getDescription() {
        return "List all effects the player currently has";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        Optional<List<Effect>> listOptional = effectManager.getObject(player.getUniqueId());
        if (listOptional.isEmpty()) {
            UtilMessage.message(player, "Effects", "You do not currently have any effects applied");
            return;
        }
        List<Effect> effects = listOptional.get();
        effects.forEach(effect -> {
            UtilMessage.message(player, "Effects", "<white>%s %s</white>: <green>%s</green>",
                    effect.getName(), effect.getAmplifier(), UtilTime.getTime(effect.getLength(), 1));
        });
    }
}
