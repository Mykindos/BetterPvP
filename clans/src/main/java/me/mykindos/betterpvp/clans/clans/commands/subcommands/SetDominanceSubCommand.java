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
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class SetDominanceSubCommand extends ClanSubCommand {

    @Inject
    public SetDominanceSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "setdominance";
    }

    @Override
    public String getDescription() {
        return "clans.command.set-dominance.description";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <clan> <dominance>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length < 2) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.set-dominance.usage", Component.text(getUsage()));
            return;
        }

        int dominance = Integer.parseInt(args[1]);
        if(dominance > 99 || dominance < 0) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.set-dominance.invalid-dominance");
            return;
        }

        Optional<Clan> targetClanOptional = clanManager.getClanByName(args[0]);
        if(targetClanOptional.isEmpty()) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.set-dominance.not-found");
            return;
        }

        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();
        Clan targetClan = targetClanOptional.get();

        ClanEnemy playerClanEnemy = playerClan.getEnemy(targetClan).orElse(null);
        if(playerClanEnemy == null) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.set-dominance.not-enemy");
            return;
        }

        ClanEnemy targetClanEnemy = targetClan.getEnemy(playerClan).orElse(null);
        if(targetClanEnemy == null) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.set-dominance.error");
            return;
        }

        playerClanEnemy.setDominance(dominance);
        targetClanEnemy.setDominance(0);
        clanManager.getRepository().updateDominance(playerClan, playerClanEnemy);
        clanManager.getRepository().updateDominance(targetClan, targetClanEnemy);

        playerClan.getMembers().forEach(member -> {
            Player clanPlayer = org.bukkit.Bukkit.getPlayer(member.getUuid());
            if (clanPlayer != null) {
                UtilMessage.message(clanPlayer, CLANS_PREFIX, "clans.command.clan.set-dominance.clan-notification",
                        Component.text(targetClan.getName(), NamedTextColor.RED),
                        Component.text(dominance + "%", NamedTextColor.GREEN));
            }
        });

        targetClan.getMembers().forEach(member -> {
            Player clanPlayer = org.bukkit.Bukkit.getPlayer(member.getUuid());
            if (clanPlayer != null) {
                UtilMessage.message(clanPlayer, CLANS_PREFIX, "clans.command.clan.set-dominance.target-notification",
                        Component.text(playerClan.getName(), NamedTextColor.RED),
                        Component.text("-" + dominance + "%", NamedTextColor.RED));
            }
        });

        Component notification = Translations.component("clans.command.clan.set-dominance.mod-notification",
                Component.text(player.getName(), NamedTextColor.YELLOW),
                Component.text(playerClan.getName(), NamedTextColor.YELLOW),
                Component.text(targetClan.getName(), NamedTextColor.YELLOW),
                Component.text(dominance, NamedTextColor.GREEN));

        clientManager.getPlayersOfRank(Rank.TRIAL_MOD).forEach(target -> {
            UtilMessage.message(target, Translations.component(CLANS_PREFIX), notification);
        });
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ClanArgumentType.CLAN.name() : ArgumentType.NONE.name();
    }

    @Override
    public boolean requiresServerAdmin() {
        return true;
    }
}
