package me.mykindos.betterpvp.clans.clans.listeners;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.ClanProperty;
import me.mykindos.betterpvp.clans.clans.ClanRelation;
import me.mykindos.betterpvp.clans.clans.core.ClanCore;
import me.mykindos.betterpvp.clans.clans.core.mailbox.ClanMailbox;
import me.mykindos.betterpvp.clans.clans.core.vault.ClanVault;
import me.mykindos.betterpvp.clans.clans.data.ClanDefaultValues;
import me.mykindos.betterpvp.clans.clans.events.*;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.offlinemessages.OfflineMessage;
import me.mykindos.betterpvp.core.client.offlinemessages.OfflineMessagesHandler;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.StatContainer;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.command.CommandManager;
import me.mykindos.betterpvp.core.command.ICommand;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.inviting.InviteHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.world.blocks.WorldBlockHandler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickCallback;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.metadata.FixedMetadataValue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Objects;
import java.util.Optional;

import static net.kyori.adventure.text.event.ClickCallback.UNLIMITED_USES;

@CustomLog
@BPvPListener
@Singleton
public class ClanEventListener extends ClanListener {

    private final InviteHandler inviteHandler;
    private final WorldBlockHandler blockHandler;
    private final Clans clans;
    private final CommandManager commandManager;
    private final CooldownManager cooldownManager;
    private final OfflineMessagesHandler offlineMessagesHandler;

    @Inject
    @Config(path = "clans.members.max", defaultValue = "8")
    private int maxClanMembers;

    @Inject
    @Config(path = "clans.core.maxY", defaultValue = "125")
    private int maxCoreY;

    @Inject
    public ClanEventListener(final Clans clans, final ClanManager clanManager, final ClientManager clientManager, final InviteHandler inviteHandler,
                             final WorldBlockHandler blockHandler, final CommandManager commandManager, final CooldownManager cooldownManager, OfflineMessagesHandler offlineMessagesHandler) {
        super(clanManager, clientManager);
        this.clans = clans;
        this.inviteHandler = inviteHandler;
        this.blockHandler = blockHandler;
        this.commandManager = commandManager;
        this.cooldownManager = cooldownManager;
        this.offlineMessagesHandler = offlineMessagesHandler;
    }

    private String getClanFullName(final ClanRelation clanRelation, final IClan clan) {
        String type = clan.isAdmin() ? "Admin Clan" : "Clan";

        return clanRelation.getPrimaryMiniColor() + type + " " + clan.getName() + "<gray>";
    }

    private String getClanShortName(final ClanRelation clanRelation, final IClan clan) {
        return clanRelation.getPrimaryMiniColor() + clan.getName() + "<gray>";
    }

    private String getPlayerName(final ClanRelation clanRelation, final String playerName) {
        return clanRelation.getPrimaryMiniColor() + playerName + "<gray>";
    }

    private String getPlayerName(final ClanRelation clanRelation, final Player player) {
        return this.getPlayerName(clanRelation, player.getName());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkClaim(final ChunkClaimEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Player player = event.getPlayer();
        final Clan clan = event.getClan();
        final Chunk chunk = event.getChunk();

        final String chunkString = UtilWorld.chunkToFile(chunk);
        clan.getTerritory().add(new ClanTerritory(chunkString));
        this.clanManager.getRepository().saveClanTerritory(clan, chunkString);

        // If the clan has no territory, set the core position
        if (clan.getTerritory().size() == 1) {
            UtilServer.callEvent(new ClanSetCoreLocationEvent(player, clan, true));
        }

        final String stringChunk = UtilWorld.chunkToPrettyString(chunk);
        UtilMessage.simpleMessage(player, "Clans", "You claimed Territory <yellow>" + stringChunk + "</yellow>.");

        clan.messageClan(String.format("%s claimed territory <yellow>%s<gray>.", this.getPlayerName(ClanRelation.SELF, player),
                stringChunk), player.getUniqueId(), true);

        this.blockHandler.outlineChunk(chunk);
        log.info("{} ({}) claimed {} for {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        stringChunk, clan.getName(), clan.getId())
                .setAction("CLAN_CLAIM").addClientContext(event.getPlayer()).addClanContext(clan)
                .addContext(LogContext.CHUNK, stringChunk).submit();
        clientManager.incrementStat(player, ClientStat.CLANS_CLAIM_TERRITORY, 1);
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onChunkUnclaimCore(final ChunkUnclaimEvent event) {
        final Player player = event.getPlayer();
        final Clan targetClan = event.getClan();
        final Chunk chunk = event.getChunk();
        final boolean selfClan = this.clanManager.getClanByPlayer(player).orElse(null) == targetClan;

        if (targetClan.getCore().getPosition() != null && targetClan.getCore().getPosition().getChunk().equals(chunk)) {
            if (selfClan && targetClan.getTerritory().size() == 1) {
                targetClan.getCore().removeBlock(); // Remove the core block if it exists
                targetClan.getCore().setPosition(null);
                this.clanManager.getRepository().updateClanCore(targetClan);
                return; // Allow the core to be unclaimed if it is the only territory and the player is in the clan
            }

            UtilMessage.simpleMessage(player, "Clans", "<red>You cannot unclaim <alt2>%s</alt2> as it contains a clan core.", UtilWorld.chunkToPrettyString(chunk));

            if (selfClan) {
                UtilMessage.simpleMessage(player, "Clans", "To unclaim the core, you must unclaim all other territories first.");
            }
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onPistonCoreExtend(final BlockPistonExtendEvent event) {
        for (final Block block : event.getBlocks()) {
            if (ClanCore.isCore(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onPistonCoreRetract(final BlockPistonRetractEvent event) {
        for (final Block block : event.getBlocks()) {
            if (ClanCore.isCore(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onExplodeCore(final BlockExplodeEvent event) {
        for (final Block block : event.blockList()) {
            if (ClanCore.isCore(block)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onCoreInteract(final PlayerInteractEvent event) {
        if (ClanCore.isCore(event.getClickedBlock())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onChunkUnclaim(final ChunkUnclaimEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Player player = event.getPlayer();
        final Clan targetClan = event.getClan();
        final Chunk chunk = event.getChunk();
        final String chunkString = UtilWorld.chunkToFile(chunk);
        final String chunkToPrettyString = UtilWorld.chunkToPrettyString(chunk);

        final Clan playerClan = this.clanManager.getClanByPlayer(player).orElse(null);

        UtilMessage.simpleMessage(player, "Clans", "You unclaimed territory <alt2>" + chunkToPrettyString + "</alt2>.");

        targetClan.messageClan(String.format("%s unclaimed territory <yellow>%s<gray>.", this.getPlayerName(this.clanManager.getRelation(targetClan, playerClan), player),
                chunkToPrettyString), player.getUniqueId(), true);
        this.clanManager.getRepository().deleteClanTerritory(targetClan, chunkString);
        targetClan.getTerritory().removeIf(territory -> territory.getChunk().equals(UtilWorld.chunkToFile(chunk)));

        log.info("{} ({}) unclaimed {} from {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        chunkToPrettyString, targetClan.getName(), targetClan.getId())
                .setAction("CLAN_UNCLAIM").addClientContext(event.getPlayer()).addClanContext(targetClan).
                addContext(LogContext.CHUNK, chunkToPrettyString).submit();

        final StatContainer container = clientManager.search().online(player).getStatContainer();
        if (targetClan.getMemberByUUID(player.getUniqueId()).isEmpty()) {
            container.incrementStat(ClientStat.CLANS_UNCLAIM_OTHER_TERRITORY, 1);
        } else {
            container.incrementStat(ClientStat.CLANS_UNCLAIM_TERRITORY, 1);
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanCreate(final ClanCreateEvent event) {

        final Clan clan = event.getClan();
        final ICommand clanCommand = this.commandManager.getCommand("clan").orElseThrow();

        Player player = event.getPlayer();
        for (final ICommand subCommand : clanCommand.getSubCommands()) {

            if (subCommand.getName().equalsIgnoreCase(clan.getName()) || subCommand.getAliases().stream().anyMatch(o -> o.equalsIgnoreCase(clan.getName()))) {
                UtilMessage.message(player, "Command", "Clan name cannot be a clan's subcommand name or alias");
                return;
            }
        }

        if (!this.cooldownManager.use(player, "Create Clan", 300, true)) {
            return;
        }

        clan.getMembers().add(new ClanMember(player.getUniqueId(), ClanMember.MemberRank.LEADER, player.getName()));
        player.setMetadata("clan", new FixedMetadataValue(this.clans, clan.getId()));

        this.clanManager.addObject(clan.getId(), clan);
        this.clanManager.getRepository().save(clan);
        this.clanManager.getLeaderboard().forceUpdate();

        final var defaultValues = this.clans.getInjector().getInstance(ClanDefaultValues.class);
        clan.saveProperty(ClanProperty.TIME_CREATED, System.currentTimeMillis());
        clan.saveProperty(ClanProperty.LAST_LOGIN, System.currentTimeMillis());
        clan.saveProperty(ClanProperty.POINTS, defaultValues.getDefaultPoints());
        clan.saveProperty(ClanProperty.ENERGY, defaultValues.getDefaultEnergy());
        clan.saveProperty(ClanProperty.EXPERIENCE, 0d);
        clan.saveProperty(ClanProperty.BALANCE, 0);
        clan.saveProperty(ClanProperty.NO_DOMINANCE_COOLDOWN, (System.currentTimeMillis() + (3_600_000L * 24)));

        UtilMessage.simpleMessage(player, "Clans", "Successfully created clan %s.", this.getClanShortName(ClanRelation.SELF, clan));
        if (clan.isAdmin()) {
            this.clientManager.sendMessageToRank("Clans", UtilMessage.deserialize("%s created admin clan %s.", this.getPlayerName(ClanRelation.NEUTRAL, player), this.getClanShortName(ClanRelation.NEUTRAL, clan)), Rank.TRIAL_MOD);

            log.info("{} ({}) created admin clan {} ({})", player.getName(), player.getUniqueId(), clan.getName(), clan.getId())
                    .setAction("CLAN_CREATE").addClientContext(player).addClanContext(clan).submit();
        } else {
            log.info("{} ({}) created {} ({})", player.getName(), player.getUniqueId(), clan.getName(), clan.getId())
                    .setAction("CLAN_CREATE").addClientContext(player).addClanContext(clan).submit();

        }
        clientManager.incrementStat(player, ClientStat.CLANS_CLAN_CREATE, 1);

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanDisband(final ClanDisbandEvent event) {

        if (event.isCancelled()) {
            return;
        }

        final Clan clan = event.getClan();

        for (final ClanAlliance alliance : clan.getAlliances()) {
            alliance.getClan().getAlliances().removeIf(ally -> ally.getClan().getName().equalsIgnoreCase(clan.getName()));
        }

        for (final Player targetPlayer : Bukkit.getServer().getOnlinePlayers()) {
            ClanRelation clanRelation = this.clanManager.getRelation(this.clanManager.getClanByPlayer(targetPlayer).orElse(null), clan);

            if (clan.getTerritory().isEmpty()) {
                UtilMessage.simpleMessage(targetPlayer, "Clans", "%s has been disbanded.", this.getClanFullName(clanRelation, clan));
            } else {
                final Chunk chunk = UtilWorld.stringToChunk(clan.getTerritory().getFirst().getChunk());
                if (chunk != null) {
                    UtilMessage.message(targetPlayer, "Clans", "%s has been disbanded. (<yellow>%s</yellow>)",
                            this.getClanFullName(clanRelation, clan), (chunk.getX() * 16) + "<gray>,</gray> " + (chunk.getZ() * 16));
                }
            }
        }

        Component enemyDominanceComponent = Component.empty();
        clan.getEnemies().sort(Comparator.comparingDouble(ClanEnemy::getDominance));
        for (final ClanEnemy enemy : clan.getEnemies()) {
            double dominance = enemy.getDominance() - enemy.getClan().getEnemy(clan).orElseThrow().getDominance();
            enemyDominanceComponent = enemyDominanceComponent.append(Component.text(enemy.getClan().getName(), ClanRelation.NEUTRAL.getPrimary()))
                    .appendSpace()
                    .append(Component.text(dominance, dominance > 0 ? NamedTextColor.GREEN : NamedTextColor.RED)).appendSpace();
            enemy.getClan().getEnemies().removeIf(en -> en.getClan().getName().equalsIgnoreCase(clan.getName()));
        }

        Component finalEnemyDominanceComponent = enemyDominanceComponent;
        ClickEvent clickEvent = ClickEvent.callback(audience -> {
                    UtilMessage.message(audience, "Clans", finalEnemyDominanceComponent);
                }, ClickCallback.Options.builder()
                        .uses(UNLIMITED_USES)
                        .build()
        );

        for (final Player targetPlayer : Bukkit.getServer().getOnlinePlayers()) {
            final ClanRelation clanRelation = this.clanManager.getRelation(this.clanManager.getClanByPlayer(targetPlayer).orElse(null), clan);

            UtilMessage.simpleMessage(targetPlayer, "Clans",
                    Component.text("Click Here", NamedTextColor.WHITE, TextDecoration.UNDERLINED).clickEvent(clickEvent).hoverEvent(HoverEvent.showText(finalEnemyDominanceComponent))
                            .append(Component.text(" to see ", NamedTextColor.GRAY)).decoration(TextDecoration.UNDERLINED, false)
                            .append(Component.text(clan.getName(), clanRelation.getPrimary()))
                            .append(Component.text("'s enemies", NamedTextColor.GRAY))
            );
        }

        try {
            ClanCore core = clan.getCore();
            if (core != null && clan.getCore().getPosition() != null) {
                Location dropLocation = clan.getCore().getPosition().clone().add(0, 1, 0);

                ClanMailbox mailbox = core.getMailbox();
                mailbox.getContents().forEach(item -> {
                    dropLocation.getWorld().dropItem(dropLocation, item);
                });
                mailbox.getContents().clear();

                ClanVault vault = core.getVault();
                vault.getContents().values().forEach(item -> {
                    dropLocation.getWorld().dropItem(dropLocation, item);
                });
                vault.getContents().clear();

                clan.getCore().removeBlock(); // Remove the core block if it exists
                clan.getCore().setPosition(null);
            }
        } catch (Exception ex) {
            log.error("Failed to clean up clan core on disband", ex).submit();
        }

        clan.getTerritory().forEach(clanManager::applyDisbandClaimCooldown);
        if (event.getPlayer() != null) {
            clan.getMembers().forEach(clanMember -> {
                if (!clanMember.isOnline()) {
                    offlineMessagesHandler.sendOfflineMessage(clanMember.getUuid(),
                            OfflineMessage.Action.CLAN_DISBAND,
                            "Your clan %s was disbanded by %s.",
                            this.getClanShortName(ClanRelation.SELF, clan), this.getPlayerName(this.clanManager.getRelation(clan, this.clanManager.getClanByPlayer(event.getPlayer()).orElse(null)), event.getPlayer()));
                } else {
                    Objects.requireNonNull(clanMember.getPlayer()).closeInventory();
                }
            });
        } else {
            clan.getMembers().forEach(clanMember -> {
                if (!clanMember.isOnline()) {
                    offlineMessagesHandler.sendOfflineMessage(clanMember.getUuid(),
                            OfflineMessage.Action.CLAN_DISBAND,
                            "Your clan %s was disbanded due to running out of energy.",
                            this.getClanShortName(ClanRelation.SELF, clan));
                }
            });
        }


        clan.getMembers().clear();
        clan.getTerritory().clear();
        clan.getEnemies().clear();
        clan.getAlliances().clear();

        this.clanManager.getRepository().delete(clan);
        this.clanManager.getObjects().remove(clan.getId());
        this.clanManager.getLeaderboard().forceUpdate();

        if (event.getPlayer() != null) {
            log.info("{} ({}) disbanded {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(), clan.getName(), clan.getId())
                    .setAction("CLAN_DISBAND").addClientContext(event.getPlayer()).addClanContext(clan).submit();
        } else {
            log.info("System disbanded {} ({}) for running out of energy", clan.getName(), clan.getId())
                    .setAction("CLAN_DISBAND").addClanContext(clan).submit();
        }

        final var memberCache = new ArrayList<>(event.getClan().getMembers());
        memberCache.forEach(member -> {
            final Player player = member.getPlayer();
            if (player != null) {
                player.removeMetadata("clan", this.clans);
            }
        });

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanInviteMember(final ClanInviteMemberEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Clan clan = event.getClan();
        final Player player = event.getPlayer();
        final Player target = event.getTarget();


        UtilMessage.simpleMessage(player, "Clans", "You invited %s to join your Clan.", this.getPlayerName(ClanRelation.NEUTRAL, target));

        clan.messageClan(String.format("%s invited %s to join your Clan.", this.getPlayerName(ClanRelation.SELF, player), this.getPlayerName(ClanRelation.NEUTRAL, target)), player.getUniqueId(), true);

        UtilMessage.simpleMessage(target, "Clans", "%s invited you to join %s.", this.getPlayerName(ClanRelation.NEUTRAL, player), this.getClanFullName(ClanRelation.NEUTRAL, clan));

        final Component inviteMessage = Component.text("Click Here", NamedTextColor.GOLD, TextDecoration.UNDERLINED)
                .clickEvent(ClickEvent.runCommand("/c join " + clan.getName()))
                .append(UtilMessage.deserialize(" or type '<alt2>/c join " + clan.getName() + "</alt2>' to accept!"));
        UtilMessage.simpleMessage(target, "Clans", inviteMessage);

        final Gamer targetGamer = this.clientManager.search().online(target).getGamer();
        this.inviteHandler.createInvite(clan, targetGamer, "Invite", 20);
        log.info("{} ({}) invited {} ({}) to {} ({})", player.getName(), player.getUniqueId(),
                        target.getName(), target.getUniqueId(), clan.getName(), clan.getId()).setAction("CLAN_INVITE")
                .addClientContext(target, true).addClientContext(event.getPlayer()).addClanContext(clan).submit();

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoinClanEvent(final MemberJoinClanEvent event) {

        final Clan clan = event.getClan();
        final Player player = event.getPlayer();
        final Client client = this.clientManager.search().online(player);
        final Gamer gamer = client.getGamer();

        if (!client.isAdministrating()) {
            if (!this.inviteHandler.isInvited(gamer, clan, "Invite")) {
                UtilMessage.simpleMessage(player, "Clans", "You are not invited to %s.", this.getClanFullName(ClanRelation.NEUTRAL, clan));
                return;
            }

            if (clan.getSquadCount() >= this.maxClanMembers) {
                UtilMessage.simpleMessage(player, "Clans", "%s has too many members or allies", this.getClanFullName(ClanRelation.NEUTRAL, clan));
                return;
            }

            boolean allySquadCountTooHigh = false;
            for (final ClanAlliance clanAlliance : clan.getAlliances()) {
                final IClan allianceClan = clanAlliance.getClan();

                if (allianceClan.getSquadCount() + 1 > maxClanMembers) {
                    UtilMessage.message(player, "Clans",
                            "You cannot join %s, as it would cause %s to have too many allies.",
                            this.getClanFullName(ClanRelation.NEUTRAL, clan), this.getClanFullName(ClanRelation.NEUTRAL, clanAlliance.getClan()));

                    clan.messageClan("%s tried to join your clan, but could not, as it would cause %s to have too many allies. You must either reduce your squad size, or have your ally reduce their squad to allow %s to join."
                                    .formatted(this.getPlayerName(ClanRelation.NEUTRAL, player), this.getClanFullName(this.clanManager.getRelation(clan, allianceClan), clanAlliance.getClan()), this.getPlayerName(ClanRelation.NEUTRAL, player)),
                            null, true);

                    allySquadCountTooHigh = true;
                }
            }

            if (clanManager.getPillageHandler().isBeingPillaged(clan)) {
                UtilMessage.message(player, "Clans", "You cannot join a clan that is being pillaged.");
                return;
            }

            if (allySquadCountTooHigh) {
                return;
            }
        } else {
            this.clientManager.sendMessageToRank("Clans", UtilMessage.deserialize("%s force joined %s.", this.getPlayerName(ClanRelation.NEUTRAL, player), this.getClanShortName(ClanRelation.NEUTRAL, clan)), Rank.TRIAL_MOD);
        }

        final ClanMember member = new ClanMember(player.getUniqueId(), client.isAdministrating() ? ClanMember.MemberRank.LEADER : ClanMember.MemberRank.RECRUIT, player.getName());
        clan.getMembers().add(member);
        player.setMetadata("clan", new FixedMetadataValue(this.clans, clan.getId()));

        this.clanManager.getRepository().saveClanMember(clan, member);


        this.inviteHandler.removeInvite(clan, gamer, "Invite");
        this.inviteHandler.removeInvite(gamer, clan, "Invite");

        clan.setOnline(true);
        // Leave below as NEUTRAL for a before-effect, or SELF as an after-effect for clan join
        clan.messageClan(String.format("%s has joined your Clan.", this.getPlayerName(ClanRelation.NEUTRAL, player)), player.getUniqueId(), true);
        UtilMessage.simpleMessage(player, "Clans", "You joined %s.", this.getClanFullName(ClanRelation.NEUTRAL, clan));

        log.info("{} ({}) joined {} ({})", player.getName(), player.getUniqueId(), clan.getName(), clan.getId()).setAction("CLAN_JOIN")
                .addClientContext(player)
                .addClanContext(clan)
                .submit();

        client.getStatContainer().incrementStat(ClientStat.CLANS_CLAN_JOIN, 1);

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onLeaveClanEvent(final MemberLeaveClanEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Player player = event.getPlayer();
        final Clan clan = event.getClan();

        final Optional<ClanMember> memberOptional = clan.getMemberByUUID(player.getUniqueId());
        if (memberOptional.isPresent()) {
            final ClanMember clanMember = memberOptional.get();

            this.clanManager.getRepository().deleteClanMember(clan, clanMember);
            clan.getMembers().remove(clanMember);

            // Leave below as NEUTRAL for an after-effect, or SELF as a before-effect for clan leave
            UtilMessage.simpleMessage(player, "Clans", "You left %s.", this.getClanFullName(ClanRelation.NEUTRAL, clan));
            clan.messageClan("%s left your Clan.".formatted(this.getPlayerName(ClanRelation.NEUTRAL, player)), null, true);

            player.removeMetadata("clan", this.clans);

            clan.setOnline(clan.getMembers().stream().anyMatch(ClanMember::isOnline));
        }

        log.info("{} ({}) left {} ({})", player.getName(), player.getUniqueId(), clan.getName(), clan.getId())
                .setAction("CLAN_LEAVE").addClientContext(player).addClanContext(clan).submit();

        clientManager.incrementStat(player, ClientStat.CLANS_CLAN_LEAVE, 1);

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMemberKicked(final ClanKickMemberEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Player player = event.getPlayer();
        final Clan clan = event.getClan();
        final Client target = event.getTarget();

        final Optional<ClanMember> memberOptional = clan.getMemberByUUID(target.getUuid());
        if (memberOptional.isPresent()) {
            final ClanMember clanMember = memberOptional.get();

            this.clanManager.getRepository().deleteClanMember(clan, clanMember);
            clan.getMembers().remove(clanMember);

            // Leave below as NEUTRAL for an after-effect, or SELF as a before-effect for clan kick
            UtilMessage.simpleMessage(player, "Clans", "You kicked %s from your Clan.", this.getPlayerName(ClanRelation.NEUTRAL, target.getName()));
            clan.messageClan(String.format("%s was kicked from your Clan.", this.getPlayerName(ClanRelation.NEUTRAL, target.getName())), null, true);

            final Player targetPlayer = target.getPlayer();
            if (targetPlayer != null) {
                UtilMessage.simpleMessage(targetPlayer, "Clans", "You were kicked from %s.", this.getClanFullName(ClanRelation.NEUTRAL, clan));
                targetPlayer.closeInventory();
                targetPlayer.removeMetadata("clan", this.clans);

            } else {
                offlineMessagesHandler.sendOfflineMessage(target.getUniqueId(), OfflineMessage.Action.CLAN_KICK, "You were kicked from %s", this.getClanFullName(ClanRelation.NEUTRAL, clan));
            }
        }

        log.info("{} ({}) was kicked by {} ({}) from {} ({})", target.getName(), target.getUuid(),
                        player.getName(), player.getUniqueId(), clan.getName(), clan.getId()).
                setAction("CLAN_KICK").addClientContext(player).addClientContext(target, true).addClanContext(clan).submit();
    }

    @EventHandler
    public void onClanRequestAlliance(final ClanRequestAllianceEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Clan clan = event.getClan();
        final Clan target = event.getTargetClan();

        if (this.inviteHandler.isInvited(target, clan, "Alliance")) {
            UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You already have a pending alliance request with %s.", this.getClanFullName(ClanRelation.NEUTRAL, target));
            return;
        }

        if (this.inviteHandler.isInvited(clan, target, "Alliance")) {

            UtilServer.callEvent(new ClanAllianceEvent(event.getPlayer(), clan, target));
            return;
        }

        this.inviteHandler.createInvite(clan, target, "Alliance", 10);
        UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You have requested an alliance with %s.", this.getClanFullName(ClanRelation.NEUTRAL, target));
        target.messageClan("%s has requested an alliance.".formatted(this.getClanFullName(ClanRelation.NEUTRAL, clan)), null, true);

        log.info("{} ({}) of {} ({}) requested alliance with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_ALLIANCE_REQUEST")
                .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanAlly(final ClanAllianceEvent event) {
        if (event.isCancelled()) {
            return;
        }
        final Clan clan = event.getClan();
        final Clan target = event.getTargetClan();

        this.inviteHandler.removeInvite(clan, target, "Alliance");
        this.inviteHandler.removeInvite(target, clan, "Alliance");

        final ClanAlliance clanAlliance = new ClanAlliance(target, false);
        final ClanAlliance targetAlliance = new ClanAlliance(clan, false);
        clan.getAlliances().add(clanAlliance);
        target.getAlliances().add(targetAlliance);

        clan.messageClan("%s is now your ally.".formatted(this.getClanFullName(ClanRelation.ALLY, target)), null, true);
        target.messageClan("%s is now your ally.".formatted(this.getClanFullName(ClanRelation.ALLY, clan)), null, true);

        this.clanManager.getRepository().saveClanAlliance(clan, clanAlliance);
        this.clanManager.getRepository().saveClanAlliance(target, targetAlliance);

        log.info("{} ({}) of {} ({}) accepted alliance with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_ALLIANCE_ACCEPT")
                .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

    }

    @EventHandler
    public void onClanRequestTrust(final ClanRequestTrustEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Clan clan = event.getClan();
        final Clan target = event.getTargetClan();

        if (this.inviteHandler.isInvited(target, clan, "Trust")) {
            UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You already have a pending trust request with %s.", this.getClanFullName(ClanRelation.ALLY, target));
            return;
        }

        if (this.inviteHandler.isInvited(clan, target, "Trust")) {

            UtilServer.callEvent(new ClanTrustEvent(event.getPlayer(), clan, target));
            return;
        }

        this.inviteHandler.createInvite(clan, target, "Trust", 10);
        UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You have requested to form a trust with %s.", this.getClanFullName(ClanRelation.ALLY, target));
        target.messageClan("%s has requested to form a trust with your clan.".formatted(this.getClanFullName(ClanRelation.ALLY, clan)), null, true);

        log.info("{} ({}) of {} ({}) requested trust with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_TRUST_REQUEST")
                .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanTrust(final ClanTrustEvent event) {
        if (event.isCancelled()) {
            return;
        }
        final Clan clan = event.getClan();
        final Clan target = event.getTargetClan();

        this.inviteHandler.removeInvite(clan, target, "Trust");
        this.inviteHandler.removeInvite(target, clan, "Trust");

        final ClanAlliance clanAlliance = event.getClan().getAlliance(target).orElseThrow();
        final ClanAlliance targetAlliance = event.getTargetClan().getAlliance(clan).orElseThrow();
        clanAlliance.setTrusted(true);
        targetAlliance.setTrusted(true);

        clan.messageClan("%s is now a trusted ally.".formatted(this.getClanFullName(ClanRelation.ALLY_TRUST, target)), null, true);
        target.messageClan("%s is now a trusted ally.".formatted(this.getClanFullName(ClanRelation.ALLY_TRUST, clan)), null, true);

        this.clanManager.getRepository().saveTrust(clan, clanAlliance);
        this.clanManager.getRepository().saveTrust(target, targetAlliance);

        log.info("{} ({}) of {} ({}) accepted trust with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_TRUST_ACCEPT")
                .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanUntrust(final ClanUntrustEvent event) {
        if (event.isCancelled()) {
            return;
        }
        final Clan clan = event.getClan();
        final Clan target = event.getTargetClan();

        final ClanAlliance clanAlliance = event.getClan().getAlliance(target).orElseThrow();
        final ClanAlliance targetAlliance = event.getTargetClan().getAlliance(clan).orElseThrow();
        clanAlliance.setTrusted(false);
        targetAlliance.setTrusted(false);

        clan.messageClan("%s is no longer a trusted ally.".formatted(this.getClanFullName(ClanRelation.ALLY, target)), null, true);
        target.messageClan("%s is no longer a trusted ally.".formatted(this.getClanFullName(ClanRelation.ALLY, clan)), null, true);

        this.clanManager.getRepository().saveTrust(clan, clanAlliance);
        this.clanManager.getRepository().saveTrust(target, targetAlliance);

        log.info("{} ({}) of {} ({}) removed trust with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_TRUST_REMOVE")
                .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

    }

    @EventHandler
    public void onClanRequestNeutral(final ClanRequestNeutralEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Clan clan = event.getClan();
        final Clan target = event.getTargetClan();

        if (clan.isEnemy(target)) {

            if (this.inviteHandler.isInvited(target, clan, "Neutral")) {
                UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You already have a pending neutral request with %s.", this.getClanFullName(ClanRelation.ENEMY, target));
                return;
            }

            if (this.inviteHandler.isInvited(clan, target, "Neutral")) {

                UtilServer.callEvent(new ClanNeutralEvent(event.getPlayer(), clan, target));
                return;
            }

            this.inviteHandler.createInvite(clan, target, "Neutral", 10);
            UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You have requested to neutral with %s.", this.getClanFullName(ClanRelation.ENEMY, target));
            target.messageClan("%s has requested to neutral with your clan.".formatted(this.getClanFullName(ClanRelation.ENEMY, clan)), null, true);

            log.info("{} ({}) of {} ({}) requested neutral with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                            clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_NEUTRAL_REQUEST")
                    .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

        } else {
            UtilServer.callEvent(new ClanNeutralEvent(event.getPlayer(), clan, target));
        }

    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanNeutral(final ClanNeutralEvent event) {
        if (event.isCancelled()) {
            return;
        }
        final Clan clan = event.getClan();
        final Clan target = event.getTargetClan();

        this.inviteHandler.removeInvite(clan, target, "Neutral");
        this.inviteHandler.removeInvite(target, clan, "Neutral");

        if (clan.isAllied(target)) {
            this.removeAlliance(clan, target);
            log.info("{} ({}) of {} ({}) removed alliance with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                            clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_ALLIANCE_REMOVE")
                    .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

        } else if (clan.isEnemy(target)) {
            this.removeEnemy(clan, target);
            log.info("{} ({}) of {} ({}) accepted neutral with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                            clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_NEUTRAL_ACCEPT")
                    .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

        }

        clan.messageClan("%s is now neutral to your Clan.".formatted(this.getClanFullName(ClanRelation.NEUTRAL, target)), null, true);
        target.messageClan("%s is now neutral to your Clan.".formatted(this.getClanFullName(ClanRelation.NEUTRAL, clan)), null, true);

    }

    private void removeAlliance(final Clan clan, final Clan target) {
        final ClanAlliance clanAlliance = clan.getAlliance(target).orElseThrow();
        final ClanAlliance targetAlliance = target.getAlliance(clan).orElseThrow();
        this.clanManager.getRepository().deleteClanAlliance(clan, clanAlliance);
        this.clanManager.getRepository().deleteClanAlliance(target, targetAlliance);
        clan.getAlliances().remove(clanAlliance);
        target.getAlliances().remove(targetAlliance);
    }

    private void removeEnemy(final Clan clan, final Clan target) {
        final ClanEnemy clanEnemy = clan.getEnemy(target).orElseThrow();
        final ClanEnemy targetEnemy = target.getEnemy(clan).orElseThrow();
        this.clanManager.getRepository().deleteClanEnemy(clan, clanEnemy);
        this.clanManager.getRepository().deleteClanEnemy(target, targetEnemy);
        clan.getEnemies().remove(clanEnemy);
        target.getEnemies().remove(targetEnemy);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanEnemy(final ClanEnemyEvent event) {
        if (event.isCancelled()) {
            return;
        }
        final Clan clan = event.getClan();
        final Clan target = event.getTargetClan();

        if (clan.isEnemy(target)) {
            return;
        }

        if (clan.isAllied(target)) {
            this.removeAlliance(clan, target);
            log.info("{} ({}) of {} ({}) removed alliance with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                            clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_ALLIANCE_REMOVE")
                    .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();

        }

        final ClanEnemy clanEnemy = new ClanEnemy(target, 0);
        final ClanEnemy targetEnemy = new ClanEnemy(clan, 0);

        clan.getEnemies().add(clanEnemy);
        target.getEnemies().add(targetEnemy);

        this.clanManager.getRepository().saveClanEnemy(clan, clanEnemy);
        this.clanManager.getRepository().saveClanEnemy(target, targetEnemy);

        clan.messageClan("%s is now your enemy.".formatted(this.getClanFullName(ClanRelation.ENEMY, target)), null, true);
        target.messageClan("%s is now your enemy.".formatted(this.getClanFullName(ClanRelation.ENEMY, clan)), null, true);

        log.info("{} ({}) of {} ({}) enemied with {} ({})", event.getPlayer().getName(), event.getPlayer().getUniqueId(),
                        clan.getName(), clan.getId(), target.getName(), target.getId()).setAction("CLAN_ENEMY")
                .addClientContext(event.getPlayer()).addClanContext(clan).addClanContext(target, true).submit();


    }

    @EventHandler
    public void onBlockCore(final BlockPlaceEvent event) {
        if (!event.getBlockReplacedState().getType().isSolid()) {
            return; // Skip non-solids
        }

        if (ClanCore.isCore(event.getBlock().getRelative(BlockFace.DOWN)) || ClanCore.isCore(event.getBlock().getRelative(BlockFace.DOWN, 2))) {
            event.setCancelled(true);
            UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You cannot place a block on top of a clan core.");
        }
    }

    @EventHandler
    public void onBreakCore(final BlockBreakEvent event) {
        if (ClanCore.isCore(event.getBlock())) {
            event.setCancelled(true);
            UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You cannot break a clan core.");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanSetCore(final ClanSetCoreLocationEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Clan clan = event.getClan();
        final Player player = event.getPlayer();

        if (!clan.isAdmin() && event.getPlayer().getLocation().getY() > maxCoreY) {
            UtilMessage.simpleMessage(event.getPlayer(), "Clans", "You cannot set the clan core above <yellow>%d Y</yellow>.", maxCoreY);
            return;
        }


        if (this.clanManager.getPillageHandler().isBeingPillaged(clan)) {
            UtilMessage.simpleMessage(player, "Clans", "You cannot set the clan core while being pillaged.");
            return;
        }

        if (!event.isIgnoreClaims()) {
            final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(player.getLocation());
            if (clanOptional.isEmpty() || !clanOptional.get().equals(clan)) {
                UtilMessage.simpleMessage(player, "Clans", "You can only set the clan core in your own territory.");
                return;
            }

        }

        final ClanCore core = clan.getCore();
        Location highest = player.getLocation();

        final Block block = core.getSafest(highest).getBlock();
        if(block.getType() == Material.BEDROCK) {
            UtilMessage.simpleMessage(player, "Clans", "You cannot place the clan core here.");
            return;
        }

        core.removeBlock(); // Remove old core
        Location coreLocation = block.getLocation().toCenterLocation().setDirection(player.getLocation().getDirection());
        core.setPosition(coreLocation); // Set new core location
        core.placeBlock(); // Place new core

        UtilMessage.simpleMessage(player, "Clans", "You set the clan core to <alt2>%s</alt2>.",
                UtilWorld.locationToString(player.getLocation()));
        log.info("{} ({}) of {} ({}) set their clan core to {}", player.getName(), player.getUniqueId(), clan.getName(), clan.getName(),
                        UtilWorld.locationToString(player.getLocation(), true)).setAction("CLAN_SETCORE")
                .addClientContext(player).addClanContext(clan).addLocationContext(player.getLocation()).submit();

        clientManager.search().online(player).getStatContainer().incrementStat(ClientStat.CLANS_SET_CORE, 1);

        this.clanManager.getRepository().updateClanCore(clan);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMemberPromote(final MemberPromoteEvent event) {
        if (event.isCancelled()) {
            return;
        }
        final Player player = event.getPlayer();
        final Clan clan = event.getClan();
        final ClanMember member = event.getClanMember();

        final ClanMember.MemberRank rank = member.getRank();

        member.setRank(ClanMember.MemberRank.getRankByPrivilege(Math.min(ClanMember.MemberRank.LEADER.getPrivilege(), member.getRank().getPrivilege() + 1)));
        this.clanManager.getRepository().updateClanMemberRank(clan, member);

        this.clientManager.search().offline(member.getUuid()).thenAcceptAsync(result -> {
            result.ifPresent(client -> {
                UtilMessage.simpleMessage(player, "Clans", "You promoted %s to <yellow>%s</yellow>.", this.getPlayerName(ClanRelation.SELF, clan.getName()), member.getRank().getName());

                log.info("{} ({}) was promoted by {} ({}) to {} in {} ({})", client.getName(), member.getUuid(), player.getName(),
                                player.getUniqueId(), member.getRank().getName(), clan.getName(), clan.getId()).setAction("CLAN_PROMOTE")
                        .addClientContext(player).addClientContext(client, true).addClanContext(clan)
                        .addContext(LogContext.CURRENT_CLAN_RANK, rank.getName()).addContext(LogContext.NEW_CLAN_RANK, member.getRank().getName()).submit();

            });
        });

        final Player memberPlayer = member.getPlayer();
        if (memberPlayer != null) {
            UtilMessage.simpleMessage(memberPlayer, "Clans", "You were promoted to <yellow>%s</yellow>.", member.getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onMemberDemote(final MemberDemoteEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Player player = event.getPlayer();
        final Clan clan = event.getClan();
        final ClanMember member = event.getClanMember();

        final ClanMember.MemberRank rank = member.getRank();

        member.setRank(ClanMember.MemberRank.getRankByPrivilege(Math.max(1, member.getRank().getPrivilege() - 1)));
        this.clanManager.getRepository().updateClanMemberRank(clan, member);

        this.clientManager.search().offline(member.getUuid()).thenAcceptAsync(result -> {
            result.ifPresent(client -> {
                UtilMessage.simpleMessage(player, "Clans", "You demoted %s to <yellow>%s</yellow>.", this.getPlayerName(ClanRelation.SELF, client.getName()), member.getRank().getName());
                log.info("{} ({}) was demoted by {} ({}) to {} in {} ({})", client.getName(), member.getUuid(),
                                player.getName(), player.getUniqueId(), member.getRank().getName(), clan.getName(), clan.getId())
                        .setAction("CLAN_DEMOTE").addClientContext(player).addClientContext(client, true).addClanContext(clan)
                        .addContext(LogContext.CURRENT_CLAN_RANK, rank.getName()).addContext(LogContext.NEW_CLAN_RANK, member.getRank().getName()).submit();

            });
        });

        final Player memberPlayer = member.getPlayer();
        if (memberPlayer != null) {
            UtilMessage.simpleMessage(memberPlayer, "Clans", "You were demoted to <yellow>%s</yellow>.", member.getName());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerJoinLoadClanClients(PlayerJoinEvent event) {
        clanManager.getClanByPlayer(event.getPlayer()).ifPresent(clan -> {
            for (ClanMember member : clan.getMembers()) {
                clientManager.search().offline(member.getUuid()).thenAcceptAsync(result -> {
                    result.ifPresent(client -> {
                        log.info("Loaded {} ({}) as they are a member of an online clan", client.getName(), client.getUniqueId().toString()).submit();
                    });
                });
            }
        });
    }
}
