package me.mykindos.betterpvp.clans.clans.protection;

import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

public class ProtectionCommand extends Command {
    private final EffectManager effectManager;

    public ProtectionCommand(EffectManager effectManager) {
        this.effectManager = effectManager;
    }

    @Override
    public String getName() {
        return "protection";
    }

    @Override
    public String getDescription() {
        return "Disables PvP protection";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (!effectManager.hasEffect(player, EffectTypes.PROTECTION)) {
            UtilMessage.message(player, "Protection", "You currently do not have protection");
            return;
        }
        client.getGamer().saveProperty(GamerProperty.REMAINING_PVP_PROTECTION, 0L);
        effectManager.removeEffect(player, EffectTypes.PROTECTION);
        UtilMessage.message(player, "Protection", "Protection disabled");
    }
}
