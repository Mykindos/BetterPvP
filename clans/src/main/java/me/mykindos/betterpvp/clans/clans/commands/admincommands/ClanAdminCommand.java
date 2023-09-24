package me.mykindos.betterpvp.clans.clans.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.framework.annotations.WithReflection;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.Optional;

@Singleton
public class ClanAdminCommand extends Command {

    private final ClanManager clanManager;
    private final GamerManager gamerManager;

    @WithReflection
    @Inject
    public ClanAdminCommand(ClanManager clanManager, GamerManager gamerManager) {
        this.clanManager = clanManager;
        this.gamerManager = gamerManager;

        aliases.addAll(List.of("cx", "clanmimic", "cm"));

    }

    @Override
    public String getName() {
        return "clanadmin";
    }

    @Override
    public String getDescription() {
        return "Basic clanadmin";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 1) return;

        Optional<Clan> targetClanOptional = clanManager.getClanByName(args[0]);
        if(targetClanOptional.isEmpty()) {
            UtilMessage.message(player, "Clans", "Could not find a clan with that name");
            return;
        }

        Gamer adminGamer = gamerManager.getObject(player.getUniqueId().toString())
        Clan targetClan = targetClanOptional.get();

        adminGamer.getClient().setMimicClan(targetClan.getId());
        UtilMessage.message(player, "Clans", "Now mimicking Clan <yellow>" + targetClan.getName());

    }

    public boolean requiresServerAdmin() {
        return true;
    }

    public boolean canExecuteWithoutClan() {
        return true;
    }
}
