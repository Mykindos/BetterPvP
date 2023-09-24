package me.mykindos.betterpvp.clans.fields.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.fields.Fields;
import me.mykindos.betterpvp.clans.fields.commands.FieldsCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(FieldsCommand.class)
public class FieldsRespawnSubCommand extends Command {

    @Inject
    private ClanManager clanManager;

    @Inject
    private Fields fields;

    @Override
    public String getName() {
        return "respawn";
    }

    @Override
    public String getDescription() {
        return "Respawn all ores in Fields.";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        fields.getBlocks().forEach((type, ore) -> ore.setLastUsed(0));

        UtilMessage.simpleMessage(player, "Clans", "Successfully respawned all ores in Fields.");
    }

}
