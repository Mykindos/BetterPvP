package me.mykindos.betterpvp.core.client.gamer.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.properties.GamerProperty;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
public class ProtectionCommand extends Command {
    private final EffectManager effectManager;

    @Inject
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
        new ConfirmationMenu("Disable Protection", (success)-> {
            client.getGamer().saveProperty(GamerProperty.REMAINING_PVP_PROTECTION, 0L);
            effectManager.removeEffect(player, EffectTypes.PROTECTION);
            UtilMessage.message(player, "Protection", "Protection disabled");
        }).show(player);

    }
}
