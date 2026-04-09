package me.mykindos.betterpvp.core.command.commands.general;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.punishments.PunishmentTypes;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.server.CrossServerMessageService;
import me.mykindos.betterpvp.core.framework.server.ServerMessage;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.entity.Player;

@Singleton
public class PlayerAdminCommand extends Command {

    private final CooldownManager cooldownManager;
    private final CrossServerMessageService crossServerMessageService;

    @Inject
    public PlayerAdminCommand(CooldownManager cooldownManager, CrossServerMessageService crossServerMessageService) {
        this.cooldownManager = cooldownManager;
        this.crossServerMessageService = crossServerMessageService;

        aliases.add("a");
    }

    @Override
    public String getName() {
        return "admin";
    }

    @Override
    public String getDescription() {
        return "Send a message to staff";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, "Core", "You must specify a message");
            return;
        }

        if (client.hasPunishment(PunishmentTypes.MUTE)) {
            if (!cooldownManager.use(player, getName(), 120, false, false)) {
                UtilMessage.message(player, "Core", "You must wait 2 minutes between using this command.");
                return;
            }
        }

        ServerMessage build = ServerMessage.builder()
                .channel("AdminMessage")
                .message(String.join(" ", args))
                .metadata("sender", player.getUniqueId().toString())
                .build();
        crossServerMessageService.broadcast(build);
        new SoundEffect("minecraft", "block.amethyst_block.resonate", 1.0F).play(player);
    }
}
