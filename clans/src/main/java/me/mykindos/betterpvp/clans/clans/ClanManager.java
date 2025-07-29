package me.mykindos.betterpvp.clans.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
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
import me.mykindos.betterpvp.clans.utilities.ClansNamespacedKeys;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.stats.Leaderboard;
import me.mykindos.betterpvp.core.stats.repository.LeaderboardManager;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.utilities.model.data.CustomDataType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.data.Bisected;
import org.bukkit.block.data.type.Door;
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
    public ClanManager(Clans clans, ClanRepository repository, ClientManager clientManager, PillageHandler pillageHandler, LeaderboardManager leaderboardManager) {
        this.clans = clans;
        this.repository = repository;
        this.clientManager = clientManager;
        this.pillageHandler = pillageHandler;
        this.leaderboardManager = leaderboardManager;
        this.dominanceScale = new HashMap<>();
        this.insuranceQueue = new ConcurrentLinkedQueue<>();

        dominanceScale = repository.getDominanceScale();

        ClanPerkManager.getInstance().init();
    }

    /**
     * Updates the name of a given clan in the repository.
     *
     * @param clan the clan whose name is to be updated
     */
    public void updateClanName(Clan clan) {
        getRepository().updateClanName(clan);
    }

    /**
     * Retrieves a clan based on its unique identifier.
     * If the provided ID is null, an empty {@code Optional} is returned.
     *
     * @param id the unique identifier of the clan, or null if no ID is specified
     * @return an {@code Optional} containing the clan if found, or an empty {@code Optional} if no clan exists for the given ID or if the ID is null
     */
    public Optional<Clan> getClanById(@Nullable UUID id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(objects.get(id.toString()));
    }

    /**
     * Retrieves the clan associated with the specified client.
     *
     * @param client the client whose clan is to be retrieved
     * @return an Optional containing the clan associated with the client, or an empty Optional if no clan is found
     */
    public Optional<Clan> getClanByClient(Client client) {
        return getClanByPlayer(client.getUniqueId());
    }

    /**
     * Retrieves the clan that the given player is a member of by searching through the available clans.
     * This method performs an expensive operation by iterating over all clans to find a match.
     *
     * @param player the player whose clan membership is being searched
     * @return an {@code Optional} containing the clan the player belongs to, or an empty {@code Optional} if the player isn't in any clan
     */
    public Optional<Clan> expensiveGetClanByPlayer(Player player) {
        return objects.values().stream()
                .filter(clan -> clan.getMemberByUUID(player.getUniqueId()).isPresent()).findFirst();

    }

    /**
     * Retrieves the clan associated with the given player, if any.
     *
     * @param player the player for whom to retrieve the associated clan; must not be null
     * @return an Optional containing the clan if the player is part of one, or an empty Optional if not
     */
    public Optional<Clan> getClanByPlayer(Player player) {

        if (player != null && player.hasMetadata("clan")) {
            List<MetadataValue> clan = player.getMetadata("clan");
            if (!clan.isEmpty()) {
                return Optional.ofNullable(clan.get(0).value())
                        .map(UUID.class::cast)
                        .flatMap(this::getClanById);
            }
        }


        return Optional.empty();
    }

    /**
     * Retrieves the clan associated with a player identified by their unique UUID.
     * If the player is online, their clan is determined through their player instance.
     * If the player is offline, the clan is identified by searching through the stored clans.
     *
     * @param uuid the unique identifier of the player whose clan is to be retrieved
     * @return an {@code Optional} containing the player's clan if found,
     *         or an empty {@code Optional} if no associated clan exists
     */
    public Optional<Clan> getClanByPlayer(UUID uuid) {
        final Player player = Bukkit.getPlayer(uuid);
        if (player == null) {
            return objects.values().stream()
                    .filter(clan -> clan.getMemberByUUID(uuid).isPresent()).findFirst();
        }

        return getClanByPlayer(player);
    }

    /**
     * Retrieves a clan with the specified name.
     *
     * @param name the name of the clan to search for; this parameter is case-insensitive
     * @return an {@code Optional} containing the {@code Clan} if one exists with the specified name,
     *         or an empty {@code Optional} if no such clan is found
     */
    public Optional<Clan> getClanByName(String name) {
        return objects.values().stream().filter(clan -> clan.getName().equalsIgnoreCase(name)).findFirst();
    }

    /**
     * Retrieves a clan associated with the specified location.
     *
     * @param location the location to check for an associated clan
     * @return an {@link Optional} containing the clan if one exists at the given location, or an empty {@link Optional} if no clan is found
     */
    public Optional<Clan> getClanByLocation(Location location) {
        return getClanByChunk(location.getChunk());
    }

    /**
     * Retrieves the clan that owns the specified chunk, if any.
     *
     * @param chunk the chunk to check for clan ownership
     * @return an {@link Optional} containing the clan that owns the chunk, or an empty {@link Optional}
     *         if the chunk is not owned by any clan
     */
    public Optional<Clan> getClanByChunk(Chunk chunk) {
        final UUID uuid = chunk.getPersistentDataContainer().get(ClansNamespacedKeys.CLAN, CustomDataType.UUID);
        if (uuid == null) {
            return Optional.empty();
        }

        return getClanById(uuid);
    }

    /**
     * Checks if the specified chunk is adjacent to any chunk claimed by other clans.
     *
     * @param chunk the chunk to check for adjacency
     * @param clan the clan that owns the specified chunk
     * @return true if the specified chunk is adjacent to a chunk claimed by other clans, false otherwise
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
     * Checks if the specified chunk is adjacent to any chunk belonging to the given clan.
     *
     * @param chunk the chunk to check
     * @param clan the clan whose territory to compare against
     * @return true if the given chunk is adjacent to a chunk owned by the specified clan, false otherwise
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

    /**
     * Applies a cooldown period to a disbanded claim in the specified clan territory.
     *
     * @param clanTerritory the territory associated with the clan where the disband claim cooldown is to be applied
     */
    public void applyDisbandClaimCooldown(ClanTerritory clanTerritory) {
        setClaimCooldown(clanTerritory.getWorldChunk(), (long) (claimDisbandCooldown * 1000L));
    }

    /**
     * Sets a claim cooldown on the specified chunk by storing the cooldown expiration timestamp
     * in the chunk's persistent data container.
     *
     * @param chunk the chunk for which the claim cooldown is being set
     * @param duration the cooldown duration in milliseconds to be added to the current time
     */
    public void setClaimCooldown(Chunk chunk, long duration) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        pdc.set(ClansNamespacedKeys.CLAIM_COOLDOWN, PersistentDataType.LONG, System.currentTimeMillis() + duration);
    }

    /**
     * Retrieves the remaining cooldown time for claiming a specific chunk.
     *
     * @param chunk The chunk for which the claim cooldown is being checked.
     * @return The remaining claim cooldown in milliseconds. If the cooldown has expired, 0 is returned.
     */
    public long getRemainingClaimCooldown(Chunk chunk) {
        PersistentDataContainer pdc = chunk.getPersistentDataContainer();
        final long endTime = pdc.getOrDefault(ClansNamespacedKeys.CLAIM_COOLDOWN, PersistentDataType.LONG, 0L);
        if (endTime < System.currentTimeMillis()) {
            return 0;
        }
        return endTime - System.currentTimeMillis();
    }

    /**
     * Retrieves an optional Clan by matching the given chunk string to a chunk in the clan's territory.
     *
     * @param serialized the serialized representation of a chunk to match against clan territories
     * @return an {@code Optional<Clan>} containing the clan whose territory matches the given chunk string,
     *         or an empty {@code Optional} if no match is found
     */
    public Optional<Clan> getClanByChunkString(String serialized) {
        return objects.values().stream()
                .filter(clan -> clan.getTerritory().stream()
                        .anyMatch(territory -> territory.getChunk().equalsIgnoreCase(serialized))).findFirst();
    }

    /**
     * Checks whether the target player belongs to the same clan as the specified player.
     *
     * @param player the player whose clan membership is to be checked.
     * @param target the target player to verify clan membership against.
     * @return true if both players belong to the same clan; false otherwise.
     */
    public boolean isClanMember(Player player, Player target) {
        Optional<Clan> aClanOptional = getClanByPlayer(player);
        Optional<Clan> bClanOptional = getClanByPlayer(target);

        if (aClanOptional.isEmpty() || bClanOptional.isEmpty()) return false;

        return aClanOptional.equals(bClanOptional);

    }

    /**
     * Determines the relationship between two clans based on their current status
     * and interactions (e.g., allies, enemies, pillaging).
     *
     * @param clanA The first clan to evaluate. Can be null.
     * @param clanB The second clan to evaluate. Can be null.
     * @return The relationship between the two clans as a {@link ClanRelation}.
     *         If either clan is null, {@code ClanRelation.NEUTRAL} is returned.
     *         If both clans are the same, {@code ClanRelation.SELF} is returned. Other
     *         specific relationships (e.g., ally, enemy, pillage) are determined
     *         based on their interactions.
     */
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

    /**
     * Determines if a specific player has access to a designated location based on various checks,
     * such as clan affiliations, administrative privileges, and clan relations.
     *
     * @param player   The player whose access is being checked.
     * @param location The location where access is being evaluated.
     * @return true if the player has access to the location; false otherwise.
     */
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

    /**
     * Finds the closest wilderness location to the specified player by scanning nearby chunks
     * and identifying those that are not claimed by any clan. Attempts to return the closest
     * safe block location in one of these wilderness chunks.
     *
     * @param player The player whose closest wilderness location is to be determined.
     * @return The location of the closest wilderness area, adjusted to be above ground, or null
     * if no wilderness area is available within the scanned radius.
     */
    @NotNull
    public Optional<Location> closestWilderness(Player player) {
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

        while (!chunks.isEmpty()) {
            Chunk chunk = UtilWorld.closestChunkToPlayer(chunks, player);

            //this should not ever happen
            if (chunk == null) continue;

            List<Location> locations = new ArrayList<>();

            int y = (int) player.getY();
            for (int i = 0; i < 16; i++) {
                for (int j = 0; j < 16; j++) {
                    locations.add(chunk.getBlock(i, y, j).getLocation().toHighestLocation());
                }
            }

            Optional<Location> locationOptional = locations.stream()
                    .filter(location -> location.getWorld().getWorldBorder().isInside(location))
                    .min(Comparator.comparingInt(a -> (int) player.getLocation().distanceSquared(a)));

            if (locationOptional.isEmpty()) {
                //try a new chunk if all locations are out of the border
                chunks.remove(chunk);
                continue;
            }

            //to prevent getting stuck in a block, add 1 to Y
            return Optional.of(locationOptional.get().add(0.5, 1, 0.5));
        }
        return Optional.empty();
    }


    /**
     * Finds the closest wilderness location moving backwards from the player's current direction.
     * This method checks backwards along the player's current direction up to a maximum of 64 steps
     * to locate a wilderness area that is not part of any clan territory.
     *
     * @param player the player whose location and direction will be used to find wilderness
     * @return the closest wilderness location backwards from the player's current location,
     *         or null if no wilderness location is found within the checked range
     */
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

    /**
     * Calculates the maximum number of claims that a given clan can have.
     * The calculation is based on a base claim amount, a bonus claim amount per member,
     * and an upper limit for the total claims allowed.
     *
     * @param clan the clan for which to calculate the maximum number of claims
     * @return the maximum number of claims the specified clan can have
     */
    public int getMaximumClaimsForClan(Clan clan) {
        return Math.min(maxAmountOfClaims, baseAmountOfClaims + (clan.getMembers().size() * bonusClaimsPerMember));
    }

    /**
     * Generates a detailed tooltip about a specific clan, including information such as
     * the clan's name, age, territory size, alliances, and members.
     *
     * @param player The player requesting the tooltip.
     * @param target The target clan for which the tooltip is being generated.
     * @return A {@code Component} containing the formatted clan tooltip with detailed information.
     */
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

    /**
     * Retrieves a formatted list of alliance names for the specified clan.
     * The list includes the names of allied clans along with their primary mini color.
     *
     * @param player The player whose clan context is used to determine relationships.
     * @param clan   The clan for which the alliance list is being generated.
     * @return A formatted string containing the names of allied clans. If there are no alliances, an empty string is returned.
     */
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

    /**
     * Retrieves and formats a list of enemy clans for the specified player's clan.
     *
     * @param player the player for whom the enemy list is being generated
     * @param clan the clan whose enemies are being listed
     * @return a formatted string containing the names of enemy clans, separated by commas
     */
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

    /**
     * Generates a formatted string representation of the enemies of a specified clan, including their dominance-related information.
     *
     * @param player the player for whom the enemy list is being generated; used to determine the player's clan and its relation to enemy clans.
     * @param clan the clan whose enemies are to be listed.
     * @return a formatted string of the enemy list, including each enemy's display color and dominance information.
     */
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

    /**
     * Generates a formatted string containing the list of members in a given clan.
     * Each member string includes their role icon, online status, and a formatted display name.
     *
     * @param clan the clan whose members are to be listed
     * @return a formatted string representing the list of clan members, or an empty string if there are no members
     */
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

    /**
     * Determines if the specified player is eligible to teleport based on their
     * current status in the game.
     *
     * @param player the player whose teleportation eligibility is being checked
     * @return true if the player can teleport, false otherwise
     */
    public boolean canTeleport(Player player) {
        Gamer gamer = clientManager.search().online(player).getGamer();
        return !gamer.isInCombat();
    }

    /**
     * Determines whether the target player is an ally of the given player.
     *
     * @param player the player to check alliances for
     * @param target the target player to check if they are an ally
     * @return true if the target player is an ally of the given player; false otherwise
     */
    public boolean isAlly(Player player, Player target) {
        return player.equals(target) || getAllies(player).stream().anyMatch(o -> Objects.equals(o.getUuid(), target.getUniqueId().toString()));
    }

    /**
     * Retrieves a list of allies for the player. This includes members of the player's clan
     * and members of all allied clans.
     *
     * @param player the player whose allies are to be retrieved
     * @return a list of allies, including members of the player's clan and allied clans.
     *         If the player does not belong to any clan, an empty list is returned.
     */
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

    /**
     * Determines if the specified player can inflict damage on the target player
     * based on their clans, relations, and location-specific rules.
     *
     * @param player the player attempting to inflict damage
     * @param target the intended target player
     * @return true if the player can hurt the target under the given conditions,
     *         false otherwise
     */
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

    /**
     * Checks if the specified player can cast abilities or perform certain actions
     * based on their current location, clan affiliation, and combat status.
     *
     * @param player the player whose ability to cast is being checked
     * @return {@code true} if the player is allowed to cast abilities,
     *          {@code false} otherwise
     */
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

    /**
     * Calculates the dominance gain for a kill operation based on the sizes of the killed and killer squads.
     * The calculation may depend on a fixed dominance gain or a dynamic scaling mechanism using a dominance scale.
     *
     * @param killedSquadSize the number of members in the squad that was killed
     * @param killerSquadSize the number of members in the squad that performed the kill
     * @return the dominance gain resulting from the kill operation
     */
    public double getDominanceForKill(int killedSquadSize, int killerSquadSize) {
        if (fixedDominanceGain) {
            return dominanceGain;
        }

        int sizeOffset = Math.min(maxClanMembers, maxClanMembers - Math.min(killerSquadSize - killedSquadSize, maxClanMembers));
        return dominanceScale.getOrDefault(sizeOffset, 6D);
    }

    /**
     * Applies dominance changes between two clans based on a kill event.
     *
     * This method determines the dominance impact when one clan (killed) is defeated by another clan (killer).
     * It checks multiple conditions, such as whether both clans are valid, are enemies, and whether dominance settings allow for an increase.
     * Dominance is then adjusted accordingly for both clans, triggering events and updating the repository as necessary.
     *
     * @param killed the clan that was defeated in the kill event; must not be null.
     * @param killer the clan that initiated the kill and gains dominance; must not be null.
     */
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

    /**
     * Calculates and retrieves a string representation of the dominance relationship
     * between two clans. The returned string includes the dominance value and indicates
     * any changes that may occur with the next kill, based on dominance thresholds.
     *
     * @param clan the clan for which the dominance string is being calculated
     * @param enemyClan the enemy clan to compare dominance against
     * @return a string representation of the dominance status between the two clans,
     *         or an empty string if no significant dominance relationship is found
     */
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

    /**
     * Generates a simple dominance string representation between two clans based on their dominance values.
     *
     * @param clan the clan for which the dominance interaction is being calculated
     * @param enemyClan the enemy clan against which the dominance interaction is being calculated
     * @return a {@link Component} representing the dominance status, which may include a dominance percentage
     *         with associated color codes indicating the state, or an empty component if dominance is not present
     */
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
     * Adds insurance for a clan based on the block and insurance type provided.
     *
     * @param clan the clan for which insurance is being added
     * @param block the block related to the insurance
     * @param insuranceType the type of insurance being applied
     */
    public void addInsurance(Clan clan, Block block, InsuranceType insuranceType) {


        Block targetBlock = block;

        if(targetBlock.getBlockData() instanceof Door door) {
            if(door.getHalf() == Bisected.Half.TOP) {
                targetBlock = block.getRelative(0, -1, 0);
            }
        }

        Insurance insurance = new Insurance(System.currentTimeMillis(), targetBlock.getType(), targetBlock.getBlockData().getAsString(),
                insuranceType, targetBlock.getLocation());
        repository.saveInsurance(clan, insurance);
        clan.getInsurance().add(insurance);
    }

    /**
     * Initiates the rollback process for all insurance policies associated with the specified clan.
     * The process involves reversing the order of the insurance policies, adding them to a rollback queue,
     * removing all insurances from the persistent storage, and clearing the clan's current insurance list.
     *
     * @param clan the clan whose insurance policies will be rolled back
     */
    public void startInsuranceRollback(Clan clan) {
        List<Insurance> insuranceList = clan.getInsurance();
        insuranceList.sort(Collections.reverseOrder());
        getInsuranceQueue().addAll(insuranceList);
        getRepository().deleteInsuranceForClan(clan);
        clan.getInsurance().clear();
    }

    /**
     * Loads a list of Clan objects into the manager, initializing their properties
     * and updating any related data such as territories, alliances, enemies, and members.
     * Also triggers related updates such as leaderboard changes.
     *
     * @param objects a list of Clan objects to be loaded and initialized
     */
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

    /**
     * Determines if the specified player is within a safe zone.
     *
     * @param player the player whose location is to be checked
     * @return true if the player is in a safe zone, false otherwise
     */
    public boolean isInSafeZone(Player player) {
        Optional<Clan> clanOptional = getClanByLocation(player.getLocation());
        if (clanOptional.isPresent()) {
            Clan clan = clanOptional.get();
            return clan.isSafe();
        }
        return false;
    }

    /**
     * Determines if the specified location belongs to a clan with the "Fields" attribute.
     *
     * @param location the location to check
     * @return true if the location belongs to a clan with the "Fields" attribute, false otherwise
     */
    public boolean isFields(Location location) {
        Optional<Clan> clan = getClanByLocation(location);
        return clan.filter(this::isFields).isPresent();
    }

    /**
     * Checks if the given clan is named "Fields" (case insensitive).
     *
     * @param clan the clan to check
     * @return true if the clan's name is "Fields" (case insensitive), false otherwise
     */
    public boolean isFields(Clan clan) {
        return (clan.getName().equalsIgnoreCase("Fields"));
    }

    /**
     * Determines if the specified location is part of a lake owned by a clan.
     *
     * @param location the location to check.
     * @return true if the location is part of a lake owned by a clan, false otherwise.
     */
    public boolean isLake(Location location) {
        Optional<Clan> clan = getClanByLocation(location);
        return clan.filter(this::isLake).isPresent();
    }

    /**
     * Checks whether the given clan's name is "Lake", ignoring case.
     *
     * @param clan The clan to check.
     * @return true if the clan's name is "Lake", ignoring case; false otherwise.
     */
    public boolean isLake(Clan clan) {
        return (clan.getName().equalsIgnoreCase("Lake"));
    }

    /**
     * Retrieves the clan leaderboard from the leaderboard manager.
     *
     * @return the {@code ClanLeaderboard} instance if present; {@code null} otherwise.
     */
    public ClanLeaderboard getLeaderboard() {
        Optional<Leaderboard<?, ?>> clans = leaderboardManager.getObject("Clans");
        return (ClanLeaderboard) clans.orElse(null);
    }

}
