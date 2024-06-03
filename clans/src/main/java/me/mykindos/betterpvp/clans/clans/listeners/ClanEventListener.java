package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.clans.clans.data.ClanDefaultValues;
import me.mykindos.betterpvp.clans.clans.events.ChunkClaimEvent;
import me.mykindos.betterpvp.clans.clans.events.ChunkUnclaimEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanAllianceEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanCreateEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanDisbandEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanEnemyEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanInviteMemberEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanKickMemberEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanNeutralEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanRequestAllianceEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanRequestNeutralEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanRequestTrustEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanSetHomeEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanTrustEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanUntrustEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberDemoteEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberJoinClanEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberLeaveClanEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberPromoteEvent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.CommandManager;
import me.mykindos.betterpvp.core.command.ICommand;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.inviting.InviteHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
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
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.Optional;
import java.util.UUID;

@CustomLog
@BPvPListener
public class ClanEventListener extends ClanListener {

    private final InviteHandler inviteHandler;
    private final WorldBlockHandler blockHandler;
    private final Clans clans;
    private final CommandManager commandManager;


    @Inject
    @Config(path = "clans.members.max", defaultValue = "8")
    private int maxClanMembers;

    @Inject
    public ClanEventListener(Clans clans, ClanManager clanManager, ClientManager clientManager, InviteHandler inviteHandler,
                             WorldBlockHandler blockHandler, CommandManager commandManager) {
        super(clanManager, clientManager);
        this.clans = clans;
        this.inviteHandler = inviteHandler;
        this.blockHandler = blockHandler;
        this.commandManager = commandManager;
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

        String stringChunk = UtilWorld.chunkToPrettyString(chunk);
        UtilMessage.simpleMessage(player, "Clans", "You claimed Territory <yellow>" + stringChunk + "</yellow>.");

        clan.messageClan(String.format("<yellow>%s<gray> claimed territory <yellow>%s<gray>.", player.getName(),
                stringChunk), player.getUniqueId(), true);

        blockHandler.outlineChunk(chunk);
        log.info("{} ({}) claimed {} for {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        stringChunk, clan.getName(), clan.getId())
                .setAction("CLAN_CLAIM").addClientContext(event.getPlayer()).addClanContext(clan)
                .addContext(LogContext.CHUNK, stringChunk).submit();

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnclaim(ChunkUnclaimEvent event) {
        if (event.isCancelled()) return;

        Player player = event.getPlayer();
        Clan targetClan = event.getClan();
        Chunk chunk = event.getChunk();

        String chunkString = UtilWorld.chunkToFile(chunk);

        String chunkToPrettyString = UtilWorld.chunkToPrettyString(chunk);
        UtilMessage.simpleMessage(player, "Clans", "You unclaimed territory <alt2>" + chunkToPrettyString + "</alt2>.");

        targetClan.messageClan(String.format("<yellow>%s<gray> unclaimed territory <yellow>%s<gray>.", player.getName(),
                chunkToPrettyString), player.getUniqueId(), true);
        clanManager.getRepository().deleteClanTerritory(targetClan, chunkString);
        targetClan.getTerritory().removeIf(territory -> territory.getChunk().equals(UtilWorld.chunkToFile(chunk)));

        if (targetClan.getHome() != null) {
            if (targetClan.getHome().getChunk().equals(chunk)) {
                Block block = targetClan.getHome().clone().subtract(0, 0.6, 0).getBlock();
                if (block.getType() == Material.RED_BED) {
                    block.setType(Material.AIR);
                }
                targetClan.setHome(null);

                targetClan.messageClan("Your clan home was destroyed!", null, true);
            }
        }

        log.info("{} ({}) unclaimed {} from {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        chunkToPrettyString, targetClan.getName(), targetClan.getId())
                .setAction("CLAN_UNCLAIM").addClientContext(event.getPlayer()).addClanContext(targetClan).
                addContext(LogContext.CHUNK, chunkToPrettyString).submit();

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanCreate(ClanCreateEvent event) {

        Clan clan = event.getClan();
        ICommand clanCommand = commandManager.getCommand("clan").orElseThrow();

        for (ICommand subCommand : clanCommand.getSubCommands()) {

            if (subCommand.getName().equalsIgnoreCase(clan.getName()) || subCommand.getAliases().stream().anyMatch(o -> o.equalsIgnoreCase(clan.getName()))) {
                UtilMessage.message(event.getPlayer(), "Command", "Clan name cannot be a clan's subcommand name or alias");
                return;
            }
        }

        clan.getMembers().add(new ClanMember(event.getPlayer().getUniqueId().toString(), ClanMember.MemberRank.LEADER));
        event.getPlayer().setMetadata("clan", new FixedMetadataValue(clans, clan.getId()));

        clanManager.addObject(clan.getId().toString(), clan);
        clanManager.getRepository().save(clan);
        clanManager.getLeaderboard().forceUpdate();

        var defaultValues = clans.getInjector().getInstance(ClanDefaultValues.class);
        clan.saveProperty(ClanProperty.TIME_CREATED, System.currentTimeMillis());
        clan.saveProperty(ClanProperty.LAST_LOGIN, System.currentTimeMillis());
        clan.saveProperty(ClanProperty.POINTS, defaultValues.getDefaultPoints());
        clan.saveProperty(ClanProperty.ENERGY, defaultValues.getDefaultEnergy());
        clan.saveProperty(ClanProperty.LAST_TNTED, 0L);
        clan.saveProperty(ClanProperty.EXPERIENCE, 0d);
        clan.saveProperty(ClanProperty.BALANCE, 0);
        clan.saveProperty(ClanProperty.NO_DOMINANCE_COOLDOWN, (System.currentTimeMillis() + (3_600_000L * 24)));

        UtilMessage.simpleMessage(event.getPlayer(), "Clans", "Successfully created clan <aqua>%s", clan.getName());
        if (clan.isAdmin()) {
            clientManager.sendMessageToRank("Clans", UtilMessage.deserialize("<yellow>%s<gray> created admin clan <yellow>%s", event.getPlayer().getName(), clan.getName()), Rank.HELPER);

            log.info("{} ({}) created admin clan {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(), clan.getName(), clan.getId())
                    .setAction("CLAN_CREATE").addClientContext(event.getPlayer()).addClanContext(clan).submit();
        } else {
            log.info("{} ({}) created {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(), clan.getName(), clan.getId())
                    .setAction("CLAN_CREATE").addClientContext(event.getPlayer()).addClanContext(clan).submit();

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

        if (clan.getTerritory().isEmpty()) {
            UtilMessage.broadcast("Clans", "<alt2>Clan " + clan.getName() + "</alt2> has been disbanded.");
        } else {
            Chunk chunk = UtilWorld.stringToChunk(clan.getTerritory().get(0).getChunk());
            if (chunk != null) {
                UtilMessage.broadcast("Clans", "<alt2>Clan " + clan.getName() + "</alt2> has been disbanded. (<yellow>%s</yellow>)",
                        (chunk.getX() * 16) + "<gray>,</gray> " + (chunk.getZ() * 16));
            }
        }

        event.getClan().getMembers().forEach(member -> {
            Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
            if (player != null) {
                player.removeMetadata("clan", clans);
            }
        });

        clan.getMembers().clear();
        clan.getTerritory().clear();
        clan.getEnemies().clear();
        clan.getAlliances().clear();

        clanManager.getRepository().delete(clan);
        clanManager.getObjects().remove(clan.getId().toString());
        clanManager.getLeaderboard().forceUpdate();

        if(event.getPlayer() != null) {
            log.info("{} ({}) disbanded {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(), clan.getName(), clan.getId())
                    .setAction("CLAN_DISBAND").addClientContext(event.getPlayer()).addClanContext(clan).submit();
        }else {
            log.info("System disbanded {} ({}) for running out of energy", clan.getName(), clan.getId())
                    .setAction("CLAN_DISBAND").addClanContext(clan).submit();
        }

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

        Gamer targetGamer = clientManager.search().online(target).getGamer();
        inviteHandler.createInvite(clan, targetGamer, "Invite", 20);
        log.info("{} ({}) invited {} ({}) to {} ({})", player.getName(), player.getUniqueId(),
                        target.getName(), target.getUniqueId(), clan.getName(), clan.getId()).setAction("CLAN_INVITE")
                .addClientContext(target, true).addClientContext(event.getPlayer()).addClanContext(clan).submit();

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onJoinClanEvent(MemberJoinClanEvent event) {
        if (event.isCancelled()) return;

        Clan clan = event.getClan();
        Player player = event.getPlayer();
        Client client = clientManager.search().online(player);
        final Gamer gamer = client.getGamer();

        if (!client.isAdministrating()) {
            if (!inviteHandler.isInvited(gamer, clan, "Invite")) {
                UtilMessage.simpleMessage(player, "Clans", "You are not invited to <alt2>Clan " + clan.getName() + "</alt2>.");
                return;
            }

            if (clan.getSquadCount() >= maxClanMembers) {
                UtilMessage.simpleMessage(player, "Clans", "<alt2>Clan " + clan.getName() + "</alt2> has too many members or allies");
                return;
            }
        } else {
            clientManager.sendMessageToRank("Clans", UtilMessage.deserialize("<yellow>%s<gray> force joined <yellow>%s", player.getName(), clan.getName()), Rank.HELPER);
        }

        ClanMember member = new ClanMember(player.getUniqueId().toString(),
                client.isAdministrating() ? ClanMember.MemberRank.LEADER : ClanMember.MemberRank.RECRUIT);
        clan.getMembers().add(member);
        player.setMetadata("clan", new FixedMetadataValue(clans, clan.getId()));

        clanManager.getRepository().saveClanMember(clan, member);


        inviteHandler.removeInvite(clan, gamer, "Invite");
        inviteHandler.removeInvite(gamer, clan, "Invite");

        clan.setOnline(true);
        clan.messageClan(String.format("<yellow>%s<gray> has joined your Clan.", player.getName()), player.getUniqueId(), true);
        UtilMessage.simpleMessage(player, "Clans", "You joined <alt2>Clan " + clan.getName() + "</alt2>.");

        log.info("{} ({}) joined {} ({})", player.getName(), player.getUniqueId(), clan.getName(), clan.getId()).setAction("CLAN_JOIN")
                .addClientContext(player)
                .addClanContext(clan)
                .submit();

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
            player.removeMetadata("clan", clans);

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

        log.info("{} ({}) left {} ({})", player.getName(), player.getUniqueId(), clan.getName(), clan.getId())
                .setAction("CLAN_LEAVE").addClientContext(player).addClanContext(clan).submit();

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
            clan.messageClan(String.format("<yellow>%s<gray> was kicked from your Clan.", target.getName()), player.getUniqueId(), true);

            Player targetPlayer = Bukkit.getPlayer(target.getName());
            if (targetPlayer != null) {
                UtilMessage.simpleMessage(targetPlayer, "Clans", "You were kicked from <alt2>" + clan.getName());
                targetPlayer.closeInventory();


                targetPlayer.removeMetadata("clan", clans);

            }
        }

        log.info("{} ({}) was kicked by {} ({}) from {} ({})", target.getName(), target.getUuid(),
                        player.getName(), player.getUniqueId(), clan.getName(), clan.getId()).
                setAction("CLAN_KICK").addClientContext(player).addClientContext(target, true).addClanContext(clan).submit();
    }

    @EventHandler
    public void onClanRequestAlliance(ClanRequestAllianceEvent event) {
        if (event.isCancelled()) return;

        Clan clan = event.getClan();
        Clan target = event.getTargetClan();

        if (inviteHandler.isInvited(target, clan, "Alliance")) {
            UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You already have a pending alliance request with <yellow>%s<gray>.", target.getName());
            return;
        }

        if (inviteHandler.isInvited(clan, target, "Alliance")) {

            UtilServer.callEvent(new ClanAllianceEvent(event.getPlayer(), clan, target));
            return;
        }

        inviteHandler.createInvite(clan, target, "Alliance", 10);
        UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You have requested an alliance with <yellow>%s<gray>.", target.getName());
        target.messageClan("<yellow>Clan " + clan.getName() + "<gray> has requested an alliance.", null, true);

        log.info("{} ({}) of {} ({}) requested alliance with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_ALLIANCE_REQUEST")
                .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

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

        log.info("{} ({}) of {} ({}) accepted alliance with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_ALLIANCE_ACCEPT")
                .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

    }

    @EventHandler
    public void onClanRequestTrust(ClanRequestTrustEvent event) {
        if (event.isCancelled()) return;

        Clan clan = event.getClan();
        Clan target = event.getTargetClan();

        if (inviteHandler.isInvited(target, clan, "Trust")) {
            UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You already have a pending trust request with <yellow>%s<gray>.", target.getName());
            return;
        }

        if (inviteHandler.isInvited(clan, target, "Trust")) {

            UtilServer.callEvent(new ClanTrustEvent(event.getPlayer(), clan, target));
            return;
        }

        inviteHandler.createInvite(clan, target, "Trust", 10);
        UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You have requested to form a trust with <yellow>%s<gray>.", target.getName());
        target.messageClan("<yellow>Clan " + clan.getName() + "<gray> has requested to form a trust with your clan.", null, true);

        log.info("{} ({}) of {} ({}) requested trust with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_TRUST_REQUEST")
                .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

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

        log.info("{} ({}) of {} ({}) accepted trust with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_TRUST_ACCEPT")
                .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

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

        log.info("{} ({}) of {} ({}) removed trust with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_TRUST_REMOVE")
                .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

    }

    @EventHandler
    public void onClanRequestNeutral(ClanRequestNeutralEvent event) {
        if (event.isCancelled()) return;

        Clan clan = event.getClan();
        Clan target = event.getTargetClan();

        if (clan.isEnemy(target)) {

            if (inviteHandler.isInvited(target, clan, "Neutral")) {
                UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You already have a pending neutral request with <red>%s<gray>.", target.getName());
                return;
            }

            if (inviteHandler.isInvited(clan, target, "Neutral")) {

                UtilServer.callEvent(new ClanNeutralEvent(event.getPlayer(), clan, target));
                return;
            }

            inviteHandler.createInvite(clan, target, "Neutral", 10);
            UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You have requested to neutral with <yellow>%s<gray>.", target.getName());
            target.messageClan("<yellow>Clan " + clan.getName() + "<gray> has requested to neutral with your clan.", null, true);

            log.info("{} ({}) of {} ({}) requested neutral with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                            clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_NEUTRAL_REQUEST")
                    .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

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
            log.info("{} ({}) of {} ({}) removed alliance with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                            clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_ALLIANCE_REMOVE")
                    .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

        } else if (clan.isEnemy(target)) {
            removeEnemy(clan, target);
            log.info("{} ({}) of {} ({}) accepted neutral with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                            clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_NEUTRAL_ACCEPT")
                    .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

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
            log.info("{} ({}) of {} ({}) removed alliance with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                            clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_ALLIANCE_REMOVE")
                    .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

        }

        ClanEnemy clanEnemy = new ClanEnemy(target, 0);
        ClanEnemy targetEnemy = new ClanEnemy(clan, 0);

        clan.getEnemies().add(clanEnemy);
        target.getEnemies().add(targetEnemy);

        clanManager.getRepository().saveClanEnemy(clan, clanEnemy);
        clanManager.getRepository().saveClanEnemy(target, targetEnemy);

        clan.messageClan("<yellow>Clan " + target.getName() + "<gray> is now your enemy.", null, true);
        target.messageClan("<yellow>Clan " + clan.getName() + "<gray> is now your enemy.", null, true);

        log.info("{} ({}) of {} ({}) enemied with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_ENEMY")
                .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();


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

        if (clan.getHome() != null) {
            Block block = clan.getHome().clone().subtract(0, 0.6, 0).getBlock();
            if (block.getType() == Material.RED_BED) {
                block.setType(Material.AIR);
            }
        }

        if (!clan.isAdmin()) {
            UtilBlock.placeBed(player.getLocation().toCenterLocation(), player.getFacing());
        }

        clan.setHome(player.getLocation().toCenterLocation().add(0, 0.6, 0));
        UtilMessage.simpleMessage(player, "Clans", "You set the clan home to <yellow>%s<gray>.",
                UtilWorld.locationToString(player.getLocation()));
        log.info("{} ({}) of {} ({}) set their clan home to {}", player.getName(), player.getUniqueId(), clan.getName(), clan.getName(),
                        UtilWorld.locationToString(player.getLocation(), true)).setAction("CLAN_SETHOME")
                .addClientContext(player).addClanContext(clan).addLocationContext(player.getLocation()).submit();

        clanManager.getRepository().updateClanHome(clan);


    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMemberPromote(MemberPromoteEvent event) {
        if (event.isCancelled()) return;
        Player player = event.getPlayer();
        Clan clan = event.getClan();
        ClanMember member = event.getClanMember();

        ClanMember.MemberRank rank = member.getRank();

        member.setRank(ClanMember.MemberRank.getRankByPrivilege(Math.min(ClanMember.MemberRank.LEADER.getPrivilege(), member.getRank().getPrivilege() + 1)));
        clanManager.getRepository().updateClanMemberRank(clan, member);

        clientManager.search().offline(UUID.fromString(member.getUuid()), result -> {
            result.ifPresent(client -> {
                UtilMessage.simpleMessage(player, "Clans", "You promoted <aqua>%s<gray> to <yellow>%s<gray>.", client.getName(), member.getRank().getName());

                log.info("{} ({}) was promoted by {} ({}) to {} in {} ({})", client.getName(), member.getUuid(), player.getName(),
                                player.getUniqueId(), member.getRank().getName(), clan.getName(), clan.getId()).setAction("CLAN_PROMOTE")
                        .addClientContext(player).addClientContext(client, true).addClanContext(clan)
                        .addContext(LogContext.CURRENT_CLAN_RANK, rank.getName()).addContext(LogContext.NEW_CLAN_RANK, member.getRank().getName()).submit();

            });
        });


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

        ClanMember.MemberRank rank = member.getRank();

        member.setRank(ClanMember.MemberRank.getRankByPrivilege(Math.max(1, member.getRank().getPrivilege() - 1)));
        clanManager.getRepository().updateClanMemberRank(clan, member);

        clientManager.search().offline(UUID.fromString(member.getUuid()), result -> {
            result.ifPresent(client -> {
                UtilMessage.simpleMessage(player, "Clans", "You demoted <aqua>%s<gray> to <yellow>%s<gray>.", client.getName(), member.getRank().getName());
                log.info("{} ({}) was demoted by {} ({}) to {} in {} ({})", client.getName(), member.getUuid(),
                                player.getName(), player.getUniqueId(), member.getRank().getName(), clan.getName(), clan.getId())
                        .setAction("CLAN_DEMOTE").addClientContext(player).addClientContext(client, true).addClanContext(clan)
                        .addContext(LogContext.CURRENT_CLAN_RANK, rank.getName()).addContext(LogContext.NEW_CLAN_RANK, member.getRank().getName()).submit();

            });
        });

        Player memberPlayer = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
        if (memberPlayer != null) {
            UtilMessage.simpleMessage(memberPlayer, "Clans", "You were demoted to <yellow>%s<gray>.", member.getName());
        }
    }
}
