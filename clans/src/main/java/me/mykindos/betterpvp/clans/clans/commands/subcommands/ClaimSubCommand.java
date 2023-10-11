package me.mykindos.betterpvp.clans.clans.commands.subcommands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.commands.ClanCommand;
import me.mykindos.betterpvp.clans.clans.commands.ClanSubCommand;
import me.mykindos.betterpvp.clans.clans.events.ChunkClaimEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class ClaimSubCommand extends ClanSubCommand {

    @Inject
    @Config(path = "clans.claims.additional", defaultValue = "3")
    private int additionalClaims;

    @Inject
    public ClaimSubCommand(ClanManager clanManager, GamerManager gamerManager) {
        super(clanManager, gamerManager);
    }

    @Override
    public String getName() {
        return "claim";
    }

    @Override
    public String getDescription() {
        return "Claim territory for your clan";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (player.getWorld().getName().equalsIgnoreCase("bossworld")) return;

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();

        if (!clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
            UtilMessage.message(player, "Clans", "You need to be a clan admin to claim land");
            return;
        }

        if (player.getWorld().getEnvironment().equals(World.Environment.NETHER) && !client.isAdministrating()) {
            UtilMessage.message(player, "Clans", "You cannot claim land in the nether.");
            return;
        }

        if (!(clan.isAdmin() || client.isAdministrating())) {
            if (clan.getTerritory().size() >= clan.getMembers().size() + additionalClaims) {
                UtilMessage.message(player, "Clans", "Your Clan cannot claim more Territory.");
                return;
            }
        } else {
            //This wording is because this is run before other validation checks are ran, so it may not go through
            gamerManager.sendMessageToRank("Clans",
                    UtilMessage.deserialize("<yellow>%s<gray> is attempting to claim teritory over the limit for <yellow>%s",
                            player.getName(), clan.getName()), Rank.HELPER);
        }

        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(player.getLocation());
        if (locationClanOptional.isPresent()) {
            Clan locationClan = locationClanOptional.get();
            if (locationClan.equals(clan)) {
                UtilMessage.message(player, "Clans", "Your clan already owns this territory");
            } else {
                UtilMessage.message(player, "Clans", "This territory is owned by <alt2>Clan " + locationClan.getName() + "</alt2>.");
            }
            return;
        }

        Chunk chunk = player.getLocation().getChunk();
        World world = player.getWorld();
        if (chunk.getEntities() != null) {
            for (Entity entitys : chunk.getEntities()) {
                if (entitys instanceof Player target) {
                    if (entitys.equals(player)) {
                        continue;
                    }

                    if (clanManager.canHurt(player, target)) {
                        Optional<Clan> targetClanOptional = clanManager.getClanByPlayer(target);
                        if (targetClanOptional.isEmpty()) continue;
                        UtilMessage.message(player, "Clans", "You cannot claim Territory containing enemies.");
                        return;
                    }

                }
            }
        }

        boolean isNextToExistingClaim = false;
        if (!clan.isAdmin()) {
            for (int x = -1; x <= 1; x++) {
                for (int z = -1; z <= 1; z++) {
                    Chunk testedChunk = world.getChunkAt(chunk.getX() + x, chunk.getZ() + z);
                    Optional<Clan> nearbyClanOptional = clanManager.getClanByChunk(testedChunk);
                    if (nearbyClanOptional.isPresent()) {
                        Clan nearbyClan = nearbyClanOptional.get();
                        if (clan.equals(nearbyClan)) {
                            isNextToExistingClaim = true;
                            continue;
                        }
                        UtilMessage.message(player, "Clans", "You cannot claim next to enemy territory.");
                        return;
                    }

                }
            }
        }

        if (clan.getTerritory().size() > 0 && !isNextToExistingClaim && !clan.isAdmin()) {
            UtilMessage.message(player, "Clans", "You must claim next to your own territory");
            return;
        }


        UtilServer.callEvent(new ChunkClaimEvent(player, clan));

    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
