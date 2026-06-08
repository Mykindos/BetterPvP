package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

@Singleton
@SubCommand(ClanCommand.class)
public class SetEnergySubCommand extends ClanSubCommand {

    @Inject
    public SetEnergySubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "setenergy";
    }

    @Override
    public String getDescription() {
        return "clans.command.set-energy.description";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <energy>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 1) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.set-energy.usage");
            return;
        }
        int energy;
        try {
            energy = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.set-energy.invalid-input", Component.text(args[0], NamedTextColor.YELLOW));
            return;
        }

        if (energy < 0) energy = 0;

        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();

        int oldEnergy = playerClan.getEnergy();
        playerClan.setEnergy(energy);

        Component component = Translations.component("clans.command.clan.set-energy.success",
                Component.text(playerClan.getEnergy(), NamedTextColor.YELLOW),
                Component.text(playerClan.getEnergyTimeRemaining(), NamedTextColor.GOLD),
                Component.text(oldEnergy, NamedTextColor.YELLOW));

        UtilMessage.message(player, CLANS_PREFIX, component);

        Component notification = Translations.component("clans.command.clan.set-energy.mod-notification",
                Component.text(player.getName(), NamedTextColor.YELLOW),
                Component.text(playerClan.getName(), NamedTextColor.YELLOW),
                Component.text(playerClan.getEnergy(), NamedTextColor.GREEN),
                Component.text(playerClan.getEnergyTimeRemaining(), NamedTextColor.YELLOW));

        clientManager.getPlayersOfRank(Rank.TRIAL_MOD).forEach(target -> {
            UtilMessage.message(target, Translations.component(CLANS_PREFIX), notification);
        });
    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }
}
