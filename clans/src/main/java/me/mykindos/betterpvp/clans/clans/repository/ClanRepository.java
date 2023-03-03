package me.mykindos.betterpvp.clans.clans.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.insurance.Insurance;
import me.mykindos.betterpvp.clans.clans.insurance.InsuranceType;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.BooleanStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Singleton
public class ClanRepository implements IRepository<Clan> {

    @Inject
    @Config(path = "clans.database.prefix", defaultValue = "clans_")
    private String databasePrefix;

    private final Clans clans;
    private final Database database;

    private final ConcurrentHashMap<String, Statement> queuedPropertyUpdates;

    @Inject
    public ClanRepository(Clans clans, Database database) {
        this.clans = clans;
        this.database = database;
        this.queuedPropertyUpdates = new ConcurrentHashMap<>();
    }

    @Override
    public List<Clan> getAll() {
        List<Clan> clanList = new ArrayList<>();
        String query = "SELECT * FROM " + databasePrefix + "clans;";
        CachedRowSet result = database.executeQuery(new Statement(query));
        try {
            while (result.next()) {
                int clanId = result.getInt(1);
                String name = result.getString(2);
                Timestamp timeCreated = result.getTimestamp(3);
                Location home = UtilWorld.stringToLocation(result.getString(4));
                boolean admin = result.getBoolean(5);
                boolean safe = result.getBoolean(6);
                int energy = result.getInt(7);
                int points = result.getInt(8);
                long cooldown = result.getLong(9);
                int level = result.getInt(10);
                Timestamp lastLogin = result.getTimestamp(11);

                Clan clan = Clan.builder().id(clanId)
                        .name(name)
                        .timeCreated(timeCreated)
                        .home(home)
                        .admin(admin)
                        .safe(safe)
                        .energy(energy)
                        .points(points)
                        .cooldown(cooldown)
                        .level(level)
                        .lastLogin(lastLogin)
                        .build();
                loadProperties(clan);
                clanList.add(clan);

            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }


        return clanList;
    }

    private void loadProperties(Clan clan) {
        String query = "SELECT properties.Property, Value, Type FROM " + databasePrefix + "clan_properties properties INNER JOIN "
                + "property_map map on properties.Property = map.Property WHERE Clan = ?";
        CachedRowSet result = database.executeQuery(new Statement(query, new IntegerStatementValue(clan.getId())));
        try {
            while (result.next()) {
                String value = result.getString(1);
                String type = result.getString(3);
                Object property = switch (type) {
                    case "int" -> result.getInt(2);
                    case "boolean" -> Boolean.parseBoolean(result.getString(2));
                    case "double" -> Double.parseDouble(result.getString(2));
                    default -> Class.forName(type).cast(result.getObject(2));
                };

                clan.putProperty(value, property);
            }
        } catch (SQLException | ClassNotFoundException ex) {
            ex.printStackTrace();
        }
    }

    public void saveProperty(Clan clan, String property, Object value) {
        String savePropertyQuery = "INSERT INTO " + databasePrefix + "clan_properties (Clan, Property, Value) VALUES (?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE Value = ?";
        Statement statement = new Statement(savePropertyQuery,
                new IntegerStatementValue(clan.getId()),
                new StringStatementValue(property),
                new StringStatementValue(value.toString()),
                new StringStatementValue(value.toString()));
        queuedPropertyUpdates.put(clan.getId() + property, statement);
    }

    public void processPropertyUpdates(boolean async){
        ConcurrentHashMap<String, Statement> statements = new ConcurrentHashMap<>(queuedPropertyUpdates);
        queuedPropertyUpdates.clear();

        List<Statement> statementList = statements.values().stream().toList();
        database.executeBatch(statementList, async);

        log.info("Updated clan properties with {} queries", statements.size());
    }

    @Override
    public void save(Clan clan) {

        /*
         * Run this async as I am relying on a blocking function to get the clan id generated by MySQL
         */
        UtilServer.runTaskAsync(clans, () -> {
            String saveClanQuery = "INSERT INTO " + databasePrefix + "clans (id, Name, Admin) VALUES (?, ?, ?);";
            database.executeUpdate(new Statement(saveClanQuery,
                    new IntegerStatementValue(clan.getId()),
                    new StringStatementValue(clan.getName()),
                    new BooleanStatementValue(clan.isAdmin())));

            String getClanIdQuery = "SELECT id FROM " + databasePrefix + "clans WHERE Name = ?";
            CachedRowSet result = database.executeQuery(new Statement(getClanIdQuery, new StringStatementValue(clan.getName())));
            try {
                if (result.next()) {
                    clan.setId(result.getInt(1));
                    clan.saveDefaultProperties();
                }
            } catch (SQLException ex) {
                ex.printStackTrace();
            }

            for (var member : clan.getMembers()) {
                saveClanMember(clan, member);
            }
        });
    }

    public void delete(Clan clan) {
        String deleteMembersQuery = "DELETE FROM " + databasePrefix + "clan_members WHERE Clan = ?;";
        database.executeUpdateAsync(new Statement(deleteMembersQuery, new IntegerStatementValue(clan.getId())));

        String deleteAllianceQuery = "DELETE FROM " + databasePrefix + "clan_alliances WHERE Clan = ? OR AllyClan = ?;";
        database.executeUpdateAsync(new Statement(deleteAllianceQuery,
                new IntegerStatementValue(clan.getId()), new IntegerStatementValue(clan.getId())));

        String deleteEnemiesQuery = "DELETE FROM " + databasePrefix + "clan_enemies WHERE Clan = ? OR EnemyClan = ?;";
        database.executeUpdateAsync(new Statement(deleteEnemiesQuery,
                new IntegerStatementValue(clan.getId()), new IntegerStatementValue(clan.getId())));

        String deleteTerritoryQuery = "DELETE FROM " + databasePrefix + "clan_territory WHERE Clan = ?;";
        database.executeUpdateAsync(new Statement(deleteTerritoryQuery, new IntegerStatementValue(clan.getId())));

        String deleteClanQuery = "DELETE FROM " + databasePrefix + "clans WHERE id = ?;";
        database.executeUpdateAsync(new Statement(deleteClanQuery, new IntegerStatementValue(clan.getId())));
    }

    public void updateClanHome(Clan clan) {
        String query = "UPDATE " + databasePrefix + "clans SET Home = ? WHERE id = ?;";
        database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(UtilWorld.locationToString(clan.getHome())),
                new IntegerStatementValue(clan.getId())));
    }

    //region Clan territory
    public void saveClanTerritory(IClan clan, String chunk) {
        String query = "INSERT INTO " + databasePrefix + "clan_territory (Clan, Chunk) VALUES (?, ?);";
        database.executeUpdateAsync(new Statement(query, new IntegerStatementValue(clan.getId()), new StringStatementValue(chunk)));
    }

    public void deleteClanTerritory(IClan clan, String chunk) {
        String query = "DELETE FROM " + databasePrefix + "clan_territory WHERE Clan = ? AND Chunk = ?;";
        database.executeUpdateAsync(new Statement(query, new IntegerStatementValue(clan.getId()), new StringStatementValue(chunk)));
    }

    public List<ClanTerritory> getTerritory(Clan clan) {
        List<ClanTerritory> territory = new ArrayList<>();
        String query = "SELECT * FROM " + databasePrefix + "clan_territory WHERE Clan = ?;";
        CachedRowSet result = database.executeQuery(new Statement(query, new IntegerStatementValue(clan.getId())));
        try {
            while (result.next()) {
                String chunk = result.getString(3);
                territory.add(new ClanTerritory(chunk));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return territory;
    }
    //endregion

    //region Clan members
    public void saveClanMember(Clan clan, ClanMember member) {
        String query = "INSERT INTO " + databasePrefix + "clan_members (Clan, Member, `Rank`) VALUES (?, ?, ?);";
        database.executeUpdateAsync(new Statement(query,
                new IntegerStatementValue(clan.getId()),
                new StringStatementValue(member.getUuid()),
                new StringStatementValue(member.getRank().name())));
    }

    public void deleteClanMember(Clan clan, ClanMember member) {
        String deleteMembersQuery = "DELETE FROM " + databasePrefix + "clan_members WHERE Clan = ? AND Member = ?;";
        database.executeUpdateAsync(new Statement(deleteMembersQuery, new IntegerStatementValue(clan.getId()),
                new StringStatementValue(member.getUuid())));
    }

    public List<ClanMember> getMembers(Clan clan) {
        List<ClanMember> members = new ArrayList<>();
        String query = "SELECT * FROM " + databasePrefix + "clan_members WHERE Clan = ?;";
        CachedRowSet result = database.executeQuery(new Statement(query, new IntegerStatementValue(clan.getId())));
        try {
            while (result.next()) {
                String uuid = result.getString(3);
                ClanMember.MemberRank rank = ClanMember.MemberRank.valueOf(result.getString(4));
                members.add(new ClanMember(uuid, rank));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return members;
    }

    public void updateClanMemberRank(Clan clan, ClanMember member) {
        String query = "UPDATE " + databasePrefix + "clan_members SET `Rank` = ? WHERE Clan = ? AND Member = ?;";
        database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(member.getRank().name()),
                new IntegerStatementValue(clan.getId()),
                new StringStatementValue(member.getUuid())));
    }

    //endregion

    //region Clan alliances
    public void saveClanAlliance(IClan clan, ClanAlliance alliance) {
        String query = "INSERT INTO " + databasePrefix + "clan_alliances (Clan, AllyClan, Trusted) VALUES (?, ?, ?);";
        database.executeUpdateAsync(new Statement(query,
                new IntegerStatementValue(clan.getId()),
                new IntegerStatementValue(alliance.getClan().getId()),
                new BooleanStatementValue(alliance.isTrusted())));
    }

    public void deleteClanAlliance(IClan clan, ClanAlliance alliance) {
        String query = "DELETE FROM " + databasePrefix + "clan_alliances WHERE Clan = ? AND AllyClan = ?;";
        database.executeUpdateAsync(new Statement(query,
                new IntegerStatementValue(clan.getId()),
                new IntegerStatementValue(alliance.getClan().getId())));
    }

    public List<ClanAlliance> getAlliances(ClanManager clanManager, Clan clan) {
        List<ClanAlliance> alliances = new ArrayList<>();
        String query = "SELECT * FROM " + databasePrefix + "clan_alliances WHERE Clan = ?;";
        CachedRowSet result = database.executeQuery(new Statement(query, new IntegerStatementValue(clan.getId())));
        try {
            while (result.next()) {
                var otherClan = clanManager.getClanById(result.getInt(3));
                if (otherClan.isPresent()) {
                    boolean trusted = result.getBoolean(4);
                    alliances.add(new ClanAlliance(otherClan.get(), trusted));
                } else {
                    log.warn("Could not find clan with id {}", result.getInt(3));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return alliances;
    }
    //endregion

    //region Clan enemies
    public void saveClanEnemy(IClan clan, ClanEnemy enemy) {
        String query = "INSERT INTO " + databasePrefix + "clan_enemies (Clan, EnemyClan, Dominance) VALUES (?, ?, ?);";
        database.executeUpdateAsync(new Statement(query,
                new IntegerStatementValue(clan.getId()),
                new IntegerStatementValue(enemy.getClan().getId()),
                new IntegerStatementValue(enemy.getDominance())));
    }

    public void deleteClanEnemy(IClan clan, ClanEnemy enemy) {
        String query = "DELETE FROM " + databasePrefix + "clan_enemies WHERE Clan = ? AND EnemyClan = ?;";
        database.executeUpdateAsync(new Statement(query,
                new IntegerStatementValue(clan.getId()),
                new IntegerStatementValue(enemy.getClan().getId())));
    }

    public void updateDominance(IClan clan, ClanEnemy enemy) {
        String query = "UPDATE " + databasePrefix + "clan_enemies SET Dominance = ? WHERE Clan = ? AND EnemyClan = ?;";
        database.executeUpdateAsync(new Statement(query,
                new IntegerStatementValue(enemy.getDominance()),
                new IntegerStatementValue(clan.getId()),
                new IntegerStatementValue(enemy.getClan().getId())));
    }

    public List<ClanEnemy> getEnemies(ClanManager clanManager, Clan clan) {
        List<ClanEnemy> enemies = new ArrayList<>();
        String query = "SELECT * FROM " + databasePrefix + "clan_enemies WHERE Clan = ?;";
        CachedRowSet result = database.executeQuery(new Statement(query, new IntegerStatementValue(clan.getId())));
        try {
            while (result.next()) {
                var otherClan = clanManager.getClanById(result.getInt(3));
                if (otherClan.isPresent()) {
                    int dominance = result.getInt(4);
                    enemies.add(new ClanEnemy(otherClan.get(), dominance));
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return enemies;
    }
    //endregion

    public Map<Integer, Integer> getDominanceScale() {
        HashMap<Integer, Integer> dominanceScale = new HashMap<>();
        String query = "SELECT * FROM " + databasePrefix + "dominance_scale;";
        CachedRowSet result = database.executeQuery(new Statement(query));
        try {
            if (result.next()) {
                dominanceScale.put(result.getInt(1), result.getInt(2));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return dominanceScale;
    }

    //region Insurance
    public void deleteExpiredInsurance(long duration) {
        String query = "DELETE FROM " + databasePrefix + "insurance WHERE ((Time+?) - ?) <= 0";
        database.executeUpdate(new Statement(query, new LongStatementValue(duration), new LongStatementValue(System.currentTimeMillis())));
    }

    public void deleteInsuranceForClan(Clan clan) {
        String query = "DELETE FROM " + databasePrefix + "insurance WHERE Clan = ?";
        database.executeUpdate(new Statement(query, new IntegerStatementValue(clan.getId())));
    }

    public void saveInsurance(Clan clan, Insurance insurance) {
        String query = "INSERT INTO " + databasePrefix + "insurance VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Location location = insurance.getBlockLocation();
        database.executeUpdateAsync(new Statement(query,
                new IntegerStatementValue(clan.getId()), new StringStatementValue(insurance.getInsuranceType().name()),
                new StringStatementValue(insurance.getBlockMaterial().name()), new StringStatementValue(insurance.getBlockData()),
                new LongStatementValue(insurance.getTime()), new IntegerStatementValue(location.getBlockX()),
                new IntegerStatementValue(location.getBlockY()), new IntegerStatementValue(location.getBlockZ())
        ));
    }

    public List<Insurance> getInsurance(Clan clan) {
        World world = Bukkit.getWorld("world");
        List<Insurance> insurance = Collections.synchronizedList(new ArrayList<>());
        String query = "SELECT * FROM " + databasePrefix + "insurance WHERE Clan = ? ORDER BY Time ASC";
        CachedRowSet result = database.executeQuery(new Statement(query, new IntegerStatementValue(clan.getId())));
        try {
            while (result.next()) {
                InsuranceType insuranceType = InsuranceType.valueOf(result.getString(2));
                Material material = Material.valueOf(result.getString(3));
                String blockData = result.getString(4);
                long time = result.getLong(5);
                int blockX = result.getInt(6);
                int blockY = result.getInt(7);
                int blockZ = result.getInt(8);

                Location blockLocation = new Location(world, blockX, blockY, blockZ);
                insurance.add(new Insurance(time, material, blockData, insuranceType, blockLocation));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }

        return insurance;
    }
    //endregion

}
