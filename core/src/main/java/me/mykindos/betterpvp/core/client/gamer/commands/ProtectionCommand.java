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
        return "core.command.protection.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (!effectManager.hasEffect(player, EffectTypes.PROTECTION)) {
            UtilMessage.message(player, "core.prefix.protection", "core.command.protection.not_active");
            return;
        }
        // NOTE: ConfirmationMenu requires a String; translating this title is skipped for now (see summary)
        new ConfirmationMenu("Disable Protection", success -> {
            if (Boolean.TRUE.equals(success)) {
                client.getGamer().saveProperty(GamerProperty.REMAINING_PVP_PROTECTION, 0L);
                effectManager.removeEffect(player, EffectTypes.PROTECTION, false);
                UtilMessage.message(player, "core.prefix.protection", "core.command.protection.disabled");
            }

        }).show(player);

    }
}
