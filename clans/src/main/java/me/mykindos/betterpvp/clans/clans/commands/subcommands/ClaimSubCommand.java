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
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.SubCommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.locale.Translations;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.util.Optional;

@Singleton
@SubCommand(ClanCommand.class)
public class ClaimSubCommand extends ClanSubCommand {

    private final ZoneManager zoneManager;

    @Inject
    public ClaimSubCommand(ClanManager clanManager, ClientManager clientManager, ZoneManager zoneManager) {
        super(clanManager, clientManager);
        this.zoneManager = zoneManager;
        aliases.add("c");
    }

    @Override
    public String getName() {
        return "claim";
    }

    @Override
    public String getDescription() {
        return "clans.command.claim.description";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        if (player.getWorld().getName().equalsIgnoreCase(BPvPWorld.BOSS_WORLD_NAME)) return;

        Clan clan = clanManager.getClanByPlayer(player).orElseThrow();

        if (!clan.getMember(player.getUniqueId()).hasRank(ClanMember.MemberRank.ADMIN)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.claim.no-rank");
            return;
        }

        if (player.getWorld().getEnvironment().equals(World.Environment.NETHER) && !client.isAdministrating()) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.claim.nether");
            return;
        }

        if (clan.getTerritory().size() >= clanManager.getMaximumClaimsForClan(clan)) {
            if (!client.isAdministrating()) {
                UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.claim.limit");
                return;
            } else {
                Component notification = Translations.component("clans.command.clan.claim.limit-mod-notification",
                        Component.text(player.getName(), NamedTextColor.YELLOW),
                        Component.text(clan.getName(), NamedTextColor.YELLOW));
                clientManager.getPlayersOfRank(Rank.TRIAL_MOD).forEach(target -> {
                    UtilMessage.message(target, Translations.component(CLANS_PREFIX), notification);
                });
            }
        }

        Optional<Clan> locationClanOptional = clanManager.getClanByLocation(player.getLocation());
        if (locationClanOptional.isPresent()) {
            Clan locationClan = locationClanOptional.get();
            if (locationClan.equals(clan)) {
                UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.claim.already-owned");
            } else {
                UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.claim.other-clan-owned", Component.text(locationClan.getName(), NamedTextColor.YELLOW));
            }
            return;
        }

        final Zone zoneAt = zoneManager.getZoneAt(player.getLocation());
        if (zoneAt != null) {
            UtilMessage.message(player, "Clans", "You cannot claim here.");
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
                        UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.claim.enemies-present");
                        return;
                    }

                }
            }
        }

        if (clanManager.adjacentToWorldBorder(chunk)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.claim.world-border");
            return;
        }

        if (clanManager.adjacentOtherClans(player.getChunk(), clan)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.claim.adjacent-enemy");
            return;
        }

        if (!chunk.getWorld().getName().equalsIgnoreCase(BPvPWorld.MAIN_WORLD_NAME)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.claim.not-main-world");
            return;
        }

        if (!clan.getTerritory().isEmpty() && !clanManager.adjacentToOwnClan(player.getChunk(), clan)) {
            UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.claim.not-adjacent-own");
            return;
        }

        long claimCooldown = clanManager.getRemainingClaimCooldown(chunk);
        if (claimCooldown > 0) {
            if (!client.isAdministrating()) {
                UtilMessage.message(player, CLANS_PREFIX, "clans.command.clan.claim.cooldown", Component.text(UtilTime.getTime(claimCooldown, 1), NamedTextColor.GREEN));
                return;
            }

            Component notification = Translations.component("clans.command.clan.claim.cooldown-mod-notification",
                    Component.text(player.getName(), NamedTextColor.YELLOW),
                    Component.text(clan.getName(), NamedTextColor.YELLOW));
            clientManager.getPlayersOfRank(Rank.TRIAL_MOD).forEach(target -> {
                UtilMessage.message(target, Translations.component(CLANS_PREFIX), notification);
            });
        }


        UtilServer.callEvent(new ChunkClaimEvent(player, clan));

    }

    @Override
    public ClanMember.MemberRank getRequiredMemberRank() {
        return ClanMember.MemberRank.ADMIN;
    }
}
