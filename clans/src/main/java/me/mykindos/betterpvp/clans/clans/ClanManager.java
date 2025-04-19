package me.mykindos.betterpvp.clans.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.CustomLog;
import lombok.Getter;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.events.ClanDominanceChangeEvent;
import me.mykindos.betterpvp.clans.clans.insurance.Insurance;
import me.mykindos.betterpvp.clans.clans.insurance.InsuranceType;
import me.mykindos.betterpvp.clans.clans.leaderboard.ClanLeaderboard;
import me.mykindos.betterpvp.clans.clans.leveling.ClanPerkManager;
import me.mykindos.betterpvp.clans.clans.pillage.Pillage;
import me.mykindos.betterpvp.clans.clans.pillage.PillageHandler;
import me.mykindos.betterpvp.clans.clans.pillage.events.PillageStartEvent;
import me.mykindos.betterpvp.clans.clans.repository.ClanRepository;
import me.mykindos.betterpvp.clans.commands.arguments.exceptions.ClanArgumentException;
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.clans.utilities.UtilClans;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.command.brigadier.arguments.ArgumentException;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.inviting.InviteHandler;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

@CustomLog
@Singleton
public class ClanManager extends Manager<Clan> {

    private final Clans clans;

    @Getter
    private final ClanRepository repository;

    private final ClientManager clientManager;
    @Getter
    private final PillageHandler pillageHandler;

    private final LeaderboardManager leaderboardManager;

    private final InviteHandler inviteHandler;

    private Map<Integer, Double> dominanceScale;

    @Getter
    private final ConcurrentLinkedQueue<Insurance> insuranceQueue;

    @Inject
    @Config(path = "clans.claims.baseAmountOfClaims", defaultValue = "3")
    private int baseAmountOfClaims;

    @Inject
    @Config(path = "clans.claims.bonusClaimsPerMember", defaultValue = "1")
    private int bonusClaimsPerMember;

    @Inject
    @Config(path = "clans.claims.maxAmountOfClaims", defaultValue = "9")
    private int maxAmountOfClaims;

    @Inject
    @Config(path = "clans.claims.disbandCooldown", defaultValue = "900.0")
    private double claimDisbandCooldown;

    @Inject
    @Getter
    @Config(path = "clans.pillage.enabled", defaultValue = "true")
    private boolean pillageEnabled;

    @Inject
    @Getter
    @Config(path = "clans.dominance.enabled", defaultValue = "true")
    private boolean dominanceEnabled;

    @Inject
    @Config(path = "clans.dominance.fixed.enabled", defaultValue = "true")
    private boolean fixedDominanceGain;

    @Inject
    @Config(path = "clans.dominance.fixed.delta", defaultValue = "5.0")
    private double dominanceGain;

    @Inject
    @Config(path = "clans.members.max", defaultValue = "8")
    private int maxClanMembers;

    @Inject
    @Config(path = "clan.name.maxCharactersInClanName", defaultValue = "13")
    @Getter
    private int maxCharactersInClanName;

    @Inject
    @Config(path = "clan.name.minCharactersInClanName", defaultValue = "3")
    @Getter
    private int minCharactersInClanName;

    @Inject
    public ClanManager(Clans clans, ClanRepository repository, ClientManager clientManager, PillageHandler pillageHandler, LeaderboardManager leaderboardManager, InviteHandler inviteHandler) {
        this.clans = clans;
        this.repository = repository;
        this.clientManager = clientManager;
        this.pillageHandler = pillageHandler;
        this.leaderboardManager = leaderboardManager;
        this.inviteHandler = inviteHandler;
        this.dominanceScale = new HashMap<>();
        this.insuranceQueue = new ConcurrentLinkedQueue<>();

        dominanceScale = repository.getDominanceScale();

        ClanPerkManager.getInstance().init();
    }

    public void updateClanName(Clan clan) {
        getRepository().updateClanName(clan);
    }

    public Optional<Clan> getClanById(@Nullable UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(objects.get(id.toString()));
    }

    public Optional<Clan> getClanByClient(Client client) {
        return getClanByPlayer(client.getUniqueId());
    }

    public Optional<Clan> expensiveGetClanByPlayer(Player player) {
        return objects.values().stream()
                .filter(clan -> clan.getMemberByUUID(player.getUniqueId()).isPresent()).findFirst();

    }

    public Optional<Clan> getClanByPlayer(@Nullable Player player) {

        if (player != null && player.hasMetadata("clan")) {
            List<MetadataValue> clan = player.getMetadata("clan");
            if (!clan.isEmpty()) {
                return Optional.ofNullable(clan.getFirst().value())
                        .map(UUID.class::cast)
                        .flatMap(this::getClanById);
            }
        }


        return Optional.empty();
    }

    public Optional<Clan> getClanByPlayer(UUID uuid) {
        final Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return objects.values().stream()
                    .filter(clan -> clan.getMemberByUUID(uuid).isPresent()).findFirst();
        }

        return getClanByPlayer(player);
    }

    public Optional<Clan> getClanByName(String name) {
        return objects.values().stream().filter(clan -> clan.getName().equalsIgnoreCase(name)).findFirst();
    }

    /**
     * Finds a clan if the location is within a claimed chunk
     *
     * @param location The location to check
     * @return a Clan optional
     */
    public Optional<Clan> getClanByLocation(Location location) {
        return getClanByChunk(location.getChunk());
    }

    public Optional<Clan> getClanByChunk(Chunk chunk) {
        final UUID uuid = chunk.getPersistentDataContainer().get(ClansNamespacedKeys.CLAN, CustomDataType.UUID);
        if (uuid == null) {
            return Optional.empty();
        }

        return getClanById(uuid);
    }

    /**
     * Checks to see if a chunk is adjacent to another clan
     * This checks the ordinal and diagonal chunks
     *
     * @param chunk the chuck to check for adjacent
     * @param clan  the that should be compared to other clans
     * @return True if adjacent to a different clan, false otherwise
     */
    public boolean adjacentOtherClans(@NotNull Chunk chunk, @NotNull Clan clan) {
        World world = chunk.getWorld();
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                Chunk testedChunk = world.getChunkAt(chunk.getX() + x, chunk.getZ() + z);
                Optional<Clan> nearbyClanOptional = this.getClanByChunk(testedChunk);
                if (nearbyClanOptional.isPresent()) {
                    Clan nearbyClan = nearbyClanOptional.get();
                    if (clan.equals(nearbyClan)) {
                        continue;
                    }
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks the original direction to see if a claim is next to its self
     *
     * @param chunk The chunk to check
     * @param clan  the Clan to compare against
     * @return True if it is adjacent in at least one of the ordinal directions
     */
    public boolean adjacentToOwnClan(@NotNull Chunk chunk, @NotNull Clan clan) {
        World world = chunk.getWorld();
        List<Chunk> chunks = new ArrayList<>();
        //north
        chunks.add(world.getChunkAt(chunk.getX(), chunk.getZ() + 1));
        //south
        chunks.add(world.getChunkAt(chunk.getX(), chunk.getZ() - 1));
        //east
        chunks.add(world.getChunkAt(chunk.getX() - 1, chunk.getZ()));
        //west
        chunks.add(world.getChunkAt(chunk.getX() + 1, chunk.getZ()));

        for (Chunk checkChunk : chunks) {
            Clan checkChunkClan = getClanByChunk(checkChunk).orElse(null);
            if (clan.equals(checkChunkClan)) {
                return true;
            }
        }
        return false;
    }

    public void applyDisbandClaimCooldown(ClanTerritory clanTerritory) {
        setClaimCooldown(clanTerritory.getWorldChunk(), (long) (claimDisbandCooldown * 1000L));
    }

    public void setClaimCooldown(Chunk chunk, long duration) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        pdc.set(ClansNamespacedKeys.CLAIM_COOLDOWN, PersistentDataType.LONG, System.currentTimeMillis() + duration);
    }

    public long getRemainingClaimCooldown(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        final long endTime = pdc.getOrDefault(ClansNamespacedKeys.CLAIM_COOLDOWN, PersistentDataType.LONG, 0L);
        if (endTime < System.currentTimeMillis()) {
            return 0;
        }
        return endTime - System.currentTimeMillis();
    }

    public Optional<Clan> getClanByChunkString(String serialized) {
        return objects.values().stream()
                .filter(clan -> clan.getTerritory().stream()
                        .anyMatch(territory -> territory.getChunk().equalsIgnoreCase(serialized))).findFirst();
    }

    public boolean isClanMember(Player player, Player target) {
        Optional<Clan> aClanOptional = getClanByPlayer(player);
        Optional<Clan> bClanOptional = getClanByPlayer(target);

        if (aClanOptional.isEmpty() || bClanOptional.isEmpty()) return false;

        return aClanOptional.equals(bClanOptional);

    }

    public ClanRelation getRelation(@Nullable IClan clanA, @Nullable IClan clanB) {
        if (clanA == null || clanB == null) {
            return ClanRelation.NEUTRAL;
        } else if (clanA.equals(clanB)) {
            return ClanRelation.SELF;
        } else if (clanB.hasTrust(clanA)) {
            return ClanRelation.ALLY_TRUST;
        } else if (clanA.isAllied(clanB)) {
            return ClanRelation.ALLY;
        } else if (clanA.isEnemy(clanB)) {
            return ClanRelation.ENEMY;
        } else if (pillageHandler.isPillaging(clanA, clanB)) {
            return ClanRelation.PILLAGE;
        } else if (pillageHandler.isPillaging(clanB, clanA)) {
            return ClanRelation.PILLAGE;
        }

        return ClanRelation.NEUTRAL;
    }

    public boolean hasAccess(Player player, Location location) {
        Optional<Clan> playerClanOptional = getClanByPlayer(player);
        Optional<Clan> locationClanOptional = getClanByLocation(location);

        Client client = clientManager.search().online(player);
        if (client.isAdministrating()) {
            return true;
        }

        if (locationClanOptional.isEmpty()) return true;
        if (playerClanOptional.isEmpty()) return false;

        Clan playerClan = playerClanOptional.get();
        Clan locationClan = locationClanOptional.get();

        if (pillageHandler.isPillaging(playerClan, locationClan) && locationClan.getCore().isDead()) {
            return true;
        }

        ClanRelation relation = getRelation(playerClan, locationClan);

        return relation == ClanRelation.SELF || (relation == ClanRelation.ALLY_TRUST && locationClan.isOnline());
    }

    public Location closestWilderness(Player player) {
        int maxChunksRadiusToScan = 3;
        List<Chunk> chunks = new ArrayList<>();

        Chunk playerChunk = player.getChunk();
        World world = player.getWorld();

        for (int i = -maxChunksRadiusToScan; i < maxChunksRadiusToScan; i++) {
            for (int j = -maxChunksRadiusToScan; j < maxChunksRadiusToScan; j++) {
                Chunk chunk = world.getChunkAt(playerChunk.getX() + i, playerChunk.getZ() + j);
                if (getClanByChunk(chunk).isEmpty()) {
                    chunks.add(chunk);
                }
            }
        }

        if (!chunks.isEmpty()) {
            Chunk chunk = UtilWorld.closestChunkToPlayer(chunks, player);

            //this should not ever happen
            if (chunk == null) return null;

            List<Location> locations = new ArrayList<>();

            int y = (int) player.getY();
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    locations.add(chunk.getBlock(i, y, j).getLocation().toHighestLocation());
                }
            }

            locations.sort(Comparator.comparingInt(a -> (int) player.getLocation().distanceSquared(a)));

            //to prevent getting stuck in a block, add 1 to Y
            return locations.get(0).add(0.5, 1, 0.5);
        }
        return null;
    }


    public Location closestWildernessBackwards(Player player) {
        List<Location> locations = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            Location location = player.getLocation().add(player.getLocation().getDirection().multiply(i * -1));
            Optional<Clan> clanOptional = getClanByLocation(location);
            if (clanOptional.isEmpty()) {
                locations.add(location);
                break;
            }
        }

        if (!locations.isEmpty()) {

            locations.sort(Comparator.comparingInt(a -> (int) player.getLocation().distance(a)));
            return locations.get(0);
        }
        return null;

    }

    public int getMaximumClaimsForClan(Clan clan) {
        return Math.min(maxAmountOfClaims, baseAmountOfClaims + (clan.getMembers().size() * bonusClaimsPerMember));
    }

    public Component getClanTooltip(Player player, Clan target) {
        Clan clan = getClanByPlayer(player).orElse(null);
        var territoryString = target.getTerritory().size() + "/" + getMaximumClaimsForClan(target);

        return Component.text(target.getName() + " Information").color(getRelation(clan, target).getPrimary())
                .appendNewline()
                .append(Component.text(" Age: ").color(NamedTextColor.WHITE).append(UtilMessage.getMiniMessage("<yellow>%s", target.getAge())))
                .appendNewline()
                .append(Component.text(" Territory: ").color(NamedTextColor.WHITE).append(UtilMessage.getMiniMessage("<yellow>%s", territoryString)))
                .appendNewline()
                .append(Component.text(" Allies: ").color(NamedTextColor.WHITE).append(UtilMessage.getMiniMessage(getAllianceList(player, target))))
                .appendNewline()
                //.append(Component.text(" Enemies: ").color(NamedTextColor.WHITE).append(UtilMessage.getMiniMessage(getEnemyList(player, target))))
                //.appendNewline()
                .append(Component.text(" Members: ").color(NamedTextColor.WHITE).append(UtilMessage.getMiniMessage("%s", getMembersList(target))));
    }

    public String getAllianceList(Player player, Clan clan) {
        Clan playerClan = getClanByPlayer(player).orElse(null);
        List<String> allies = new ArrayList<>();

        if (!clan.getAlliances().isEmpty()) {
            for (ClanAlliance enemy : clan.getAlliances()) {
                ClanRelation relation = getRelation(playerClan, enemy.getClan());
                allies.add(relation.getPrimaryMiniColor() + enemy.getClan().getName());
            }
        }
        return String.join("<gray>, ", allies);
    }

    public String getEnemyList(Player player, Clan clan) {
        Clan playerClan = getClanByPlayer(player).orElse(null);
        List<String> enemies = new ArrayList<>();

        if (!clan.getEnemies().isEmpty()) {
            for (ClanEnemy enemy : clan.getEnemies()) {
                ClanRelation relation = getRelation(playerClan, enemy.getClan());
                enemies.add(relation.getPrimaryMiniColor() + enemy.getClan().getName());
            }
        }
        return String.join("<gray>, ", enemies);
    }

    public String getEnemyListDom(Player player, Clan clan) {
        Clan playerClan = getClanByPlayer(player).orElse(null);
        List<String> enemies = new ArrayList<>();

        if (!clan.getEnemies().isEmpty()) {
            for (ClanEnemy enemy : clan.getEnemies()) {
                ClanRelation relation = getRelation(playerClan, enemy.getClan());
                enemies.add((relation.getPrimaryMiniColor() + enemy.getClan().getName() + " " + getDominanceString(clan, enemy.getClan())).trim());
            }
        }
        return String.join("<gray>, ", enemies);
    }

    public String getMembersList(Clan clan) {
        StringBuilder membersString = new StringBuilder();
        if (clan.getMembers() != null && !clan.getMembers().isEmpty()) {
            for (ClanMember member : clan.getMembers()) {

                    membersString.append(!membersString.isEmpty() ? "<gray>, " : "").append("<yellow>")
                            .append(member.getRoleIcon())
                            .append(UtilFormat.getOnlineStatus(member.getUuid()))
                            .append(UtilFormat.spoofNameForLunar(member.getClientName()));

            }
        }
        return membersString.toString();
    }

    public boolean canTeleport(Player player) {
        Gamer gamer = clientManager.search().online(player).getGamer();
        return !gamer.isInCombat();
    }

    public boolean isAlly(Player player, Player target) {
        return player.equals(target) || getAllies(player).stream().anyMatch(o -> Objects.equals(o.getUuid(), target.getUniqueId().toString()));
    }

    public List<ClanMember> getAllies(Player player) {
        ArrayList<ClanMember> allyList = new ArrayList<>(List.of());
        Optional<Clan> clanOptional = getClanByPlayer(player);

        if (clanOptional.isEmpty()) {
            return allyList;
        }

        Clan mainClan = clanOptional.get();
        allyList.addAll(mainClan.getMembers());

        for (ClanAlliance clanAlliance : mainClan.getAlliances()) {
            allyList.addAll(clanAlliance.getClan().getMembers());
        }
        return allyList;
    }

    public boolean canHurt(Player player, Player target) {
        Clan playerClan = getClanByPlayer(player).orElse(null);
        Clan targetClan = getClanByPlayer(target).orElse(null);
        ClanRelation relation = getRelation(playerClan, targetClan);

        Clan targetLocationClan = getClanByLocation(target.getLocation()).orElse(null);
        if (targetLocationClan != null && targetLocationClan.isSafe()) {

            if (targetLocationClan.getName().toLowerCase().contains("shop")) {
                if(relation == ClanRelation.PILLAGE) {
                    return true;
                }
            }

            Gamer gamer = clientManager.search().online(target).getGamer();
            if (!gamer.isInCombat() && relation != ClanRelation.PILLAGE) {
                return false;
            }
        }

        return relation != ClanRelation.SELF && relation != ClanRelation.ALLY && relation != ClanRelation.ALLY_TRUST;
    }

    public boolean canCast(Player player) {
        Optional<Clan> locationClanOptional = getClanByLocation(player.getLocation());
        if (locationClanOptional.isPresent()) {
            Clan locationClan = locationClanOptional.get();
            if (locationClan.isAdmin() && locationClan.isSafe()) {

                if (locationClan.getName().toLowerCase().contains("shop")) {
                    Clan playerClan = getClanByPlayer(player).orElse(null);
                    if (playerClan != null) {
                        // Allow using skills anywhere while participating in a pillage
                        if (getPillageHandler().getActivePillages().stream().anyMatch(pillage -> pillage.getPillager().getName().equals(playerClan.getName())
                                || pillage.getPillaged().getName().equals(playerClan.getName()))) {
                            return true;
                        }
                    }
                }

                Gamer gamer = clientManager.search().online(player).getGamer();
                return gamer.isInCombat();
            }
        }

        return true;
    }

    public double getDominanceForKill(int killedSquadSize, int killerSquadSize) {
        if (fixedDominanceGain) {
            return dominanceGain;
        }

        int sizeOffset = Math.min(maxClanMembers, maxClanMembers - Math.min(killerSquadSize - killedSquadSize, maxClanMembers));
        return dominanceScale.getOrDefault(sizeOffset, 6D);
    }

    public void applyDominance(IClan killed, IClan killer) {
        if (!dominanceEnabled) return;
        if (killed == null || killer == null) return;
        if (killed.equals(killer)) return;
        if (!killed.isEnemy(killer)) return;

        ClanEnemy killedEnemy = killed.getEnemy(killer).orElseThrow();
        ClanEnemy killerEnemy = killer.getEnemy(killed).orElseThrow();

        int killerSize = killer.getMembers().size();
        int killedSize = killed.getMembers().size();

        double dominance = getDominanceForKill(killedSize, killerSize);

        if (!pillageEnabled && (killerEnemy.getDominance() + dominance) >= 100) {
            //pillaging is disabled, so stop a pillage from happening
            return;
        }

        // If the killed players clan has no dominance on the killer players clan, then give dominance to the killer
        if (killedEnemy.getDominance() == 0) {
            killerEnemy.addDominance(dominance);
        }
        killedEnemy.takeDominance(dominance);

        UtilServer.callEvent(new ClanDominanceChangeEvent(null, killer));
        UtilServer.callEvent(new ClanDominanceChangeEvent(null, killed));

        killed.messageClan("You lost <red>" + dominance + "%<gray> dominance to <red>" + killer.getName() + getDominanceString(killed, killer), null, true);
        killer.messageClan("You gained <green>" + dominance + "%<gray> dominance on <red>" + killed.getName() + getDominanceString(killer, killed), null, true);

        getRepository().updateDominance(killed, killedEnemy);
        getRepository().updateDominance(killer, killerEnemy);

        if (killerEnemy.getDominance() == 100) {
            UtilServer.callEvent(new PillageStartEvent(new Pillage(killer, killed)));
        }
    }

    public String getDominanceString(IClan clan, IClan enemyClan) {
        Optional<ClanEnemy> enemyOptional = clan.getEnemy(enemyClan);
        Optional<ClanEnemy> theirEnemyOptional = enemyClan.getEnemy(clan);
        if (enemyOptional.isPresent() && theirEnemyOptional.isPresent()) {

            ClanEnemy enemy = enemyOptional.get();
            ClanEnemy theirEnemy = theirEnemyOptional.get();


            String text;
            if (enemy.getDominance() > 0) {
                boolean nextKillDoms = enemy.getDominance() + getDominanceForKill(enemyClan.getSquadCount(), clan.getSquadCount()) >= 100;
                text = (nextKillDoms ? "<light_purple>+" : "<green>+") + enemy.getDominance() + "%";
            } else if (theirEnemy.getDominance() > 0) {
                boolean nextKillDoms = theirEnemy.getDominance() + getDominanceForKill(clan.getSquadCount(), enemyClan.getSquadCount()) >= 100;
                text = (nextKillDoms ? "<light_purple>-" : "<red>-") + theirEnemy.getDominance() + "%";
            } else {
                return "";
            }
            return "<gray> (" + text + "<gray>)";
        }
        return "";
    }

    public Component getSimpleDominanceString(IClan clan, IClan enemyClan) {
        Optional<ClanEnemy> enemyOptional = clan.getEnemy(enemyClan);
        Optional<ClanEnemy> theirEnemyOptional = enemyClan.getEnemy(clan);
        if (enemyOptional.isPresent() && theirEnemyOptional.isPresent()) {

            ClanEnemy enemy = enemyOptional.get();
            ClanEnemy theirEnemy = theirEnemyOptional.get();


            if (theirEnemy.getDominance() == 0 && enemy.getDominance() == 0) {
                return Component.text(" 0", NamedTextColor.WHITE);
            }
            if (theirEnemy.getDominance() > 0) {
                boolean nextKillDoms = theirEnemy.getDominance() + getDominanceForKill(clan.getSquadCount(), enemyClan.getSquadCount()) >= 100;
                return Component.text(" +" + theirEnemy.getDominance() + "%", nextKillDoms ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.GREEN);
            } else {
                boolean nextKillDoms = enemy.getDominance() + getDominanceForKill(enemyClan.getSquadCount(), clan.getSquadCount()) >= 100;
                return Component.text(" -" + enemy.getDominance() + "%", nextKillDoms ? NamedTextColor.LIGHT_PURPLE : NamedTextColor.DARK_RED);
            }

        }
        return Component.empty();
    }

    /**
     * Save insurance data for a particular block
     *
     * @param clan          The clan to save the insurance for
     * @param block         The block to be insured
     * @param insuranceType The insurance type (BREAK / PLACE)
     */
    public void addInsurance(Clan clan, Block block, InsuranceType insuranceType) {
        Insurance insurance = new Insurance(System.currentTimeMillis(), block.getType(), block.getBlockData().getAsString(),
                insuranceType, block.getLocation());

        repository.saveInsurance(clan, insurance);
        clan.getInsurance().add(insurance);
    }

    public void startInsuranceRollback(Clan clan) {
        List<Insurance> insuranceList = clan.getInsurance();
        insuranceList.sort(Collections.reverseOrder());
        getInsuranceQueue().addAll(insuranceList);
        getRepository().deleteInsuranceForClan(clan);
        clan.getInsurance().clear();
    }

    @Override
    public void loadFromList(List<Clan> objects) {
        // Load the base clan objects first so they can be referenced in the loop below
        objects.forEach(clan -> addObject(clan.getId().toString(), clan));

        objects.forEach(clan -> {
            clan.setTerritory(repository.getTerritory(clan));
            clan.setAlliances(repository.getAlliances(this, clan));
            clan.setEnemies(repository.getEnemies(this, clan));
            clan.setMembers(repository.getMembers(clan));
            clan.setInsurance(repository.getInsurance(clan));
        });

        log.info("Loaded {} clans", objects.size()).submit();
        leaderboardManager.getObject("Clans").ifPresent(Leaderboard::forceUpdate);
    }

    public boolean isInSafeZone(Player player) {
        Optional<Clan> clanOptional = getClanByLocation(player.getLocation());
        if (clanOptional.isPresent()) {
            Clan clan = clanOptional.get();
            return clan.isSafe();
        }
        return false;
    }

    public boolean isFields(Location location) {
        Optional<Clan> clan = getClanByLocation(location);
        return clan.filter(this::isFields).isPresent();
    }

    public boolean isFields(Clan clan) {
        return (clan.getName().equalsIgnoreCase("Fields"));
    }

    public boolean isLake(Location location) {
        Optional<Clan> clan = getClanByLocation(location);
        return clan.filter(this::isLake).isPresent();
    }

    public boolean isLake(Clan clan) {
        return (clan.getName().equalsIgnoreCase("Lake"));
    }

    public ClanLeaderboard getLeaderboard() {
        Optional<Leaderboard<?, ?>> clans = leaderboardManager.getObject("Clans");
        return (ClanLeaderboard) clans.orElse(null);
    }

    /**
     * Verifies that the origin {@link Clan} can neutral the target {@link Clan}
     * by throwing a {@link CommandSyntaxException} if it cannot
     * @param origin the {@link Clan} looking to neutral the target
     * @param target the {@link Clan} to be neutral with the origin
     * @throws CommandSyntaxException if this {@link Clan} cannot neutral the target {@link Clan}
     */
    public void canNeutralThrow(Clan origin, Clan target) throws CommandSyntaxException {
        if (origin.equals(target)) {
            throw ClanArgumentException.CLAN_MUST_NOT_BE_SAME.create(origin, target);
        }

        if (!(origin.isAllied(target) || !origin.isEnemy(target))) {
            throw ClanArgumentException.CLAN_NOT_ALLY_OR_ENEMY_OF_CLAN.create(origin, target);
        }

        if (origin.isEnemy(target) && inviteHandler.isInvited(target, origin, "Neutral")) {
            throw ArgumentException.TARGET_ALREADY_INVITED_BY_ORIGIN_TYPE.create(origin.getName(), target.getName(), "Ally");
        }

    }

    /**
     * Verifies that the origin {@link Clan} can ally the target {@link Clan}
     * by throwing a {@link CommandSyntaxException} if it cannot
     * @param origin the {@link Clan} looking to ally the target
     * @param target the {@link Clan} to be allied with the origin
     * @throws CommandSyntaxException if this {@link Clan} cannot ally the target {@link Clan}
     */
    public void canAllyThrow(Clan origin, Clan target) throws CommandSyntaxException {
        if (origin.equals(target)) {
            throw ClanArgumentException.CLAN_MUST_NOT_BE_SAME.create(origin, target);
        }

        if (origin.isAllied(target) || origin.isEnemy(target)) {
            throw ClanArgumentException.CLAN_NOT_NEUTRAL_OF_CLAN.create(origin, target);
        }

        if (origin.getSquadCount() >= maxClanMembers) {
            throw ClanArgumentException.CLAN_AT_MAX_SQUAD_COUNT_ALLY.create(origin.getName(), maxClanMembers);
        }

        int originClanSize = origin.getMembers().size();
        int potentialTargetSquadCount = originClanSize + target.getSquadCount();
        if (potentialTargetSquadCount > maxClanMembers) {
            throw ClanArgumentException.CLAN_OVER_MAX_SQUAD_COUNT_ALLY.create(target.getName(), potentialTargetSquadCount);
        }
        int targetClanSize = target.getMembers().size();
        int potentialOriginSquadCount = targetClanSize + origin.getSquadCount();
        if (potentialOriginSquadCount > maxClanMembers) {
            throw ClanArgumentException.CLAN_OVER_MAX_SQUAD_COUNT_ALLY.create(origin.getName(), potentialOriginSquadCount);
        }

        if (inviteHandler.isInvited(target, origin, "Ally")) {
            throw ArgumentException.TARGET_ALREADY_INVITED_BY_ORIGIN_TYPE.create(origin.getName(), target.getName(), "Ally");
        }

    }


    /**
     * Verifies that the origin {@link Clan} can trust the target {@link Clan}
     * by throwing a {@link CommandSyntaxException} if it cannot
     * @param origin the {@link Clan} looking to trust the target
     * @param target the {@link Clan} to be trusted with the origin
     * @throws CommandSyntaxException if this {@link Clan} cannot trust the target {@link Clan}
     */
    public void canTrustThrow(Clan origin, Clan target) throws CommandSyntaxException {
        if (origin.equals(target)) {
            throw ClanArgumentException.CLAN_MUST_NOT_BE_SAME.create(origin, target);
        }

        if (!origin.isAllied(target) || origin.isEnemy(target)) {
            throw ClanArgumentException.CLAN_NOT_ALLY_OF_CLAN.create(origin, target);
        }

        ClanAlliance targetAlly = origin.getAlliance(target).orElseThrow(() -> ClanArgumentException.CLAN_NOT_ALLY_OF_CLAN.create(origin, target));

        if (targetAlly.isTrusted()) {
            throw ClanArgumentException.CLAN_ALREADY_TRUSTS_CLAN.create(origin, target);
        }

        if (inviteHandler.isInvited(target, origin, "Trust")) {
            throw ArgumentException.TARGET_ALREADY_INVITED_BY_ORIGIN_TYPE.create(origin.getName(), target.getName(), "Trust");
        }

    }

    /**
     * Verifies that the origin {@link ClanMember} can promot the target {@link ClanMember}
     * by throwing a {@link CommandSyntaxException} if it cannot
     * @param origin the {@link ClanMember} looking to promote the target
     * @param target the {@link ClanMember} to be promoted
     * @throws CommandSyntaxException if this {@link ClanMember} cannot promote the target {@link ClanMember}
     */
    public void targetIsLowerRankThrow(ClanMember origin, ClanMember target) throws CommandSyntaxException {
        if (origin.getRank().getPrivilege() < target.getRank().getPrivilege()) {
            throw ClanArgumentException.MEMBER_CANNOT_ACTION_MEMBER_RANK.create(target.getClientName());
        }

        if (origin.equals(target)) {
            throw ClanArgumentException.MEMBER_CLAN_CANNOT_ACTION_SELF.create();
        }
    }

    /**
     * Verifies that the origin {@link Client} can invite the target {@link Player}
     * by throwing a {@link CommandSyntaxException} if they cannot
     * @param origin the {@link Client} looking to invite the target
     * @param target the {@link Client} to be invited
     * @throws CommandSyntaxException if this {@link Client} can invite the target {@link Client}
     */
    public void canInviteToClan(Client origin, Client target) throws CommandSyntaxException {

        final Clan originClan = getClanByClient(origin).orElseThrow(ClanArgumentException.MUST_BE_IN_A_CLAN_EXCEPTION::create);

        final Optional<Clan> targetClan = getClanByClient(target);
        if (targetClan.isPresent()) {
            throw ClanArgumentException.MUST_NOT_BE_IN_A_CLAN_EXCEPTION.create(target.getName());
        }

        final Gamer targetGamer = target.getGamer();

        if (inviteHandler.isInvited(targetGamer, originClan, "Invite")) {
            throw ArgumentException.TARGET_ALREADY_INVITED_BY_ORIGIN_TYPE.create(originClan.getName(), target.getName(), "Clan");
        }

        if (originClan.getSquadCount() + 1 > maxClanMembers) {
            throw ClanArgumentException.CLAN_AT_MAX_SQUAD_COUNT_INVITE.create(originClan.getName(), target.getName(), maxClanMembers);
        }

        for (final ClanAlliance alliance : originClan.getAlliances()) {
            final IClan allyClan = alliance.getClan();
            if (allyClan.getSquadCount() + 1 > maxClanMembers) {
                throw ClanArgumentException.ALLY_AT_MAX_SQUAD_COUNT_INVITE.create(allyClan.getName(), target.getName(), maxClanMembers);
            }
        }

    }

    /**
     * Verifies that the joiner {@link Client} can join the target {@link Clan}
     * by throwing a {@link CommandSyntaxException} if they cannot
     * @param joiner the {@link Client} looking to join the target
     * @param target the {@link Clan} to join
     * @throws CommandSyntaxException if this {@link Client} can join the target {@link Clan}
     */
    public void canJoinClan(final Client joiner, final Clan target) throws CommandSyntaxException {


        final Gamer joinerGamer = joiner.getGamer();

        if (!inviteHandler.isInvited(joinerGamer, target, "Invite")) {
            throw ArgumentException.TARGET_NOT_INVITED_BY_ORIGIN_TYPE.create(target.getName(), joiner.getName(), "Clan");
        }

        if (target.getSquadCount() + 1 > maxClanMembers) {
            throw ClanArgumentException.CLAN_AT_MAX_SQUAD_COUNT_JOIN.create(target.getName(),  maxClanMembers);
        }

        for (final ClanAlliance alliance : target.getAlliances()) {
            final IClan allyClan = alliance.getClan();
            if (allyClan.getSquadCount() + 1 > maxClanMembers) {
                throw ClanArgumentException.ALLY_AT_MAX_SQUAD_COUNT_JOIN.create(target.getName(), allyClan.getName(), maxClanMembers);
            }
        }

    }

    /**
     * Verifies that the origin {@link Clan} can enemy the target {@link Clan}
     * by throwing a {@link CommandSyntaxException} if it cannot
     * @param origin the {@link Clan} looking to enemy the target
     * @param target the {@link Clan} to be enemied with the origin
     * @throws CommandSyntaxException if this {@link Clan} cannot enemy the target {@link Clan}
     */
    public void canEnemyThrow(@NotNull Clan origin, @NotNull Clan target) throws CommandSyntaxException {
        if (origin.equals(target)) {
            throw ClanArgumentException.CLAN_MUST_NOT_BE_SAME.create(origin, target);
        }

        if (origin.isAllied(target) || origin.isEnemy(target)) {
            throw ClanArgumentException.CLAN_NOT_NEUTRAL_OF_CLAN.create(origin, target);
        }

        if (getPillageHandler().isPillaging(origin, target)
                || getPillageHandler().isPillaging(target, origin)) {
            throw ClanArgumentException.CLAN_CANNOT_ACTION_CLAN_WHILE_PILLAGING.create(origin, target);
        }

    }

    /**
     * Checks if the origin {@link Player} can claim the {@link Chunk}
     * by throwing a {@link CommandSyntaxException} if it cannot
     * @param origin the origin {@link Player}
     * @param originClan the {@link Clan} of the origin
     * @param chunk the {@link Chunk} to claim
     * @throws CommandSyntaxException if this {@link Chunk} is invalid for the {@link Player origin} to claim
     */
    public void canClaimThrow(@NotNull final Player origin, @NotNull final Clan originClan, @NotNull final Chunk chunk) throws CommandSyntaxException {
        if (chunk.getWorld().getName().equalsIgnoreCase(BPvPWorld.BOSS_WORLD_NAME)
                && !chunk.getWorld().getName().equalsIgnoreCase(BPvPWorld.MAIN_WORLD_NAME)) {
            throw ClanArgumentException.CANNOT_CLAIM_WORLD.create();
        }

        if (originClan.getTerritory().size() > getMaximumClaimsForClan(originClan)) {
            throw ClanArgumentException.CANNOT_CLAIM_MORE_TERRITORY.create();
        }

        final Optional<Clan> locationClanOptional = getClanByChunk(chunk);
        if (locationClanOptional.isPresent()) {
            final Clan locationClan = locationClanOptional.get();
            throw ClanArgumentException.CLAN_ALREADY_CLAIMS_TERRITORY.create(locationClan);
        }

        if (adjacentOtherClans(chunk, originClan)) {
            throw ClanArgumentException.CANNOT_CLAIM_ADJACENT_TO_OTHER_TERRITORY.create();
        }

        if (!originClan.getTerritory().isEmpty() && !adjacentToOwnClan(chunk, originClan)) {
            throw ClanArgumentException.MUST_CLAIM_TERRITORY_ADJACENT_TO_OWN_TERRITORY.create();
        }

        long claimCooldown = getRemainingClaimCooldown(chunk);
        if (claimCooldown > 0) {
            throw ClanArgumentException.TERRITORY_ON_CLAIM_COOLDOWN.create(claimCooldown);
        }

        for (Entity entity : chunk.getEntities()) {
            if (entity instanceof final Player target) {
                if (target.equals(origin)) {
                    continue;
                }
                if (canHurt(origin, target)) {
                    Optional<Clan> targetClanOptional = getClanByPlayer(target);
                    if (targetClanOptional.isEmpty()) continue;
                    final Clan targetClan = targetClanOptional.get();
                    if (!originClan.isAllied(targetClan)) {
                        throw ClanArgumentException.CANNOT_CLAIM_WITH_ENEMIES.create();
                    }
                }

            }
        }
    }

    /**
     * Checks if the origin {@link Player} can unclaim the {@link Player#getChunk() chunk}
     * by throwing a {@link CommandSyntaxException} if it cannot
     * @param origin the origin {@link Player}
     * @param originClan the {@link Clan} of the origin
     * @throws CommandSyntaxException if this {@link Player#getChunk() chunk} is invalid for the {@link Player origin} to unclaim
     */
    public void canUnclaimOwnThrow(@NotNull final Player origin, @NotNull final Clan originClan) throws CommandSyntaxException {
        if (originClan.getTerritory().size() > 2 && UtilClans.isClaimRequired(UtilClans.getClaimLayout(origin, originClan))) {
            throw ClanArgumentException.CLAN_UNCLAIM_SPLIT_TERRITORY.create(originClan);
        }
    }

    /**
     * Checks if the origin {@link Player} can unclaim the {@link Player#getChunk() chunk} of another {@link Clan}
     * by throwing a {@link CommandSyntaxException} if it cannot
     * @param origin the origin {@link Player}
     * @param locationClan the {@link Clan} the origin is trying to unclaim
     * @throws CommandSyntaxException if this {@link Player#getChunk() chunk} is invalid for the {@link Player origin} to unclaim
     */
    public void canUnclaimOtherThrow(@NotNull final Player origin, @NotNull final Clan locationClan) throws CommandSyntaxException {
        if (locationClan.isAdmin()) {
            throw ClanArgumentException.CANNOT_UNCLAIM_FROM_CLAN.create();
        }

        if (locationClan.getTerritory().size() <= getMaximumClaimsForClan(locationClan)) {
            throw ClanArgumentException.CLAN_ABLE_TO_RETAIN_TERRITORY.create(locationClan);
        }

        if (UtilClans.isClaimRequired(UtilClans.getClaimLayout(origin, locationClan))){
            throw ClanArgumentException.CLAN_UNCLAIM_SPLIT_TERRITORY.create(locationClan);
        }

        for (Player clanMember : locationClan.getMembersAsPlayers()) {
            final Client memberClient = clientManager.search().online(clanMember);
            if (memberClient.isAdministrating()) {
                clientManager.sendMessageToRank("Clans",
                        UtilMessage.deserialize("<yellow>%s<gray> prevented <yellow>%s<gray> from unclaiming <yellow>%s<gray>'s territory because they are in adminstrator mode",
                                memberClient.getName(), origin.getName(), locationClan.getName()), Rank.HELPER);
                throw ClanArgumentException.CANNOT_UNCLAIM_FROM_CLAN.create();
            }
        }
    }

}
