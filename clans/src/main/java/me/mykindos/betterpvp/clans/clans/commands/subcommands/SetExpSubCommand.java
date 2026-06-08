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
public class SetExpSubCommand extends ClanSubCommand {

    @Inject
    public SetExpSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "setexp";
    }

    @Override
    public String getDescription() {
        return "clans.command.set-exp.description";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <experience>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length != 1) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.set-exp.usage", Component.text(getUsage()));
            return;
        }

        double experience;
        try {
            experience = Double.parseDouble(args[0]);
        } catch (NumberFormatException e) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.set-exp.invalid-number");
            return;
        }

        if (experience < 0) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.set-exp.negative-exp");
            return;
        }

        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();

        double prevExperience = playerClan.getExperience();
        long prevLevel = playerClan.getLevel();
        playerClan.setExperience(experience);
        double newExperience = playerClan.getExperience();
        long newLevel = playerClan.getLevel();

        UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.set-exp.success",
                Component.text(playerClan.getName(), NamedTextColor.YELLOW),
                Component.text(String.format("%,.1f", prevExperience), NamedTextColor.YELLOW),
                Component.text(String.format("%,d", prevLevel), NamedTextColor.YELLOW),
                Component.text(String.format("%,.1f", newExperience), NamedTextColor.YELLOW),
                Component.text(String.format("%,d", newLevel), NamedTextColor.YELLOW));


        Component notification = Translations.component("clans.command.clan.set-exp.mod-notification",
                Component.text(client.getName(), NamedTextColor.YELLOW),
                Component.text(playerClan.getName(), NamedTextColor.YELLOW),
                Component.text(String.format("%,.1f", prevExperience), NamedTextColor.YELLOW),
                Component.text(String.format("%,d", prevLevel), NamedTextColor.YELLOW),
                Component.text(String.format("%,.1f", newExperience), NamedTextColor.YELLOW),
                Component.text(String.format("%,d", newLevel), NamedTextColor.YELLOW));

        clientManager.getPlayersOfRank(Rank.TRIAL_MOD).forEach(target -> {
            UtilMessage.message(target, Translations.component(CLANS_PREFIX), notification);
        });
    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }
}
