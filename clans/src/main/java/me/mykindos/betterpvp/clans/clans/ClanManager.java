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
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

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
    @Config(path = "clans.pillage.enabled", defaultValue = "true")
    private boolean pillageEnabled;

    @Inject
    @Config(path = "clans.dominance.enabled", defaultValue = "true")
    private boolean dominanceEnabled;

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

    public void updateClanName(Clan clan) {
        getRepository().updateClanName(clan);
    }

    public Optional<Clan> getClanById(UUID id) {
        return Optional.ofNullable(objects.get(id.toString()));
    }

    public Optional<Clan> getClanByClient(Client client) {
        return getClanByPlayer(client.getUniqueId());
    }

    public Optional<Clan> expensiveGetClanByPlayer(Player player) {
        return objects.values().stream()
                .filter(clan -> clan.getMemberByUUID(player.getUniqueId()).isPresent()).findFirst();

    }

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

    public ClanRelation getRelation(IClan clanA, IClan clanB) {
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

        return relation == ClanRelation.SELF || relation == ClanRelation.ALLY_TRUST;
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
            return locations.get(0).add(0, 1, 0);
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
                clientManager.search().offline(UUID.fromString(member.getUuid()), clientOpt -> clientOpt.ifPresent(client -> {
                    membersString.append(!membersString.isEmpty() ? "<gray>, " : "").append("<yellow>")
                            .append(member.getRoleIcon())
                            .append(UtilFormat.getOnlineStatus(member.getUuid()))
                            .append(UtilFormat.spoofNameForLunar(client.getName()));
                }));
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
        Clan targetLocationClan = getClanByLocation(target.getLocation()).orElse(null);
        if (targetLocationClan != null && targetLocationClan.isSafe()) {
            Gamer gamer = clientManager.search().online(target).getGamer();
            if (!gamer.isInCombat()) {
                return false;
            }
        }

        Clan playerClan = getClanByPlayer(player).orElse(null);
        Clan targetClan = getClanByPlayer(target).orElse(null);
        ClanRelation relation = getRelation(playerClan, targetClan);

        return relation != ClanRelation.SELF && relation != ClanRelation.ALLY && relation != ClanRelation.ALLY_TRUST;
    }

    public boolean canCast(Player player) {
        Optional<Clan> locationClanOptional = getClanByLocation(player.getLocation());
        if (locationClanOptional.isPresent()) {
            Clan locationClan = locationClanOptional.get();
            if (locationClan.isAdmin() && locationClan.isSafe()) {

                Gamer gamer = clientManager.search().online(player).getGamer();
                return gamer.isInCombat();
            }
        }

        return true;
    }

    public double getDominanceForKill(int killedSquadSize, int killerSquadSize) {

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

        killed.messageClan("You lost <red>" + dominance + "%<gray> dominance to <red>" + killer.getName() +  getDominanceString(killed, killer), null, true);
        killer.messageClan("You gained <green>" + dominance + "%<gray> dominance on <red>" + killed.getName() + getDominanceString(killer, killed) , null, true);

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

}
