package me.mykindos.betterpvp.core.client.stats.commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.client.stats.StatConcurrentHashMap;
import me.mykindos.betterpvp.core.client.stats.StatFilterType;
import me.mykindos.betterpvp.core.client.stats.impl.ClientStat;
import me.mykindos.betterpvp.core.client.stats.impl.GenericStat;
import me.mykindos.betterpvp.core.client.stats.impl.IStat;
import me.mykindos.betterpvp.core.client.stats.impl.champions.ChampionsSkillStat;
import me.mykindos.betterpvp.core.client.stats.impl.clans.ClanWrapperStat;
import me.mykindos.betterpvp.core.client.stats.impl.dungeons.DungeonNativeStat;
import me.mykindos.betterpvp.core.client.stats.impl.dungeons.DungeonWrapperStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapNativeStat;
import me.mykindos.betterpvp.core.client.stats.impl.game.GameTeamMapWrapperStat;
import me.mykindos.betterpvp.core.command.Command;
import me.mykindos.betterpvp.core.command.IConsoleCommand;
import me.mykindos.betterpvp.core.server.Realm;
import me.mykindos.betterpvp.core.server.Season;
import me.mykindos.betterpvp.core.server.Server;
import me.mykindos.betterpvp.core.utilities.UtilMath;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Dev profiling command that seeds a player's in-memory stat map with a large, realistic dataset.
 * <p>
 * The generated data intentionally emphasizes stat families that read through {@code getFilteredStat()},
 * such as:
 * <ul>
 *     <li>{@link ChampionsSkillStat} TIME_PLAYED across many skills / levels (UseAllSkills-style queries)</li>
 *     <li>{@link DungeonNativeStat} generic dungeon action queries</li>
 *     <li>{@link DungeonWrapperStat} generic dungeon wrapper queries</li>
 *     <li>{@link ClanWrapperStat} generic clan wrapper queries</li>
 *     <li>{@link GameTeamMapNativeStat} generic game/map/team queries</li>
 *     <li>{@link GameTeamMapWrapperStat} generic game wrapper queries</li>
 * </ul>
 * The command writes directly into {@link StatConcurrentHashMap} with silent puts so the dataset is prepared
 * for spark profiling without spending the profiling budget on achievement / stat update events during seeding.
 */
@Singleton
public class ProfileStatsCommand extends Command implements IConsoleCommand {

    private static final int SEASON_COUNT = 5;
    private static final int REALMS_PER_SEASON = 3;
    private static final int SKILL_LEVELS = 5;
    private static final int MAPS_PER_GAME = 10;
    private static final String[] TEAM_NAMES = {"Red", "Blue"};
    private static final ClientStat[] WRAPPED_BASE_STATS = {
            ClientStat.PLAYER_KILLS,
            ClientStat.PLAYER_DEATHS,
            ClientStat.TIME_PLAYED
    };
    private static final DungeonNativeStat.Action[] DUNGEON_ACTIONS = {
            DungeonNativeStat.Action.ENTER,
            DungeonNativeStat.Action.WIN,
            DungeonNativeStat.Action.LOSS,
            DungeonNativeStat.Action.BOSS_KILL
    };
    private static final GameTeamMapNativeStat.Action[] GAME_NATIVE_ACTIONS = {
            GameTeamMapNativeStat.Action.GAME_TIME_PLAYED,
            GameTeamMapNativeStat.Action.MATCHES_PLAYED,
            GameTeamMapNativeStat.Action.WIN
    };

    private final ClientManager clientManager;

    @Inject
    public ProfileStatsCommand(ClientManager clientManager) {
        this.clientManager = clientManager;
        aliases.add("seedprofilestats");
        aliases.add("profilestatsload");
    }

    @Override
    public String getName() {
        return "profilestats";
    }

    @Override
    public String getDescription() {
        return "Seed realistic filtered-stat-heavy profiling data into one or more online players";
    }

    @Override
    public void execute(Player player, Client client, String... args) {
        executeInternal(player, args);
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        executeInternal(sender, args);
    }

    @Override
    public Rank getRequiredRank() {
        return Rank.DEVELOPER;
    }

    @Override
    public String getArgumentType(int argCount) {
        return argCount == 1 ? ArgumentType.PLAYER.name() : ArgumentType.NONE.name();
    }

    private void executeInternal(CommandSender sender, String[] args) {
        ParsedArgs parsed = parseArgs(sender, args);
        if (parsed == null) {
            return;
        }

        final List<Realm> profilingRealms = buildProfilingRealms();
        int totalEntries = 0;
        long totalReadMs = 0;
        long totalReads = 0;
        for (Player target : parsed.targets()) {
            final Client targetClient = clientManager.search().online(target);
            totalEntries += seedProfilingStats(targetClient, profilingRealms, parsed.multiplier());
            final long[] readResult = readProfilingStats(targetClient, profilingRealms, parsed.multiplier());
            totalReadMs += readResult[0];
            totalReads  += readResult[1];
        }

        final int perPlayer = parsed.targets().isEmpty() ? 0 : totalEntries / parsed.targets().size();
        UtilMessage.simpleMessage(sender, "Stats",
                "Seeded <green>%s</green> entries across <green>%s</green> realms for <green>%s</green> player(s) (~<green>%s</green>/player, ×<green>%s</green>).",
                totalEntries, profilingRealms.size(), parsed.targets().size(), perPlayer, parsed.multiplier());
        UtilMessage.simpleMessage(sender, "Stats",
                "Read phase: <green>%s</green> getFilteredStat queries across all targets in <green>%s ms</green>.",
                totalReads, totalReadMs);
        UtilMessage.simpleMessage(sender, "Stats",
                "Profiling data is hot. Use <green>/spark profiler</green> then trigger stat menus or achievement checks.");
    }

    private ParsedArgs parseArgs(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof Player player) {
                return new ParsedArgs(List.of(player), 1);
            }
            sendUsage(sender);
            return null;
        }

        if (args.length > 2) {
            sendUsage(sender);
            return null;
        }

        if (isInteger(args[0])) {
            if (!(sender instanceof Player player)) {
                UtilMessage.simpleMessage(sender, "Stats", "Console must specify a <player> or <all> target before the multiplier.");
                return null;
            }
            return new ParsedArgs(List.of(player), UtilMath.getInteger(args[0], 1, 50));
        }

        final Collection<? extends Player> targets;
        if (args[0].equalsIgnoreCase("all")) {
            targets = Bukkit.getOnlinePlayers();
        } else {
            Player target = Bukkit.getPlayerExact(args[0]);
            if (target == null) {
                UtilMessage.simpleMessage(sender, "Stats", "Could not find online player <red>%s</red>.", args[0]);
                return null;
            }
            targets = List.of(target);
        }

        if (targets.isEmpty()) {
            UtilMessage.simpleMessage(sender, "Stats", "There are no matching online players to seed.");
            return null;
        }

        int multiplier = args.length == 2 ? UtilMath.getInteger(args[1], 1, 50) : 1;
        return new ParsedArgs(new ArrayList<>(targets), multiplier);
    }

    private void sendUsage(CommandSender sender) {
        UtilMessage.simpleMessage(sender, "Stats", "Usage: /profilestats [player|all|multiplier] [multiplier]");
        UtilMessage.simpleMessage(sender, "Stats", "Examples: /profilestats | /profilestats 3 | /profilestats all 2 | /profilestats Owen 5");
    }

    private boolean isInteger(String value) {
        try {
            Integer.parseInt(value);
            return true;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }

    private List<Realm> buildProfilingRealms() {
        final Realm currentRealm = Core.getCurrentRealm();
        final List<Realm> realms = new ArrayList<>(SEASON_COUNT * REALMS_PER_SEASON);
        realms.add(currentRealm);

        int realmId = 900_000;
        int serverId = 900_000;

        // Fill out the current season first so current-season views have realistic data.
        for (int i = 1; i < REALMS_PER_SEASON; i++) {
            realms.add(new Realm(realmId++, new Server(serverId++, currentRealm.getServer().getName() + "-profile-" + i), currentRealm.getSeason()));
        }

        for (int s = 1; s < SEASON_COUNT; s++) {
            Season season = new Season(
                    currentRealm.getSeason().getId() + s,
                    currentRealm.getSeason().getName() + " Profile " + s,
                    currentRealm.getSeason().getStart().minusMonths((long) s * 3)
            );
            for (int r = 0; r < REALMS_PER_SEASON; r++) {
                realms.add(new Realm(realmId++, new Server(serverId++, "profile-server-" + s + "-" + r), season));
            }
        }

        return realms;
    }

    private int seedProfilingStats(Client client, List<Realm> realms, int multiplier) {
        final StatConcurrentHashMap stats = client.getStatContainer().getStats();
        int entries = 0;

        final int skillCount = 20 * multiplier;
        final int dungeonCount = 8 * multiplier;
        final int clanCount = 10 * multiplier;
        final int gameCount = 6 * multiplier;

        for (Realm realm : realms) {
            // A few direct base stats for baseline load.
            entries += put(stats, realm, ClientStat.PLAYER_KILLS, 250L + realm.getId());
            entries += put(stats, realm, ClientStat.PLAYER_DEATHS, 180L + realm.getId());
            entries += put(stats, realm, ClientStat.PLAYER_KILL_ASSISTS, 140L + realm.getId());
            entries += put(stats, realm, ClientStat.TIME_PLAYED, 3_600_000L + realm.getId());
            entries += put(stats, realm, ClientStat.CLANS_CANNON_SHOT, 75L + realm.getId());
            entries += put(stats, realm, ClientStat.CLANS_DESTROY_CORE, 25L + realm.getId());

            // UseAllSkills-style load: many skill names with many explicit levels.
            for (int skill = 0; skill < skillCount; skill++) {
                for (int level = 1; level <= SKILL_LEVELS; level++) {
                    IStat stat = ChampionsSkillStat.builder()
                            .action(ChampionsSkillStat.Action.TIME_PLAYED)
                            .skillName("ProfileSkill_" + skill)
                            .level(level)
                            .build();
                    entries += put(stats, realm, stat, 60_000L * (level + 1L) + skill);
                }
            }

            // Dungeon native + wrapper stats.
            for (int dungeon = 0; dungeon < dungeonCount; dungeon++) {
                final String dungeonName = "ProfileDungeon_" + dungeon;
                for (DungeonNativeStat.Action action : DUNGEON_ACTIONS) {
                    entries += put(stats, realm, DungeonNativeStat.builder()
                            .action(action)
                            .dungeonName(dungeonName)
                            .build(), 50L + dungeon);
                }
                for (ClientStat wrapped : WRAPPED_BASE_STATS) {
                    entries += put(stats, realm, DungeonWrapperStat.builder()
                            .dungeonName(dungeonName)
                            .wrappedStat(wrapped)
                            .build(), 100L + dungeon);
                }
            }

            // Clan wrappers around both client stats and dungeon stats.
            for (int clan = 0; clan < clanCount; clan++) {
                final String clanName = "ProfileClan_" + clan;
                final long clanId = 1_000_000L + clan;
                for (ClientStat wrapped : WRAPPED_BASE_STATS) {
                    entries += put(stats, realm, ClanWrapperStat.builder()
                            .clanName(clanName)
                            .clanId(clanId)
                            .wrappedStat(wrapped)
                            .build(), 200L + clan);
                }
                for (int dungeon = 0; dungeon < Math.min(dungeonCount, 4 * multiplier); dungeon++) {
                    entries += put(stats, realm, ClanWrapperStat.builder()
                            .clanName(clanName)
                            .clanId(clanId)
                            .wrappedStat(DungeonNativeStat.builder()
                                    .action(DungeonNativeStat.Action.ENTER)
                                    .dungeonName("ProfileDungeon_" + dungeon)
                                    .build())
                            .build(), 20L + dungeon + clan);
                }
            }

            // Game native + wrapper stats with explicit game/map/team context.
            for (int game = 0; game < gameCount; game++) {
                final long gameId = ((long) realm.getId() * 10_000L) + game;
                final String gameName = "ProfileGame_" + game;
                for (int mapIndex = 0; mapIndex < MAPS_PER_GAME; mapIndex++) {
                    final String mapName = "Map_" + mapIndex;
                    for (String team : TEAM_NAMES) {
                        for (GameTeamMapNativeStat.Action action : GAME_NATIVE_ACTIONS) {
                            entries += put(stats, realm, GameTeamMapNativeStat.builder()
                                    .gameId(gameId)
                                    .gameName(gameName)
                                    .mapName(mapName)
                                    .teamName(team)
                                    .action(action)
                                    .build(), 300L + game + mapIndex);
                        }
                        for (ClientStat wrapped : WRAPPED_BASE_STATS) {
                            entries += put(stats, realm, GameTeamMapWrapperStat.builder()
                                    .gameId(gameId)
                                    .gameName(gameName)
                                    .mapName(mapName)
                                    .teamName(team)
                                    .wrappedStat(wrapped)
                                    .build(), 400L + game + mapIndex);
                        }
                    }
                }
            }
        }

        return entries;
    }

    /**
     * Performs a burst of stat reads that exercise the O(n) {@code getFilteredStat} paths.
     * This is where the real CPU cost lives during achievement checks and stat-menu rendering.
     *
     * @return [elapsedMs, readCount]
     */
    private long[] readProfilingStats(Client client, List<Realm> realms, int multiplier) {
        final StatConcurrentHashMap stats = client.getStatContainer().getStats();
        final int skillCount = 20 * multiplier;
        final int dungeonCount = 8 * multiplier;
        final int clanCount = 10 * multiplier;
        final int gameCount = 6 * multiplier;
        final int repeats = 5; // simulate multiple achievement checks / menu renders per command

        // Build query stats once — these are the non-savable, partial, and generic stats
        // that force the O(n) filtered-scan path.
        final List<IStat> queries = new ArrayList<>();

        // GenericStat(ClientStat) — O(1) leaf aggregate path, but included for baseline comparison.
        for (ClientStat base : WRAPPED_BASE_STATS) {
            queries.add(new GenericStat(base));
        }

        // Partial skill queries (no level) — UseAllSkillsAchievement style, always O(n).
        for (int skill = 0; skill < skillCount; skill++) {
            final IStat partialSkillStat = ChampionsSkillStat.builder()
                    .action(ChampionsSkillStat.Action.TIME_PLAYED)
                    .skillName("ProfileSkill_" + skill)
                    .level(-1)    // level = -1 means "not savable", triggers O(n) filtered scan
                    .build();
            queries.add(new GenericStat(partialSkillStat));
        }

        // GenericStat wrapping DungeonNativeStat — dungeon achievement / stat menu reads.
        for (int dungeon = 0; dungeon < dungeonCount; dungeon++) {
            for (DungeonNativeStat.Action action : DUNGEON_ACTIONS) {
                queries.add(new GenericStat(DungeonNativeStat.builder()
                        .action(action)
                        .dungeonName("ProfileDungeon_" + dungeon)
                        .build()));
            }
        }

        // GenericStat wrapping ClanWrapperStat — clan stat menu reads.
        for (int clan = 0; clan < Math.min(clanCount, 5 * multiplier); clan++) {
            for (ClientStat wrapped : WRAPPED_BASE_STATS) {
                queries.add(new GenericStat(ClanWrapperStat.builder()
                        .clanName("ProfileClan_" + clan)
                        .clanId(1_000_000L + clan)
                        .wrappedStat(wrapped)
                        .build()));
            }
        }

        // GenericStat wrapping GameTeamMapNativeStat — game stat menu reads.
        for (int game = 0; game < Math.min(gameCount, 4 * multiplier); game++) {
            final long gameId = ((long) realms.get(0).getId() * 10_000L) + game;
            for (GameTeamMapNativeStat.Action action : GAME_NATIVE_ACTIONS) {
                queries.add(new GenericStat(GameTeamMapNativeStat.builder()
                        .gameId(gameId)
                        .gameName("ProfileGame_" + game)
                        .mapName("Map_0")
                        .teamName("Red")
                        .action(action)
                        .build()));
            }
        }

        final StatFilterType filterType = StatFilterType.ALL;
        long reads = 0;
        final long start = System.nanoTime();
        for (int r = 0; r < repeats; r++) {
            for (IStat query : queries) {
                // Use the StatConcurrentHashMap.get() path that calls IStat.getStat() / getFilteredStat().
                stats.get(filterType, null, query);
                reads++;
            }
        }
        final long elapsedMs = (System.nanoTime() - start) / 1_000_000L;
        return new long[]{elapsedMs, reads};
    }

    private int put(StatConcurrentHashMap stats, Realm realm, IStat stat, long value) {
        stats.put(realm, stat, value, true);
        return 1;
    }

    private record ParsedArgs(List<Player> targets, int multiplier) {
    }
}




