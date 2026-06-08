package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.MemberDemoteEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@SubCommand(ClanCommand.class)
public class DemoteSubCommand extends ClanSubCommand {

    @Inject
    public DemoteSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "demote";
    }

    @Override
    public String getDescription() {
        return "clans.command.demote.description";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <player>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        if (args.length == 0) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.demote.no-args");
            return;
        }

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();;

        ClanMember member = clan.getMember(player.getUniqueId());
        if (member.getRank().getPrivilege() < ClanMember.MemberRank.ADMIN.getPrivilege()) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.demote.no-permission");
            return;
        }

        String targetMemberName = args[0];
        if(player.getName().equalsIgnoreCase(targetMemberName)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.demote.self");
            return;
        }

        clientManager.search(player).offline(targetMemberName).thenAcceptAsync(result -> {
            UtilServer.runTask(JavaPlugin.getPlugin(Clans.class), () -> {
                if (result.isEmpty()) {
                    UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.demote.not-found");
                    return;
                }

                final Client found = result.get();
                clan.getMemberByUUID(found.getUniqueId()).ifPresentOrElse(targetMember -> {
                    if (targetMember.getRank().getPrivilege() >= member.getRank().getPrivilege()
                            && !client.isAdministrating()) {
                        UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.demote.lower-rank-only");
                        return;
                    } else if (client.isAdministrating() && targetMember.getRank().getPrivilege() >= member.getRank().getPrivilege()) {
                        Component notification = Translations.component("clans.command.clan.demote.mod-notification",
                                Component.text(player.getName(), NamedTextColor.YELLOW),
                                Component.text(found.getName(), NamedTextColor.YELLOW));
                        clientManager.getPlayersOfRank(Rank.TRIAL_MOD).forEach(target -> {
                            UtilMessage.message(target, Translations.component(CLANS_PREFIX), notification);
                        });
                    }

                    UtilServer.callEvent(new MemberDemoteEvent(player, clan, targetMember));
                    SoundEffect.LOW_PITCH_PLING.play(player);
                }, () -> {
                    UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.demote.not-in-clan");
                });
            });
        });

    }

    @Override
    public String getArgumentType(int arg) {
        return arg == 1 ? ClanArgumentType.CLAN_MEMBER.name() : ArgumentType.NONE.name();
    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
