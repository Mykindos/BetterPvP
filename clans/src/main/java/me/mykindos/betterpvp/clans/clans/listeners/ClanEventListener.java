package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.clans.clans.data.ClanDefaultValues;
import me.mykindos.betterpvp.clans.clans.events.*;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.components.clans.events.ClanEvent;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.framework.inviting.InviteHandler;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.gamer.exceptions.NoSuchGamerException;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;

import java.util.Optional;
import java.util.UUID;

@BPvPListener
public class ClanEventListener extends ClanListener {

    private final InviteHandler inviteHandler;
    private final WorldBlockHandler blockHandler;
    private final Clans clans;

    @Inject
    @Config(path = "clans.members.max", defaultValue = "6")
    private int maxClanMembers;

    @Inject
    public ClanEventListener(Clans clans, ClanManager clanManager, GamerManager gamerManager, InviteHandler inviteHandler,
                             WorldBlockHandler blockHandler) {
        super(clanManager, gamerManager);
        this.clans = clans;
        this.inviteHandler = inviteHandler;
        this.blockHandler = blockHandler;
    }

    @EventHandler
    public void onClanEvent(ClanEvent<Clan> event) {
        if (event.isGlobalScoreboardUpdate()) {
            Bukkit.getOnlinePlayers().forEach(player -> UtilServer.runTaskLater(clans,
                    () -> UtilServer.callEvent(new ScoreboardUpdateEvent(player)), 5));
        } else {
            UtilServer.runTaskLater(clans, () -> UtilServer.callEvent(new ScoreboardUpdateEvent(event.getPlayer())), 5);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkClaim(ChunkClaimEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Clan clan = event.getClan();
        Chunk chunk = event.getChunk();

        String chunkString = UtilWorld.chunkToFile(chunk);
        clan.getTerritory().add(new ClanTerritory(chunkString));
        clanManager.getRepository().saveClanTerritory(clan, chunkString);

        UtilMessage.simpleMessage(player, "Clans", "You claimed Territory <yellow>" + UtilWorld.chunkToPrettyString(chunk) + "</yellow>.");

        clan.messageClan(String.format("<yellow>%s<gray> claimed territory <yellow>%s<gray>.", player.getName(),
                UtilWorld.chunkToPrettyString(chunk)), player.getUniqueId(), true);

        blockHandler.outlineChunk(chunk);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnclaim(ChunkUnclaimEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Clan targetClan = event.getClan();
        Chunk chunk = event.getChunk();

        String chunkString = UtilWorld.chunkToFile(chunk);

        UtilMessage.simpleMessage(player, "Clans", "You unclaimed territory <alt2>" + UtilWorld.chunkToPrettyString(chunk) + "</alt2>.");

        targetClan.messageClan(String.format("<yellow>%s<gray> unclaimed territory <yellow>%s<gray>.", player.getName(),
                UtilWorld.chunkToPrettyString(chunk)), player.getUniqueId(), true);
        clanManager.getRepository().deleteClanTerritory(targetClan, chunkString);
        targetClan.getTerritory().removeIf(territory -> territory.getChunk().equals(UtilWorld.chunkToFile(chunk)));
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanCreate(ClanCreateEvent event) {

        Clan clan = event.getClan();

        clan.getMembers().add(new ClanMember(event.getPlayer().getUniqueId().toString(), ClanMember.MemberRank.LEADER));

        clanManager.addObject(clan.getName().toLowerCase(), clan);
        clanManager.getRepository().save(clan);

        var defaultValues = clans.getInjector().getInstance(ClanDefaultValues.class);
        clan.saveProperty(ClanProperty.TIME_CREATED, System.currentTimeMillis());
        clan.saveProperty(ClanProperty.LAST_LOGIN, System.currentTimeMillis());
        clan.saveProperty(ClanProperty.LEVEL, defaultValues.getDefaultLevel());
        clan.saveProperty(ClanProperty.POINTS, defaultValues.getDefaultPoints());
        clan.saveProperty(ClanProperty.ENERGY, defaultValues.getDefaultEnergy());
        clan.saveProperty(ClanProperty.NO_DOMINANCE_COOLDOWN, System.currentTimeMillis() + (3_600_000L * 24));
        clan.saveProperty(ClanProperty.LAST_TNTED, 0L);
        clan.saveProperty(ClanProperty.BALANCE, 0);

        UtilMessage.simpleMessage(event.getPlayer(), "Clans", "Successfully created clan <aqua>%s", clan.getName());
        if (clan.isAdmin()) {
            gamerManager.sendMessageToRank("Clans", UtilMessage.deserialize("<yellow>%s<gray> created admin clan <yellow>%s", event.getPlayer().getName(), clan.getName()), Rank.HELPER);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanDisband(ClanDisbandEvent event) {

        if (event.isCancelled()) {
            return;
        }

        Clan clan = event.getClan();

        for (ClanAlliance alliance : clan.getAlliances()) {
            alliance.getClan().getAlliances().removeIf(ally -> ally.getClan().getName().equalsIgnoreCase(clan.getName()));
        }

        for (ClanEnemy enemy : clan.getEnemies()) {
            enemy.getClan().getEnemies().removeIf(en -> en.getClan().getName().equalsIgnoreCase(clan.getName()));
        }

        clan.getMembers().clear();
        clan.getTerritory().clear();
        clan.getEnemies().clear();
        clan.getAlliances().clear();

        clanManager.getRepository().delete(clan);
        clanManager.getObjects().remove(clan.getName());

        //UtilMessage.broadcast("Clans", "<alt2>" + event.getPlayer().getName() + "</alt2> disbanded <alt2>Clan " + clan.getName() + "</alt2>.");
        UtilMessage.broadcast("Clans", "<alt2>Clan " + clan.getName() + "</alt2> has been disbanded.");
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanInviteMember(ClanInviteMemberEvent event) {
        if (event.isCancelled()) return;

        Clan clan = event.getClan();
        Player player = event.getPlayer();
        Player target = event.getTarget();


        UtilMessage.simpleMessage(player, "Clans", "You invited <alt2>" + target.getName() + "</alt2> to join your Clan.");

        clan.messageClan(String.format("<yellow>%s<gray> invited <yellow>%s<gray> to join your Clan.", player.getName(), target.getName()), player.getUniqueId(), true);

        UtilMessage.simpleMessage(target, "Clans", "<alt2>" + player.getName() + "</alt2> invited you to join <alt2>Clan " + clan.getName() + "</alt2>.");

        Component inviteMessage = Component.text("Click Here", NamedTextColor.GOLD, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand("/c join " + clan.getName()))
                .append(UtilMessage.deserialize(" or type '<alt2>/c join " + clan.getName() + "</alt2>' to accept!"));
        UtilMessage.simpleMessage(target, "Clans", inviteMessage);

        Gamer targetGamer = gamerManager.getObject(target.getUniqueId().toString()).orElseThrow(() -> new NoSuchGamerException(target.getName()));
        inviteHandler.createInvite(clan, targetGamer, "Invite", 20);

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoinClanEvent(MemberJoinClanEvent event) {
        if (event.isCancelled()) return;

        Clan clan = event.getClan();
        Player player = event.getPlayer();
        Gamer targetGamer = gamerManager.getObject(player.getUniqueId().toString()).orElseThrow(() -> new NoSuchGamerException(player.getName()));

        if (!targetGamer.getClient().isAdministrating()) {
            if (!inviteHandler.isInvited(targetGamer, clan, "Invite")) {
                UtilMessage.simpleMessage(player, "Clans", "You are not invited to <alt2>Clan " + clan.getName() + "</alt2>.");
                return;
            }

            if (clan.getSquadCount() >= maxClanMembers) {
                UtilMessage.simpleMessage(player, "Clans", "<alt2>Clan " + clan.getName() + "</alt2> has too many members or allies");
                return;
            }
        }
        else {
            gamerManager.sendMessageToRank("Clans", UtilMessage.deserialize("<yellow>%s<gray> force joined <yellow>%s", player.getName(), clan.getName()), Rank.HELPER);
        }

        ClanMember member = new ClanMember(player.getUniqueId().toString(),
                targetGamer.getClient().isAdministrating() ? ClanMember.MemberRank.LEADER : ClanMember.MemberRank.RECRUIT);
        clan.getMembers().add(member);
        clanManager.getRepository().saveClanMember(clan, member);

        inviteHandler.removeInvite(clan, targetGamer, "Invite");
        inviteHandler.removeInvite(targetGamer, clan, "Invite");

        clan.setOnline(true);
        clan.messageClan(String.format("<yellow>%s<gray> has joined your Clan.", player.getName()), player.getUniqueId(), true);
        UtilMessage.simpleMessage(player, "Clans", "You joined <alt2>Clan " + clan.getName() + "</alt2>.");

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeaveClanEvent(MemberLeaveClanEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Clan clan = event.getClan();

        Optional<ClanMember> memberOptional = clan.getMemberByUUID(player.getUniqueId());
        if (memberOptional.isPresent()) {
            ClanMember clanMember = memberOptional.get();

            clanManager.getRepository().deleteClanMember(clan, clanMember);
            clan.getMembers().remove(clanMember);

            UtilMessage.simpleMessage(player, "Clans", "You left <alt2>Clan " + clan.getName() + "</alt2>.");

            boolean isOnline = false;
            for (ClanMember member : clan.getMembers()) {
                Player playerMember = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
                if (playerMember != null) {
                    UtilMessage.message(playerMember, "Clans", "<yellow>%s<gray> left your Clan.", player.getName());
                    isOnline = true;
                }
            }
            clan.setOnline(isOnline);
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMemberKicked(ClanKickMemberEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Clan clan = event.getClan();
        Client target = event.getTarget();

        Optional<ClanMember> memberOptional = clan.getMemberByUUID(target.getUuid());
        if (memberOptional.isPresent()) {
            ClanMember clanMember = memberOptional.get();

            clanManager.getRepository().deleteClanMember(clan, clanMember);
            clan.getMembers().remove(clanMember);

            UtilMessage.simpleMessage(player, "Clans", "You kicked <alt2>" + target.getName() + "</alt2>.");
            clan.messageClan(String.format("<yellow>%s<gray> was kicked from your Clan.", player.getName()), player.getUniqueId(), true);

            Player targetPlayer = Bukkit.getPlayer(target.getName());
            if (targetPlayer != null) {
                UtilMessage.simpleMessage(targetPlayer, "Clans", "You were kicked from <alt2>" + clan.getName());
            }
        }

    }

    @EventHandler
    public void onClanRequestAlliance(ClanRequestAllianceEvent event) {
        if (event.isCancelled()) return;

        Clan clan = event.getClan();
        Clan target = event.getTargetClan();

        if (inviteHandler.isInvited(clan, target, "Alliance") || inviteHandler.isInvited(target, clan, "Alliance")) {

            UtilServer.callEvent(new ClanAllianceEvent(event.getPlayer(), clan, target));
            return;
        }

        inviteHandler.createInvite(clan, target, "Alliance", 10);
        UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You have requested an alliance with <yellow>%s<gray>.", target.getName());
        target.messageClan("<yellow>Clan " + clan.getName() + "<gray> has requested an alliance.", null, true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanAlly(ClanAllianceEvent event) {
        if (event.isCancelled()) return;
        Clan clan = event.getClan();
        Clan target = event.getTargetClan();

        inviteHandler.removeInvite(clan, target, "Alliance");
        inviteHandler.removeInvite(target, clan, "Alliance");

        ClanAlliance clanAlliance = new ClanAlliance(target, false);
        ClanAlliance targetAlliance = new ClanAlliance(clan, false);
        clan.getAlliances().add(clanAlliance);
        target.getAlliances().add(targetAlliance);

        clan.messageClan("<yellow>Clan " + target.getName() + "<gray> is now your ally.", null, true);
        target.messageClan("<yellow>Clan " + clan.getName() + "<gray> is now your ally.", null, true);

        clanManager.getRepository().saveClanAlliance(clan, clanAlliance);
        clanManager.getRepository().saveClanAlliance(target, targetAlliance);
    }

    @EventHandler
    public void onClanRequestTrust(ClanRequestTrustEvent event) {
        if (event.isCancelled()) return;

        Clan clan = event.getClan();
        Clan target = event.getTargetClan();

        if (inviteHandler.isInvited(clan, target, "Trust") || inviteHandler.isInvited(target, clan, "Trust")) {

            UtilServer.callEvent(new ClanTrustEvent(event.getPlayer(), clan, target));
            return;
        }

        inviteHandler.createInvite(clan, target, "Trust", 10);
        UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You have requested to form a trust with <yellow>%s<gray>.", target.getName());
        target.messageClan("<yellow>Clan " + clan.getName() + "<gray> has requested to form a trust with your clan.", null, true);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanTrust(ClanTrustEvent event) {
        if (event.isCancelled()) return;
        Clan clan = event.getClan();
        Clan target = event.getTargetClan();

        inviteHandler.removeInvite(clan, target, "Trust");
        inviteHandler.removeInvite(target, clan, "Trust");

        ClanAlliance clanAlliance = event.getClan().getAlliance(target).orElseThrow();
        ClanAlliance targetAlliance = event.getTargetClan().getAlliance(clan).orElseThrow();
        clanAlliance.setTrusted(true);
        targetAlliance.setTrusted(true);

        clan.messageClan("<yellow>Clan " + target.getName() + "<gray> is now a trusted ally.", null, true);
        target.messageClan("<yellow>Clan " + clan.getName() + "<gray> is now a trusted ally.", null, true);

        clanManager.getRepository().saveTrust(clan, clanAlliance);
        clanManager.getRepository().saveTrust(target, targetAlliance);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanUntrust(ClanUntrustEvent event) {
        if (event.isCancelled()) return;
        Clan clan = event.getClan();
        Clan target = event.getTargetClan();

        ClanAlliance clanAlliance = event.getClan().getAlliance(target).orElseThrow();
        ClanAlliance targetAlliance = event.getTargetClan().getAlliance(clan).orElseThrow();
        clanAlliance.setTrusted(false);
        targetAlliance.setTrusted(false);

        clan.messageClan("<yellow>Clan " + target.getName() + "<gray> is no longer a trusted ally.", null, true);
        target.messageClan("<yellow>Clan " + clan.getName() + "<gray> is no longer a trusted ally.", null, true);

        clanManager.getRepository().saveTrust(clan, clanAlliance);
        clanManager.getRepository().saveTrust(target, targetAlliance);
    }

    @EventHandler
    public void onClanRequestNeutral(ClanRequestNeutralEvent event) {
        if (event.isCancelled()) return;

        Clan clan = event.getClan();
        Clan target = event.getTargetClan();

        if (clan.isEnemy(target)) {
            if (inviteHandler.isInvited(clan, target, "Neutral") || inviteHandler.isInvited(target, clan, "Neutral")) {

                UtilServer.callEvent(new ClanNeutralEvent(event.getPlayer(), clan, target));
                return;
            }

            inviteHandler.createInvite(clan, target, "Neutral", 10);
            UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You have requested to neutral with <yellow>%s<gray>.", target.getName());
            target.messageClan("<yellow>Clan " + clan.getName() + "<gray> has requested to neutral with your clan.", null, true);
        } else {
            UtilServer.callEvent(new ClanNeutralEvent(event.getPlayer(), clan, target));
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanNeutral(ClanNeutralEvent event) {
        if (event.isCancelled()) return;
        Clan clan = event.getClan();
        Clan target = event.getTargetClan();

        inviteHandler.removeInvite(clan, target, "Neutral");
        inviteHandler.removeInvite(target, clan, "Neutral");

        if (clan.isAllied(target)) {
            removeAlliance(clan, target);
        } else if (clan.isEnemy(target)) {
            removeEnemy(clan, target);
        }

        clan.messageClan("<yellow>Clan " + target.getName() + "<gray> is now neutral to your Clan.", null, true);
        target.messageClan("<yellow>Clan " + clan.getName() + "<gray> is now neutral to your Clan.", null, true);

    }

    private void removeAlliance(Clan clan, Clan target) {
        ClanAlliance clanAlliance = clan.getAlliance(target).orElseThrow();
        ClanAlliance targetAlliance = target.getAlliance(clan).orElseThrow();
        clanManager.getRepository().deleteClanAlliance(clan, clanAlliance);
        clanManager.getRepository().deleteClanAlliance(target, targetAlliance);
        clan.getAlliances().remove(clanAlliance);
        target.getAlliances().remove(targetAlliance);
    }

    private void removeEnemy(Clan clan, Clan target) {
        ClanEnemy clanEnemy = clan.getEnemy(target).orElseThrow();
        ClanEnemy targetEnemy = target.getEnemy(clan).orElseThrow();
        clanManager.getRepository().deleteClanEnemy(clan, clanEnemy);
        clanManager.getRepository().deleteClanEnemy(target, targetEnemy);
        clan.getEnemies().remove(clanEnemy);
        target.getEnemies().remove(targetEnemy);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanEnemy(ClanEnemyEvent event) {
        if (event.isCancelled()) return;
        Clan clan = event.getClan();
        Clan target = event.getTargetClan();

        if (clan.isEnemy(target)) {
            return;
        }

        if (clan.isAllied(target)) {
            removeAlliance(clan, target);
        }

        ClanEnemy clanEnemy = new ClanEnemy(target, 0);
        ClanEnemy targetEnemy = new ClanEnemy(clan, 0);

        clan.getEnemies().add(clanEnemy);
        target.getEnemies().add(targetEnemy);

        clanManager.getRepository().saveClanEnemy(clan, clanEnemy);
        clanManager.getRepository().saveClanEnemy(target, targetEnemy);

        clan.messageClan("<yellow>Clan " + target.getName() + "<gray> is now your enemy.", null, true);
        target.messageClan("<yellow>Clan " + clan.getName() + "<gray> is now your enemy.", null, true);

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanSetHome(ClanSetHomeEvent event) {
        if (event.isCancelled()) return;

        Clan clan = event.getClan();
        Player player = event.getPlayer();

        Optional<Clan> clanOptional = clanManager.getClanByLocation(player.getLocation());
        if (clanOptional.isEmpty() || !clanOptional.get().equals(clan)) {
            UtilMessage.simpleMessage(player, "Clans", "You can only set the clan home in your own territory.");
            return;
        }

        clan.setHome(player.getLocation());
        UtilMessage.simpleMessage(player, "Clans", "You set the clan home to <yellow>%s<gray>.",
                UtilWorld.locationToString(player.getLocation()));

        clanManager.getRepository().updateClanHome(clan);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMemberPromote(MemberPromoteEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Clan clan = event.getClan();
        ClanMember member = event.getClanMember();

        member.setRank(ClanMember.MemberRank.getRankByPrivilege(Math.min(ClanMember.MemberRank.LEADER.getPrivilege(), member.getRank().getPrivilege() + 1)));
        clanManager.getRepository().updateClanMemberRank(clan, member);

        Gamer memberGamer = gamerManager.getObject(member.getUuid()).orElseThrow(() -> new NoSuchGamerException(member.getUuid()));

        UtilMessage.simpleMessage(player, "Clans", "You promoted <aqua>%s<gray> to <yellow>%s<gray>.",
                memberGamer.getClient().getName(), member.getName());

        Player memberPlayer = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
        if (memberPlayer != null) {
            UtilMessage.simpleMessage(memberPlayer, "Clans", "You were promoted to <yellow>%s<gray>.", member.getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMemberDemote(MemberDemoteEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Clan clan = event.getClan();
        ClanMember member = event.getClanMember();

        member.setRank(ClanMember.MemberRank.getRankByPrivilege(Math.max(1, member.getRank().getPrivilege() - 1)));
        clanManager.getRepository().updateClanMemberRank(clan, member);

        Gamer memberGamer = gamerManager.getObject(member.getUuid()).orElseThrow(() -> new NoSuchGamerException(member.getUuid()));

        if (!player.getUniqueId().toString().equalsIgnoreCase(member.getUuid())) {
            UtilMessage.simpleMessage(player, "Clans", "You demoted <aqua>%s<gray> to <yellow>%s<gray>.",
                    memberGamer.getClient().getName(), member.getName());
        }

        Player memberPlayer = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
        if (memberPlayer != null) {
            UtilMessage.simpleMessage(memberPlayer, "Clans", "You were demoted to <yellow>%s<gray>.", member.getName());
        }
    }
}
