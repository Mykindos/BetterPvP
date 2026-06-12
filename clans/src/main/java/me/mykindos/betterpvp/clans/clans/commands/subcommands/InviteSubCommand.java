package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanInviteMemberEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class InviteSubCommand extends ClanSubCommand {

    @Inject
    @Config(path="clans.members.max", defaultValue = "8")
    private int maxClanMembers;

    @Inject
    public InviteSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "invite";
    }

    @Override
    public String getDescription() {
        return "clans.command.invite.description";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <player>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.invite.no-args");
            return;
        }

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();;

        if (!clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)){
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.invite.no-rank");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null){
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.invite.not-online");
            return;
        }

        if (target.equals(player)){
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.invite.self");
            return;
        }

        Optional<Clan> targetClan = clanManager.getClanByPlayer(target);
        if (targetClan.isPresent()) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.invite.already-in-clan",
                    Component.text(target.getName(), NamedTextColor.YELLOW),
                    Component.text(targetClan.get().getName(), NamedTextColor.YELLOW));
            return;
        }

        if (clan.getSquadCount() >= maxClanMembers){
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.invite.limit");
            return;
        }

        boolean allySquadCountTooHigh = false;
        for (ClanAlliance clanAlliance : clan.getAlliances()) {
            if (clanAlliance.getClan().getSquadCount() + 1 > maxClanMembers) {
                UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.invite.ally-limit",
                        Component.text(clanAlliance.getClan().getName(), NamedTextColor.YELLOW));
                allySquadCountTooHigh = true;
            }
        }

        if(clanManager.getPillageHandler().isBeingPillaged(clan)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.invite.pillaged");
            return;
        }

        if (allySquadCountTooHigh) {
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

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
