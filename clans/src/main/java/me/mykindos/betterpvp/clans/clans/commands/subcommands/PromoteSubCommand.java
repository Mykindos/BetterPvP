package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.MemberDemoteEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberPromoteEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.menu.impl.ConfirmationMenu;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

@Singleton
@SubCommand(ClanCommand.class)
public class PromoteSubCommand extends ClanSubCommand {

    @Inject
    public PromoteSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "promote";
    }

    @Override
    public String getDescription() {
        return "clans.command.promote.description";
    }

    @Override
    public String getUsage() {
        return super.getUsage() + " <player>";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (args.length == 0) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.promote.no-args");
            return;
        }

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();

        ClanMember member = clan.getMember(player.getUniqueId());
        if (member.getRank().getPrivilege() < ClanMember.MemberRank.ADMIN.getPrivilege()) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.promote.no-permission");
            return;
        }

        String targetMemberName = args[0];
        if(player.getName().equalsIgnoreCase(targetMemberName)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.promote.self");
            return;
        }

        clientManager.search(player).offline(targetMemberName).thenAcceptAsync(result -> {
            UtilServer.runTask(JavaPlugin.getPlugin(Clans.class), () -> {
                if (result.isEmpty()) {
                    UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.promote.not-found");
                    return;
                }

                clan.getMemberByUUID(result.get().getUniqueId()).ifPresentOrElse(targetMember -> {
                    if (targetMember.getRank().getPrivilege() + 1 >= member.getRank().getPrivilege()){
                        if (member.getRank() == ClanMember.MemberRank.LEADER) {
                            new ConfirmationMenu(Translations.component("clans.command.clan.promote.leader-confirmation").toString(), success -> {
                                if (success) {
                                    UtilServer.callEvent(new MemberDemoteEvent(player, clan, member));
                                    UtilServer.callEvent(new MemberPromoteEvent(player, clan, targetMember));
                                }
                            }).show(player);
                        } else {
                            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.promote.lower-rank-only");
                        }

                        return;
                    }

                    UtilServer.callEvent(new MemberPromoteEvent(player, clan, targetMember));
                    SoundEffect.HIGH_PITCH_PLING.play(player);
                }, () -> {
                    UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.promote.not-in-clan");
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
