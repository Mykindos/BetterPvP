package me.mykindos.betterpvp.clans.clans;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.clans.insurance.Insurance;
import me.mykindos.betterpvp.clans.clans.insurance.InsuranceType;
import me.mykindos.betterpvp.clans.clans.pillage.Pillage;
import me.mykindos.betterpvp.clans.clans.pillage.PillageHandler;
import me.mykindos.betterpvp.clans.clans.pillage.events.PillageStartEvent;
import me.mykindos.betterpvp.clans.clans.repository.ClanRepository;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.framework.events.scoreboard.ScoreboardUpdateEvent;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.gamer.Gamer;
import me.mykindos.betterpvp.core.gamer.GamerManager;
import me.mykindos.betterpvp.core.utilities.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minecraft.core.Direction;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

@Slf4j
@Singleton
public class ClanManager extends Manager<Clan> {

    @Getter
    private final ClanRepository repository;
    private final GamerManager gamerManager;
    @Getter
    private final PillageHandler pillageHandler;

    private Map<Integer, Integer> dominanceScale;

    @Getter
    private final ConcurrentLinkedQueue<Insurance> insuranceQueue;

    @Inject
    @Config(path = "clans.claims.additional", defaultValue = "3")
    private int additionalClaims;

    @Inject
    public ClanManager(ClanRepository repository, GamerManager gamerManager, PillageHandler pillageHandler) {
        this.repository = repository;
        this.gamerManager = gamerManager;
        this.pillageHandler = pillageHandler;
        this.dominanceScale = new HashMap<>();
        this.insuranceQueue = new ConcurrentLinkedQueue<>();

        dominanceScale = repository.getDominanceScale();

    }

    public Optional<Clan> getClanById(UUID id) {
        return objects.values().stream().filter(clan -> clan.getId().equals(id)).findFirst();
    }

    public Optional<Clan> getClanByClient(Client client) {
        return objects.values().stream()
                .filter(clan -> clan.getMemberByUUID(client.getUuid()).isPresent()).findFirst();
    }

    public Optional<Clan> getClanByPlayer(Player player) {
        return objects.values().stream()
                .filter(clan -> clan.getMemberByUUID(player.getUniqueId().toString()).isPresent()).findFirst();
    }

    public Optional<Clan> getClanByPlayer(UUID uuid) {
        return objects.values().stream()
                .filter(clan -> clan.getMemberByUUID(uuid).isPresent()).findFirst();
    }

    public Optional<Clan> getClanByName(String name) {
        return Optional.ofNullable(objects.get(name.toLowerCase()));
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
        return objects.values().stream()
                .filter(clan -> clan.getTerritory().stream()
                        .anyMatch(territory -> territory.getChunk().equalsIgnoreCase(UtilWorld.chunkToFile(chunk)))).findFirst();
    }

    public Optional<Clan> getClanByChunkString(String chunk) {
        return objects.values().stream()
                .filter(clan -> clan.getTerritory().stream()
                        .anyMatch(territory -> territory.getChunk().equalsIgnoreCase(chunk))).findFirst();
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

        Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId());
        if(gamerOptional.isPresent()) {
            Gamer gamer = gamerOptional.get();
            if(gamer.getClient().isAdministrating()) {
                return true;
            }
        }

        if (locationClanOptional.isEmpty()) return true;
        if (playerClanOptional.isEmpty()) return false;

        Clan playerClan = playerClanOptional.get();
        Clan locationClan = locationClanOptional.get();

        if (pillageHandler.isPillaging(playerClan, locationClan)) {
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

    public Component getClanTooltip(Player player, Clan target) {
        Clan clan = getClanByPlayer(player).orElse(null);
        var territoryString = target.getTerritory().size() + "/" + (target.getMembers().size() + additionalClaims);

        return Component.text(target.getName() + " Information").color(getRelation(clan, target).getPrimary()).append(Component.newline())
                .append(Component.text(" Age: ").color(NamedTextColor.WHITE).append(UtilMessage.getMiniMessage("<yellow>%s\n", target.getAge())))
                .append(Component.text(" Territory: ").color(NamedTextColor.WHITE).append(UtilMessage.getMiniMessage("<yellow>%s\n", territoryString)))
                .append(Component.text(" Allies: ").color(NamedTextColor.WHITE).append(Component.text(getAllianceList(player, target)))).append(Component.newline())
                .append(Component.text(" Enemies: ").color(NamedTextColor.WHITE).append(Component.text(getEnemyList(player, target)))).append(Component.newline())
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
                enemies.add((relation.getPrimaryMiniColor() + enemy.getClan().getName() + " " + clan.getDominanceString(enemy.getClan())).trim());
            }
        }
        return String.join("<gray>, ", enemies);
    }

    public String getMembersList(Clan clan) {
        StringBuilder membersString = new StringBuilder();
        if (clan.getMembers() != null && !clan.getMembers().isEmpty()) {
            for (ClanMember member : clan.getMembers()) {
                Optional<Gamer> gamerOptional = gamerManager.getObject(member.getUuid());
                gamerOptional.ifPresent(gamer -> {
                    membersString.append(membersString.length() != 0 ? "<gray>, " : "").append("<yellow>")
                            .append(member.getRoleIcon())
                            .append(UtilFormat.getOnlineStatus(member.getUuid()))
                            .append(UtilFormat.spoofNameForLunar(gamer.getClient().getName()));
                });

            }
        }
        return membersString.toString();
    }

    public boolean canHurt(Player player, Player target) {

        Clan targetLocationClan = getClanByLocation(target.getLocation()).orElse(null);
        if (targetLocationClan != null && targetLocationClan.isSafe()) {
            Optional<Gamer> gamerOptional = gamerManager.getObject(target.getUniqueId());
            if (gamerOptional.isPresent()) {
                Gamer gamer = gamerOptional.get();
                if (UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
                    return false;
                }
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

                Optional<Gamer> gamerOptional = gamerManager.getObject(player.getUniqueId().toString());
                if (gamerOptional.isPresent()) {
                    Gamer gamer = gamerOptional.get();
                    // Allow skills if player is combat tagged
                    if (!UtilTime.elapsed(gamer.getLastDamaged(), 15000)) {
                        return true;

                    }
                }

                return false;
            }
        }

        return true;
    }

    public void applyDominance(IClan killed, IClan killer) {
        if (killed == null || killer == null) return;
        if (killed.equals(killer)) return;
        if (!killed.isEnemy(killer)) return;

        ClanEnemy killedEnemy = killed.getEnemy(killer).orElseThrow();
        ClanEnemy killerEnemy = killer.getEnemy(killed).orElseThrow();

        int killerSize = killer.getSquadCount();
        int killedSize = killer.getSquadCount();

        int sizeOffset = Math.min(6, 6 - Math.min(killerSize - killedSize, 6));

        int dominance = dominanceScale.getOrDefault(sizeOffset, 6);

        // If the killed players clan has no dominance on the killer players clan, then give dominance to the killer
        if (killedEnemy.getDominance() == 0) {
            killerEnemy.addDominance(dominance);
        }
        killedEnemy.takeDominance(dominance);

        killed.messageClan("You lost <red>" + dominance + "%<gray> dominance to <red>" + killer.getName(), null, true);
        killer.messageClan("You gained <green>" + dominance + "%<gray> dominance on <red>" + killed.getName(), null, true);

        getRepository().updateDominance(killed, killedEnemy);
        getRepository().updateDominance(killer, killerEnemy);

        if (killerEnemy.getDominance() == 100) {
            UtilServer.callEvent(new PillageStartEvent(new Pillage(killer, killed)));
        }

        killed.getMembers().forEach(member -> {
            Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
            if (player != null) {
                UtilServer.callEvent(new ScoreboardUpdateEvent(player));
            }
        });

        killer.getMembers().forEach(member -> {
            Player player = Bukkit.getPlayer(UUID.fromString(member.getUuid()));
            if (player != null) {
                UtilServer.callEvent(new ScoreboardUpdateEvent(player));
            }
        });
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

    @Override
    public void loadFromList(List<Clan> objects) {
        // Load the base clan objects first so they can be referenced in the loop below
        objects.forEach(clan -> addObject(clan.getName().toLowerCase(), clan));

        objects.forEach(clan -> {
            clan.setTerritory(repository.getTerritory(clan));
            clan.setAlliances(repository.getAlliances(this, clan));
            clan.setEnemies(repository.getEnemies(this, clan));
            clan.setMembers(repository.getMembers(clan));
            clan.setInsurance(repository.getInsurance(clan));
        });

        log.info("Loaded {} clans", objects.size());
    }

    public boolean isInSafeZone(Player player) {
        Optional<Clan> clanOptional = getClanByLocation(player.getLocation());
        if (clanOptional.isPresent()) {
            Clan clan = clanOptional.get();
            return clan.isAdmin() && clan.isSafe();
        }
        return false;
    }
}
