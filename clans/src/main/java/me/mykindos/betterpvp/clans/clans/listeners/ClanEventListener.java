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
import me.mykindos.betterpvp.clans.clans.events.ClanSetCoreLocationEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanTrustEvent;
import me.mykindos.betterpvp.clans.clans.events.ClanUntrustEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberDemoteEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberJoinClanEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberLeaveClanEvent;
import me.mykindos.betterpvp.clans.clans.events.MemberPromoteEvent;
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
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.framework.inviting.InviteHandler;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.locale.Translations;
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
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockExplodeEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
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
        UtilMessage.message(player, "clans.prefix", "clans.command.clan.claim.success-self",
                Component.text(stringChunk, NamedTextColor.YELLOW)
        );

        clan.getMembers().forEach(member -> {
            if (member.getUuid().equals(player.getUniqueId())) return;
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.claim.success-clan",
                        Component.text(player.getName(), NamedTextColor.YELLOW),
                        Component.text(stringChunk, NamedTextColor.YELLOW)
                );
            }
        });

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

            UtilMessage.message(player, "clans.prefix", "clans.command.clan.unclaim.core-fail",
                    Component.text(UtilWorld.chunkToPrettyString(chunk), NamedTextColor.YELLOW)
            );

            if (selfClan) {
                UtilMessage.message(player, "clans.prefix", "clans.command.clan.unclaim.core-self-fail");
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

        UtilMessage.message(player, "clans.prefix", "clans.command.clan.unclaim.success-self", Component.text(chunkToPrettyString, NamedTextColor.YELLOW));

        targetClan.getMembers().forEach(member -> {
            if (member.getUuid().equals(player.getUniqueId())) return;
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.unclaim.success-clan",
                        this.clanManager.getPlayerName(ClanRelation.SELF, player),
                        Component.text(chunkToPrettyString, NamedTextColor.YELLOW)
                );
            }
        });
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
                UtilMessage.message(player, "clans.prefix", "clans.command.clan.create.invalid-name");
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

        UtilMessage.message(player, "clans.prefix", "clans.command.clan.create.success",
                this.clanManager.getClanShortName(ClanRelation.SELF, clan)
        );

        log.info("{} ({}) created {} ({})", player.getName(), player.getUniqueId(), clan.getName(), clan.getId())
                .setAction("CLAN_CREATE").addClientContext(player).addClanContext(clan).submit();
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

        if (clan.getTerritory().isEmpty()) {
            for (Player targetPlayer : Bukkit.getServer().getOnlinePlayers()) {
                ClanRelation clanRelation = this.clanManager.getRelation(this.clanManager.getClanByPlayer(targetPlayer).orElse(null), clan);

                UtilMessage.message(targetPlayer, "clans.prefix", "clans.command.clan.disband.broadcast",
                        this.clanManager.getClanShortName(clanRelation, clan)
                );
            }
        } else {
            final Chunk chunk = UtilWorld.stringToChunk(clan.getTerritory().getFirst().getChunk());
            if (chunk != null) {
                for (Player targetPlayer : Bukkit.getServer().getOnlinePlayers()) {
                    ClanRelation clanRelation = this.clanManager.getRelation(this.clanManager.getClanByPlayer(targetPlayer).orElse(null), clan);

                    UtilMessage.message(targetPlayer, "clans.prefix", "clans.command.clan.disband.broadcast-with-location",
                            this.clanManager.getClanShortName(clanRelation, clan),
                            Component.text(chunk.getX() * 16, NamedTextColor.YELLOW),
                            Component.text(chunk.getZ() * 16, NamedTextColor.YELLOW)
                    );
                }
            }
        }

        // TODO: Could replace `Component.text(enemy.getClan().getName(), ClanRelation.NEUTRAL.getPrimary())` with per message receiver's clan colors
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
                    UtilMessage.message(audience, "clans.prefix", finalEnemyDominanceComponent);
                }, ClickCallback.Options.builder()
                        .uses(UNLIMITED_USES)
                        .build()
        );

        for (Player targetPlayer : Bukkit.getServer().getOnlinePlayers()) {
            ClanRelation clanRelation = this.clanManager.getRelation(this.clanManager.getClanByPlayer(targetPlayer).orElse(null), clan);

            UtilMessage.message(targetPlayer, "clans.prefix", Translations.component("clans.command.clan.disband.enemies-link",
                    this.clanManager.getClanShortName(clanRelation, clan)).clickEvent(clickEvent).hoverEvent(HoverEvent.showText(finalEnemyDominanceComponent))
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

        Player player = event.getPlayer();
        if (player != null) {
            ClanRelation clanRelation = this.clanManager.getRelation(clan, this.clanManager.getClanByPlayer(player).orElse(null));

            clan.getMembers().forEach(clanMember -> {
                if (!clanMember.isOnline()) {
                    offlineMessagesHandler.sendOfflineMessage(clanMember.getUuid(),
                            OfflineMessage.Action.CLAN_DISBAND,
                            "Your clan %s was disbanded by %s.",
                            this.clanManager.getClanShortName(ClanRelation.SELF, clan), this.clanManager.getPlayerName(clanRelation, player)
                    );
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
                            this.clanManager.getClanShortName(ClanRelation.SELF, clan));
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
            final Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                memberPlayer.removeMetadata("clan", this.clans);
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
        final Player targetPlayer = event.getTarget();

        final ClanRelation playerClanRelation = this.clanManager.getRelation(clan, this.clanManager.getClanByPlayer(targetPlayer).orElse(null));

        UtilMessage.message(player, "clans.prefix", "clans.command.clan.invite.success-self",
                this.clanManager.getPlayerName(playerClanRelation, targetPlayer)
        );

        clan.getMembers().forEach(member -> {
            if (member.getUuid().equals(player.getUniqueId())) return;
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.invite.success-clan",
                        this.clanManager.getPlayerName(ClanRelation.SELF, player),
                        this.clanManager.getPlayerName(playerClanRelation, targetPlayer)
                );
            }
        });

        UtilMessage.message(targetPlayer, "clans.prefix", "clans.command.clan.invite.success-target", Component.text(player.getName(), NamedTextColor.YELLOW), Component.text(clan.getName(), NamedTextColor.YELLOW));

        final Component inviteMessage = Translations.component("clans.command.clan.invite.accept-link")
                .clickEvent(ClickEvent.runCommand("/c join " + clan.getName()))
                .append(Translations.component("clans.command.clan.invite.accept-text", Component.text("/c join " + clan.getName(), NamedTextColor.YELLOW)));
        UtilMessage.message(targetPlayer, "clans.prefix", inviteMessage);

        final Gamer targetGamer = this.clientManager.search().online(targetPlayer).getGamer();
        this.inviteHandler.createInvite(clan, targetGamer, "Invite", 20);
        log.info("{} ({}) invited {} ({}) to {} ({})", player.getName(), player.getUniqueId(),
                        targetPlayer.getName(), targetPlayer.getUniqueId(), clan.getName(), clan.getId()).setAction("CLAN_INVITE")
                .addClientContext(targetPlayer, true).addClientContext(event.getPlayer()).addClanContext(clan).submit();

    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoinClanEvent(final MemberJoinClanEvent event) {

        final Clan clan = event.getClan();
        final Player player = event.getPlayer();
        final Client client = this.clientManager.search().online(player);
        final Gamer gamer = client.getGamer();

        if (!client.isAdministrating()) {
            if (!this.inviteHandler.isInvited(gamer, clan, "Invite")) {
                UtilMessage.message(player, "clans.prefix", "clans.command.clan.join.not-invited", this.clanManager.getClanShortName(ClanRelation.NEUTRAL, clan));
                return;
            }

            if (clan.getSquadCount() >= this.maxClanMembers) {
                UtilMessage.message(player, "clans.prefix", "clans.command.clan.join.limit", this.clanManager.getClanShortName(ClanRelation.NEUTRAL, clan));
                return;
            }

            boolean allySquadCountTooHigh = false;
            for (ClanAlliance clanAlliance : clan.getAlliances()) {
                if (clanAlliance.getClan().getSquadCount() + 1 > maxClanMembers) {
                    UtilMessage.message(player, "clans.prefix", "clans.command.clan.join.ally-limit",
                            this.clanManager.getClanShortName(ClanRelation.NEUTRAL, clan),
                            this.clanManager.getClanShortName(ClanRelation.NEUTRAL, clanAlliance.getClan())
                    );

                    clan.getMembers().forEach(member -> {
                        Player memberPlayer = Bukkit.getPlayer(member.getUuid());
                        if (memberPlayer != null) {
                            UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.join.ally-limit-clan",
                                    this.clanManager.getPlayerName(ClanRelation.NEUTRAL, player),
                                    this.clanManager.getClanShortName(this.clanManager.getRelation(clan, clanAlliance.getClan()), clanAlliance.getClan())
                            );
                        }
                    });
                    allySquadCountTooHigh = true;
                }
            }

            if (clanManager.getPillageHandler().isBeingPillaged(clan)) {
                UtilMessage.message(player, "clans.prefix", "clans.command.clan.join.pillaged");
                return;
            }

            if (allySquadCountTooHigh) {
                return;
            }
        } else {
            clientManager.getPlayersOfRank(Rank.TRIAL_MOD).forEach(targetPlayer -> {
                Clan targetPlayerClan = this.clanManager.getClanByPlayer(targetPlayer).orElse(null);
                ClanRelation clanRelation = this.clanManager.getRelation(targetPlayerClan, clan);

                Component notification = Translations.component("clans.command.clan.join.force-mod-notification",
                        this.clanManager.getPlayerName(clanRelation, player),
                        this.clanManager.getClanShortName(clanRelation, clan)
                );

                UtilMessage.message(targetPlayer, "clans.prefix", notification);
            });
        }

        final ClanMember member = new ClanMember(player.getUniqueId(), client.isAdministrating() ? ClanMember.MemberRank.LEADER : ClanMember.MemberRank.RECRUIT, player.getName());
        clan.getMembers().add(member);
        player.setMetadata("clan", new FixedMetadataValue(this.clans, clan.getId()));

        this.clanManager.getRepository().saveClanMember(clan, member);


        this.inviteHandler.removeInvite(clan, gamer, "Invite");
        this.inviteHandler.removeInvite(gamer, clan, "Invite");

        clan.setOnline(true);
        clan.getMembers().forEach(clanMember -> {
            if (clanMember.getUuid().equals(player.getUniqueId())) return;
            Player memberPlayer = Bukkit.getPlayer(clanMember.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.join.success-clan",
                        this.clanManager.getPlayerName(ClanRelation.SELF, player)
                );
            }
        });

        UtilMessage.message(player, "clans.prefix", "clans.command.clan.join.success-self", Component.text(clan.getName(), NamedTextColor.YELLOW));

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

            UtilMessage.message(player, "clans.prefix", "clans.command.clan.leave.success-self",
                    this.clanManager.getClanShortName(ClanRelation.NEUTRAL, clan)
            );

            player.removeMetadata("clan", this.clans);

            boolean isOnline = false;
            for (final ClanMember member : clan.getMembers()) {
                final Player memberPlayer = Bukkit.getPlayer(member.getUuid());
                if (memberPlayer != null) {
                    UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.leave.success-clan",
                            this.clanManager.getPlayerName(ClanRelation.NEUTRAL, player)
                    );

                    isOnline = true;
                }
            }
            clan.setOnline(isOnline);
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

            UtilMessage.message(player, "clans.prefix", "clans.command.clan.kick.success-self",
                    this.clanManager.getPlayerName(ClanRelation.NEUTRAL, target.getName())
            );

            clan.getMembers().forEach(member -> {
                Player memberPlayer = Bukkit.getPlayer(member.getUuid());
                if (memberPlayer != null) {
                    UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.kick.success-clan",
                            this.clanManager.getPlayerName(ClanRelation.NEUTRAL, target.getName())
                    );
                }
            });

            final Player targetPlayer = Bukkit.getPlayerExact(target.getName());
            if (targetPlayer != null) {
                UtilMessage.message(targetPlayer, "clans.prefix", "clans.command.clan.kick.success-target",
                        this.clanManager.getClanShortName(ClanRelation.NEUTRAL, clan)
                );

                targetPlayer.closeInventory();
                targetPlayer.removeMetadata("clan", this.clans);

            } else {
                offlineMessagesHandler.sendOfflineMessage(target.getUniqueId(), OfflineMessage.Action.CLAN_KICK, "You were kicked from clan %s", this.clanManager.getClanShortName(ClanRelation.NEUTRAL, clan));
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
            UtilMessage.message(event.getPlayer(), "clans.prefix", "clans.command.clan.ally.already-requested",
                    this.clanManager.getClanShortName(ClanRelation.NEUTRAL, target)
            );
            return;
        }

        if (this.inviteHandler.isInvited(clan, target, "Alliance")) {

            UtilServer.callEvent(new ClanAllianceEvent(event.getPlayer(), clan, target));
            return;
        }

        this.inviteHandler.createInvite(clan, target, "Alliance", 10);

        UtilMessage.message(event.getPlayer(), "clans.prefix", "clans.command.clan.ally.success-self",
                this.clanManager.getClanShortName(ClanRelation.NEUTRAL, target)
        );

        // TODO: Forgot a `clan` members message?? ("Your Clan has requested an alliance with ...") created below, translation needed
//        clan.getMembers().forEach(member -> {
//            if (member.getUuid().equals(event.getPlayer().getUniqueId())) return;
//            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
//            if (memberPlayer != null) {
//                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.ally.success-self-others",
//                        this.clanManager.getClanShortName(ClanRelation.NEUTRAL, target)
//                );
//            }
//        });

        target.getMembers().forEach(member -> {
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.ally.success-target",
                        this.clanManager.getClanShortName(ClanRelation.NEUTRAL, clan)
                );
            }
        });

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

        clan.getMembers().forEach(member -> {
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.ally.success-clan",
                        this.clanManager.getClanShortName(ClanRelation.ALLY, target)
                );
            }
        });
        target.getMembers().forEach(member -> {
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.ally.success-clan",
                        this.clanManager.getClanShortName(ClanRelation.ALLY, clan)
                );
            }
        });

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
            UtilMessage.message(event.getPlayer(), "clans.prefix", "clans.command.clan.trust.already-requested",
                    this.clanManager.getClanShortName(ClanRelation.ALLY, target)
            );
            return;
        }

        if (this.inviteHandler.isInvited(clan, target, "Trust")) {

            UtilServer.callEvent(new ClanTrustEvent(event.getPlayer(), clan, target));
            return;
        }

        this.inviteHandler.createInvite(clan, target, "Trust", 10);

        UtilMessage.message(event.getPlayer(), "clans.prefix", "clans.command.clan.trust.success-self",
                this.clanManager.getClanShortName(ClanRelation.ALLY, target)
        );

        // TODO: Forgot a `clan` members message?? ("Your Clan has requested to trust with ...") created below, translation needed
//        clan.getMembers().forEach(member -> {
//            if (member.getUuid().equals(event.getPlayer().getUniqueId())) return;
//            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
//            if (memberPlayer != null) {
//                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.trust.success-self-others",
//                        this.clanManager.getClanShortName(ClanRelation.ALLY, target)
//                );
//            }
//        });

        target.getMembers().forEach(member -> {
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.trust.success-target",
                        this.clanManager.getClanShortName(ClanRelation.ALLY, clan)
                );
            }
        });

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

        clan.getMembers().forEach(member -> {
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.trust.success-clan",
                        this.clanManager.getClanShortName(ClanRelation.ALLY_TRUST, target)
                );
            }
        });
        target.getMembers().forEach(member -> {
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.trust.success-clan",
                        this.clanManager.getClanShortName(ClanRelation.ALLY_TRUST, clan)
                );
            }
        });

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

        clan.getMembers().forEach(member -> {
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.untrust.success-clan",
                        this.clanManager.getClanShortName(ClanRelation.ALLY, target)
                );
            }
        });
        target.getMembers().forEach(member -> {
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.untrust.success-clan",
                        this.clanManager.getClanShortName(ClanRelation.ALLY, clan)
                );
            }
        });

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

        final ClanRelation clanToTargetRelation = this.clanManager.getRelation(clan, target);
        final ClanRelation targetToClanRelation = this.clanManager.getRelation(clan, target);

        if (clan.isEnemy(target)) {

            if (this.inviteHandler.isInvited(target, clan, "Neutral")) {
                UtilMessage.message(event.getPlayer(), "clans.prefix", "clans.command.clan.neutral.already-requested",
                        this.clanManager.getClanShortName(clanToTargetRelation, target)
                );
                return;
            }

            if (this.inviteHandler.isInvited(clan, target, "Neutral")) {

                UtilServer.callEvent(new ClanNeutralEvent(event.getPlayer(), clan, target));
                return;
            }

            this.inviteHandler.createInvite(clan, target, "Neutral", 10);
            UtilMessage.message(event.getPlayer(), "clans.prefix", "clans.command.clan.neutral.success-self",
                    this.clanManager.getClanShortName(clanToTargetRelation, target)
            );

            // TODO: Forgot a `clan` members message?? ("Your Clan has requested to neutral with ...") created below, translation needed
//        clan.getMembers().forEach(member -> {
//            if (member.getUuid().equals(event.getPlayer().getUniqueId())) return;
//            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
//            if (memberPlayer != null) {
//                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.neutral.success-self-others",
//                        this.clanManager.getClanShortName(clanToTargetRelation, target)
//                );
//            }
//        });

            target.getMembers().forEach(member -> {
                Player memberPlayer = Bukkit.getPlayer(member.getUuid());
                if (memberPlayer != null) {
                    UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.neutral.success-target",
                            this.clanManager.getClanShortName(targetToClanRelation, clan)
                    );
                }
            });

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

        final ClanRelation clanToTargetRelation = this.clanManager.getRelation(clan, target);
        final ClanRelation targetToClanRelation = this.clanManager.getRelation(clan, target);

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

        clan.getMembers().forEach(member -> {
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.neutral.success-clan",
                        this.clanManager.getClanShortName(clanToTargetRelation, target)
                );
            }
        });
        target.getMembers().forEach(member -> {
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.neutral.success-clan",
                        this.clanManager.getClanShortName(targetToClanRelation, clan)
                );
            }
        });

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

        clan.getMembers().forEach(member -> {
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.enemy.success-clan",
                        this.clanManager.getClanShortName(ClanRelation.ENEMY, target)
                );
            }
        });
        target.getMembers().forEach(member -> {
            Player memberPlayer = Bukkit.getPlayer(member.getUuid());
            if (memberPlayer != null) {
                UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.enemy.success-clan",
                        this.clanManager.getClanShortName(ClanRelation.ENEMY, clan)
                );
            }
        });

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
            UtilMessage.message(event.getPlayer(), "clans.prefix", "clans.command.clan.set-core.no-build-on-top");
        }
    }

    @EventHandler
    public void onBreakCore(final BlockBreakEvent event) {
        if (ClanCore.isCore(event.getBlock())) {
            event.setCancelled(true);
            UtilMessage.message(event.getPlayer(), "clans.prefix", "clans.command.clan.set-core.no-break");
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onClanSetCore(final ClanSetCoreLocationEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final Clan clan = event.getClan();
        final Player player = event.getPlayer();

        if (event.getPlayer().getLocation().getY() > maxCoreY) {
            UtilMessage.message(event.getPlayer(), "clans.prefix", "clans.command.clan.set-core.too-high", Component.text(maxCoreY, NamedTextColor.YELLOW));
            return;
        }


        if (this.clanManager.getPillageHandler().isBeingPillaged(clan)) {
            UtilMessage.message(player, "clans.prefix", "clans.command.clan.set-core.pillaged");
            return;
        }

        if (!event.isIgnoreClaims()) {
            final Optional<Clan> clanOptional = this.clanManager.getClanByLocation(player.getLocation());
            if (clanOptional.isEmpty() || !clanOptional.get().equals(clan)) {
                UtilMessage.message(player, "clans.prefix", "clans.command.clan.set-core.not-territory");
                return;
            }

        }

        final ClanCore core = clan.getCore();
        Location highest = player.getLocation();

        final Block block = core.getSafest(highest).getBlock();
        if (block.getType() == Material.BEDROCK) {
            UtilMessage.message(player, "clans.prefix", "clans.command.clan.set-core.invalid-location");
            return;
        }

        core.removeBlock(); // Remove old core
        Location coreLocation = block.getLocation().toCenterLocation().setDirection(player.getLocation().getDirection());
        core.setPosition(coreLocation); // Set new core location
        core.placeBlock(); // Place new core

        UtilMessage.message(player, "clans.prefix", "clans.command.clan.set-core.success",
                Component.text(UtilWorld.locationToString(player.getLocation()), NamedTextColor.YELLOW)
        );

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
            result.ifPresent(clientFound -> {
                UtilMessage.message(player, "clans.prefix", "clans.command.clan.promote.success-self",
                        this.clanManager.getPlayerName(ClanRelation.SELF, clientFound.getName()),
                        Component.text(member.getRank().getName(), NamedTextColor.YELLOW)
                );

                log.info("{} ({}) was promoted by {} ({}) to {} in {} ({})", clientFound.getName(), member.getUuid(), player.getName(),
                                player.getUniqueId(), member.getRank().getName(), clan.getName(), clan.getId()).setAction("CLAN_PROMOTE")
                        .addClientContext(player).addClientContext(clientFound, true).addClanContext(clan)
                        .addContext(LogContext.CURRENT_CLAN_RANK, rank.getName()).addContext(LogContext.NEW_CLAN_RANK, member.getRank().getName()).submit();

            });
        });


        final Player memberPlayer = Bukkit.getPlayer(member.getUuid());
        if (memberPlayer != null) {
            UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.promote.success-target", Component.text(member.getRank().getName(), NamedTextColor.YELLOW));
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
            result.ifPresent(clientFound -> {
                UtilMessage.message(player, "clans.prefix", "clans.command.clan.demote.success-self",
                        this.clanManager.getPlayerName(ClanRelation.SELF, clientFound.getName()),
                        Component.text(member.getRank().getName(), NamedTextColor.YELLOW)
                );

                log.info("{} ({}) was demoted by {} ({}) to {} in {} ({})", clientFound.getName(), member.getUuid(),
                                player.getName(), player.getUniqueId(), member.getRank().getName(), clan.getName(), clan.getId())
                        .setAction("CLAN_DEMOTE").addClientContext(player).addClientContext(clientFound, true).addClanContext(clan)
                        .addContext(LogContext.CURRENT_CLAN_RANK, rank.getName()).addContext(LogContext.NEW_CLAN_RANK, member.getRank().getName()).submit();

            });
        });

        final Player memberPlayer = Bukkit.getPlayer(member.getUuid());
        if (memberPlayer != null) {
            UtilMessage.message(memberPlayer, "clans.prefix", "clans.command.clan.demote.success-target", Component.text(member.getRank().getName(), NamedTextColor.YELLOW));
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
