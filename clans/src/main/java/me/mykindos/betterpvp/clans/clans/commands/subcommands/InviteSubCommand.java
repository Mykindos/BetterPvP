package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanInviteMemberEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Optional;

public class InviteSubCommand extends ClanSubCommand {

    @Inject
    @Config(path="clans.members.max", defaultValue = "6")
    private int maxClanMembers;

    public InviteSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "invite";
    }

    @Override
    public String getDescription() {
        return "Invite a player to your clan";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if(args.length == 0) {
            UtilMessage.message(player, "Clans", "You did not input a player to invite.");
            return;
        }

        Optional<Clan> clanOptional = clanManager.getClanByPlayer(player);
        if(clanOptional.isEmpty()) return;

        Clan clan = clanOptional.get();

        if(!clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)){
            UtilMessage.message(player, "Clans", "Only the Clan Leader and Admins can send invites.");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if(target == null){
            UtilMessage.message(player, "Clans", "The player you want to invite must be online");
            return;
        }

        if(target.equals(player)){
            UtilMessage.message(player, "Clans", "You cannot invite yourself");
            return;
        }

        Optional<Clan> targetClan = clanManager.getClanByPlayer(target);
        if(targetClan.isPresent()) {
            UtilMessage.message(player, "Clans", ChatColor.YELLOW + target.getName() + ChatColor.GRAY + " is apart of "
                    + ChatColor.YELLOW + "Clan " + targetClan.get().getName() + ChatColor.GRAY + ".");
            return;
        }

        if(clan.getSquadCount() >= maxClanMembers){
            UtilMessage.message(player, "Clans", "Your clan has too many members or allies to invite another member.");
            return;
        }

        UtilServer.callEvent(new ClanInviteMemberEvent(player, clan, target));

    }

    @Override
    public String getArgumentType(int arg) {
        if (arg == 1) {
            return ArgumentType.PLAYER.name();
        }

        return ArgumentType.NONE.name();
    }
}
