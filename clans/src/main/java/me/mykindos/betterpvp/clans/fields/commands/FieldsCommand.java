package me.mykindos.betterpvp.clans.fields.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.entity.Player;

@Singleton
public class FieldsCommand extends Command {

    private final ClanManager clanManager;
    private final GamerManager gamerManager;

    @WithReflection
    @Inject
    public FieldsCommand(ClanManager clanManager, GamerManager gamerManager) {
        this.clanManager = clanManager;
        this.gamerManager = gamerManager;
    }

    @Override
    public String getName() {
        return "fields";
    }

    @Override
    public String getDescription() {
        return "Fields administration command";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 0) return;

        UtilMessage.simpleMessage(player, "Fields", "Fields administration commands:");
        UtilMessage.simpleMessage(player, "Fields", "/fields claim [borderSize] - Claim land for fields");
        UtilMessage.simpleMessage(player, "Fields", "/fields respawn - Respawn all ores in fields");
        UtilMessage.simpleMessage(player, "Fields", "/fields info - Get current information for fields");
    }

}
