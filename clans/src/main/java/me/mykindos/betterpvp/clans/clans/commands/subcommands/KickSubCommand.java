package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanKickMemberEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.ClientManager;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.Optional;

public class KickSubCommand extends ClanSubCommand {

    @Inject
    private ClientManager clientManager;

    public KickSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public String getDescription() {
        return "Kick a member from your clan";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, "Clans", "You must specify a member to kick.");
            return;
        }

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();

        Optional<Client> clientOptional = clientManager.getClientByName(args[0]);
        if (clientOptional.isPresent()) {
            Client target = clientOptional.get();

            ClanMember ourMember = clan.getMember(player.getUniqueId());
            if (!ourMember.hasRank(ClanMember.MemberRank.ADMIN)) {
                UtilMessage.message(player, "Clans", "Only the Clan Leader and Admins can kick members.");
                return;
            }

            if (target.getUuid().equals(player.getUniqueId().toString())) {
                UtilMessage.message(player, "Clans", "You cannot kick yourself.");
                return;
            }

            Optional<ClanMember> memberOptional = clan.getMemberByUUID(target.getUuid());
            if (memberOptional.isPresent()) {
                ClanMember member = memberOptional.get();

                if (member.getRank().getPrivilege() >= ourMember.getRank().getPrivilege()) {
                    UtilMessage.message(player, "Clans", "You are not a high enough rank to kick this member");
                    return;
                }


                Player targetPlayer = Bukkit.getPlayer(args[0]);
                if (targetPlayer != null) {
                    Optional<Clan> locationClanOptional = clanManager.getClanByLocation(targetPlayer.getLocation());
                    if (locationClanOptional.isPresent()) {
                        Clan locationClan = locationClanOptional.get();
                        if (clan.isEnemy(locationClan)) {
                            UtilMessage.message(player, "Clans", "You cannot leave your clan while in enemy territory");
                            return;
                        }
                    }
                }

                UtilServer.callEvent(new ClanKickMemberEvent(player, clan, target));

            } else {
                UtilMessage.message(player, "Clans", ChatColor.YELLOW + target.getName() + ChatColor.GRAY + " is not in your clan");
            }

        }
    }

    @Override
    public String getArgumentType(int arg) {
        return ClanArgumentType.CLAN_MEMBER.name();
    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
