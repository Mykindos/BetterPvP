package me.mykindos.betterpvp.core.client.gamer.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
public class ResetProtectionCommand extends Command {
    private final EffectManager effectManager;

    @Inject
    public ResetProtectionCommand(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @Override
    public String getName() {
        return "resetprotection";
    }

    @Override
    public String getDescription() {
        return "Resets PvP protection";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        client.getGamer().saveProperty(GamerProperty.REMAINING_PVP_PROTECTION, 60*1000L);
        effectManager.addEffect(player, EffectTypes.PROTECTION, 60*1000L);
        UtilMessage.message(player, "Protection", "Protection reset");
    }
}
