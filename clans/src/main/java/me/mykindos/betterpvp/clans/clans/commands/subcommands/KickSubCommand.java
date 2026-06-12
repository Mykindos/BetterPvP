package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ClanKickMemberEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class KickSubCommand extends ClanSubCommand {

    @Inject
    private ClientManager clientManager;

    @Inject
    public KickSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "kick";
    }

    @Override
    public String getDescription() {
        return "clans.command.kick.description";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <player>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.kick.no-args");
            return;
        }

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();

        clientManager.search(player).offline(args[0]).thenAcceptAsync(result -> {
            UtilServer.runTask(JavaPlugin.getPlugin(Clans.class), () -> {
                if (result.isEmpty()) {
                    return;
                }

                Client target = result.get();
                ClanMember ourMember = clan.getMember(player.getUniqueId());
                if (!ourMember.hasRank(ClanMember.MemberRank.ADMIN)) {
                    UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.kick.no-rank");
                    return;
                }

                if (target.getUuid().equals(player.getUniqueId().toString())) {
                    UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.kick.self");
                    return;
                }

                Optional<ClanMember> memberOptional = clan.getMemberByUUID(target.getUuid());
                if (memberOptional.isPresent()) {
                    ClanMember member = memberOptional.get();

                    if (member.getRank().getPrivilege() >= ourMember.getRank().getPrivilege()) {
                        UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.kick.low-rank");
                        return;
                    }


                    Player targetPlayer = Bukkit.getPlayerExact(args[0]);
                    if (targetPlayer != null) {
                        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(targetPlayer.getLocation());
                        if (locationClanOptional.isPresent()) {
                            Clan locationClan = locationClanOptional.get();
                            if (clan.isEnemy(locationClan)) {
                                UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.kick.enemy-territory", Component.text(targetPlayer.getName(), NamedTextColor.AQUA));
                                return;
                            }
                        }
                    }


                    UtilServer.callEvent(new ClanKickMemberEvent(player, clan, target));

                } else {
                    UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.kick.not-in-clan", Component.text(target.getName(), NamedTextColor.YELLOW));
                }
            });

        });
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
