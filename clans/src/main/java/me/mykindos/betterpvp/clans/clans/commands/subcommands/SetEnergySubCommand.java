package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class SetEnergySubCommand extends ClanSubCommand {

    @Inject
    public SetEnergySubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "setenergy";
    }

    @Override
    public String getDescription() {
        return "Force set the energy for your current clan";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <energy>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        int energy = Integer.parseInt(args[0]);

        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();

        int oldEnergy = playerClan.getEnergy();
        playerClan.setEnergy(energy);

        Component component = Component.text("Energy set to: ", NamedTextColor.GRAY).append(Component.text(playerClan.getEnergy() + " - (", NamedTextColor.YELLOW)
                .append(Component.text(playerClan.getEnergyTimeRemaining(), NamedTextColor.GOLD).append(Component.text(")", NamedTextColor.YELLOW))))
            .append(Component.text(" Previous: ", NamedTextColor.GRAY)).append(Component.text(oldEnergy, NamedTextColor.YELLOW));

        playerClan.getMembersAsPlayers().forEach(player1 -> {
            UtilServer.callEvent(new ScoreboardUpdateEvent(player1));
        });

        UtilMessage.message(player, "Clans", component);
    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }
}
