package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ChunkUnclaimEvent;
import me.mykindos.betterpvp.clans.utilities.UtilClans;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class UnclaimSubCommand extends ClanSubCommand {

    @Inject
    public UnclaimSubCommand(ClanManager clanManager, ClientManager clientManager) {
        super(clanManager, clientManager);
    }

    @Override
    public String getName() {
        return "unclaim";
    }

    @Override
    public String getDescription() {
        return "clans.command.unclaim.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {

        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(player.getLocation());
        if(locationClanOptional.isEmpty()) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.unclaim.not-claimed");
            return;
        }

        Clan playerClan = clanManager.getClanByPlayer(player).orElseThrow();
        Clan locationClan = locationClanOptional.get();

        if (playerClan.equals(locationClan)) {
            if (!playerClan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
                UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.unclaim.no-rank");
                return;
            }

            if (locationClan.getTerritory().size() > 2) {

                // Pass territory 2d array into algorithm.
                if(UtilClans.isClaimRequired(UtilClans.getClaimLayout(player, locationClan))){
                    UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.unclaim.split-territory");
                    return;
                }
            }
        } else {
            if (!client.isAdministrating()) {
                if (locationClan.getTerritory().size() <= clanManager.getMaximumClaimsForClan(locationClan) && !client.isAdministrating()) {
                    UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.unclaim.not-enough-members",
                            Component.text(locationClan.getName(), NamedTextColor.YELLOW));
                    return;
                }
            } else {
                Component notification = Translations.component("clans.command.clan.unclaim.mod-notification",
                        Component.text(player.getName(), NamedTextColor.YELLOW),
                        Component.text(UtilWorld.chunkToPrettyString(player.getLocation().getChunk()), NamedTextColor.YELLOW),
                        Component.text(locationClan.getName(), NamedTextColor.YELLOW));
                clientManager.getPlayersOfRank(Rank.TRIAL_MOD).forEach(target -> {
                    UtilMessage.message(target, Translations.component(CLANS_PREFIX), notification);
                });
            }


            if (UtilClans.isClaimRequired(UtilClans.getClaimLayout(player, locationClan))){
                UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.unclaim.other-split-territory");
                return;
            }

            for (ClanMember clanMember : locationClan.getMembers()) {
                final Optional<Client> clientOpt = clientManager.search().online(clanMember.getUuid());
                if (clientOpt.isPresent()) {
                    final Client online = clientOpt.get();
                    if (online.isAdministrating()) {
                        UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.unclaim.prevented");

                        Component notification = Translations.component("clans.command.clan.unclaim.prevented-mod-notification",
                                Component.text(online.getName(), NamedTextColor.YELLOW),
                                Component.text(player.getName(), NamedTextColor.YELLOW),
                                Component.text(locationClan.getName(), NamedTextColor.YELLOW));
                        clientManager.getPlayersOfRank(Rank.TRIAL_MOD).forEach(target -> {
                            UtilMessage.message(target, Translations.component(CLANS_PREFIX), notification);
                        });
                        return;
                    }
                }
            }
        }

        UtilServer.callEvent(new ChunkUnclaimEvent(player, locationClan));
    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
