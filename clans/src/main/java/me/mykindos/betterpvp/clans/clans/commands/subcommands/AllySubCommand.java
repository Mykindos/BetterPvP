package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanRequestAllianceEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class AllySubCommand extends ClanSubCommand {

    @Inject
    @Config(path = "clans.members.max", defaultValue = "8")
    private int maxClanMembers;

    @Inject
    public AllySubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "ally";
    }

    @Override
    public String getDescription() {
        return "clans.command.ally.description";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <clan>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.ally.no-args");
            return;
        }

        Clan clan = clanManager.getClanByPlayer(player).orElse(null);
        if(clan == null) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.ally.error");
            return;
        }

        if (!clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.ally.no-rank");
            return;
        }

        Optional<Clan> targetClanOptional = clanManager.getClanByName(args[0]);
        if (targetClanOptional.isEmpty()) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.ally.not-found");
            return;
        }

        Clan targetClan = targetClanOptional.get();
        if (clan.equals(targetClan)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.ally.self");
            return;
        }

        if (clan.isAllied(targetClan)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.ally.already-allied");
            return;
        }

        if(clan.isEnemy(targetClan)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.ally.at-war");
            return;
        }

        if (clan.getSquadCount() >= maxClanMembers) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.ally.max-squad-size", Component.text(clan.getSquadCount(), NamedTextColor.GREEN));
            return;
        }

        int ownClanSize = clan.getMembers().size();
        if (ownClanSize + targetClan.getSquadCount() > maxClanMembers) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.ally.target-too-many-members",
                    Component.text(targetClan.getName(), NamedTextColor.YELLOW),
                    Component.text(targetClan.getSquadCount(), NamedTextColor.GREEN),
                    Component.text(ownClanSize, NamedTextColor.GREEN));
            return;
        }

        int targetClanSize = targetClan.getMembers().size();
        if (targetClanSize + clan.getSquadCount() > maxClanMembers) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.ally.own-too-many-members",
                    Component.text(clan.getSquadCount(), NamedTextColor.GREEN),
                    Component.text(targetClan.getName(), NamedTextColor.YELLOW),
                    Component.text(targetClanSize, NamedTextColor.GREEN));
            return;
        }

        UtilServer.callEvent(new ClanRequestAllianceEvent(player, clan, targetClan));

    }

    @Override
    public String getArgumentType(int arg) {
        return ClanArgumentType.CLAN.name();
    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }

}
