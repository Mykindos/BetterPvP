package me.mykindos.betterpvp.clans.clans.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.core.ClanCore;
import me.mykindos.betterpvp.clans.clans.insurance.Insurance;
import me.mykindos.betterpvp.clans.clans.insurance.InsuranceType;
import me.mykindos.betterpvp.clans.logging.KillClanLog;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.mappers.PropertyMapper;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.BooleanStatementValue;
import me.mykindos.betterpvp.core.database.query.values.DoubleStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.LongStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.logging.CachedLog;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.UtilWorld;
import me.mykindos.betterpvp.core.utilities.model.item.banner.BannerColor;
import me.mykindos.betterpvp.core.utilities.model.item.banner.BannerWrapper;
import me.mykindos.betterpvp.core.world.model.BPvPWorld;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BannerMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import javax.sql.rowset.CachedRowSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@CustomLog
@Singleton
public class ClanRepository implements IRepository<Clan> {

    private final Database database;
    private final PropertyMapper propertyMapper;
    private final LogRepository logRepository;

    private final ConcurrentHashMap<String, Statement> queuedPropertyUpdates;

    @Inject
    public ClanRepository(Database database, PropertyMapper propertyMapper, LogRepository logRepository) {
        this.database = database;
        this.propertyMapper = propertyMapper;
        this.logRepository = logRepository;
        this.queuedPropertyUpdates = new ConcurrentHashMap<>();
    }

    @Override
    public List<Clan> getAll() {
        List<Clan> clanList = new ArrayList<>();
        String query = "SELECT * FROM clans;";

        try (CachedRowSet result = database.executeQuery(new Statement(query)).join()) {
            while (result.next()) {
                UUID clanId = UUID.fromString(result.getString(1));
                String name = result.getString(2);
                String coreLocString = result.getString(3);
                Location coreLoc = coreLocString == null || coreLocString.isEmpty() ? null : UtilWorld.stringToLocation(coreLocString);
                boolean admin = result.getBoolean(4);
                boolean safe = result.getBoolean(5);
                String banner = result.getString(6);
                String vault = result.getString(7);
                String mailbox = result.getString(8);

                Clan clan = new Clan(clanId);
                clan.setName(name);
                clan.getCore().setPosition(coreLoc);
                clan.setAdmin(admin);
                clan.setSafe(safe);

                if (vault != null) {
                    clan.getCore().getVault().read(vault);
                }

                if(mailbox != null) {
                    clan.getCore().getMailbox().read(mailbox);
                }

                if (banner != null && !banner.isEmpty()) {
                    final ItemStack bannerItem = ItemStack.deserializeBytes(Base64.getDecoder().decode(banner));
                    final ItemMeta meta = bannerItem.getItemMeta();
                    final BannerMeta bannerMeta = (BannerMeta) meta;
                    final BannerColor color = BannerColor.fromType(bannerItem.getType());
                    final List<Pattern> patterns = bannerMeta.getPatterns();
                    clan.setBanner(BannerWrapper.builder().baseColor(color).patterns(patterns).build());
                }

                loadProperties(clan);
                clanList.add(clan);

            }
        } catch (SQLException ex) {
            log.error("Failed to load clans", ex).submit();
        }


        return clanList;
    }

    private void loadProperties(Clan clan) {
        String query = "SELECT Property, Value FROM clan_properties WHERE Clan = ?";

        try (CachedRowSet result = database.executeQuery(new Statement(query, new UuidStatementValue(clan.getId()))).join()){
            propertyMapper.parseProperties(result, clan);
        } catch (SQLException | ClassNotFoundException ex) {
            log.error("Failed to load clan properties for {}", clan.getId(), ex).submit();
        }

        clan.getProperties().registerListener(clan);
    }

    public void saveProperty(Clan clan, String property, Object value) {
        String savePropertyQuery = "INSERT INTO clan_properties (Clan, Property, Value) VALUES (?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE Value = ?";
        Statement statement = new Statement(savePropertyQuery,
                new UuidStatementValue(clan.getId()),
                new StringStatementValue(property),
                new StringStatementValue(value.toString()),
                new StringStatementValue(value.toString()));
        queuedPropertyUpdates.put(clan.getId() + property, statement);
    }

    public void processPropertyUpdates(boolean async) {
        ConcurrentHashMap<String, Statement> statements = new ConcurrentHashMap<>(queuedPropertyUpdates);
        queuedPropertyUpdates.clear();

        List<Statement> statementList = statements.values().stream().toList();
        database.executeBatch(statementList);

        log.info("Updated clan properties with {} queries", statements.size()).submit();
    }

    @Override
    public void save(Clan clan) {

        String saveClanQuery = "INSERT INTO clans (id, Name, Admin) VALUES (?, ?, ?);";
        database.executeUpdate(new Statement(saveClanQuery,
                new UuidStatementValue(clan.getId()),
                new StringStatementValue(clan.getName()),
                new BooleanStatementValue(clan.isAdmin())));

        for (var member : clan.getMembers()) {
            saveClanMember(clan, member);
        }

    }

    public void delete(Clan clan) {

        String deleteMembersQuery = "DELETE FROM clan_members WHERE Clan = ?;";
        database.executeUpdateAsync(new Statement(deleteMembersQuery, new UuidStatementValue(clan.getId())));

        String deleteAllianceQuery = "DELETE FROM clan_alliances WHERE Clan = ? OR AllyClan = ?;";
        database.executeUpdateAsync(new Statement(deleteAllianceQuery,
                new UuidStatementValue(clan.getId()), new UuidStatementValue(clan.getId())));

        String deleteEnemiesQuery = "DELETE FROM clan_enemies WHERE Clan = ? OR EnemyClan = ?;";
        database.executeUpdateAsync(new Statement(deleteEnemiesQuery,
                new UuidStatementValue(clan.getId()), new UuidStatementValue(clan.getId())));

        String deleteTerritoryQuery = "DELETE FROM clan_territory WHERE Clan = ?;";
        database.executeUpdateAsync(new Statement(deleteTerritoryQuery, new UuidStatementValue(clan.getId())));

        String deletePropertiesQuery = "DELETE FROM clan_properties WHERE Clan = ?;";
        database.executeUpdateAsync(new Statement(deletePropertiesQuery, new UuidStatementValue(clan.getId())));

        String deleteClanQuery = "DELETE FROM clans WHERE id = ?;";
        database.executeUpdateAsync(new Statement(deleteClanQuery, new UuidStatementValue(clan.getId())));
    }

    public void updateClanCore(Clan clan) {
        String query = "UPDATE clans SET Home = ? WHERE id = ?;";
        ClanCore core = clan.getCore();
        database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(core.getPosition() == null ? "" : UtilWorld.locationToString(core.getPosition(), false)),
                new UuidStatementValue(clan.getId())));
    }

    public void updateClanName(Clan clan) {
        String query = "UPDATE clans SET Name = ? WHERE id = ?;";
        database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(clan.getName()),
                new UuidStatementValue(clan.getId())));
    }

    public void updateClanBanner(Clan clan) {
        String query = "UPDATE clans SET Banner = ? WHERE id = ?;";
        database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(Base64.getEncoder().encodeToString(clan.getBanner().get().serializeAsBytes())),
                new UuidStatementValue(clan.getId())));
    }

    public void updateClanSafe(Clan clan) {
        String query = "UPDATE clans SET Safe = ? WHERE id = ?;";
        database.executeUpdateAsync(new Statement(query,
                new BooleanStatementValue(clan.isSafe()),
                new UuidStatementValue(clan.getId())));
    }

    public void updateClanAdmin(Clan clan) {
        String query = "UPDATE clans SET Admin = ? WHERE id = ?;";
        database.executeUpdateAsync(new Statement(query,
                new BooleanStatementValue(clan.isAdmin()),
                new UuidStatementValue(clan.getId())));
    }

    public void updateClanVault(Clan clan) {
        String query = "UPDATE clans SET Vault = ? WHERE id = ?;";
        database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(clan.getCore().getVault().serialize()),
                new UuidStatementValue(clan.getId())));
    }

    public CompletableFuture<Void> updateClanMailbox(Clan clan) {
        String query = "UPDATE clans SET Mailbox = ? WHERE id = ?;";
        return database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(clan.getCore().getMailbox().serialize()),
                new UuidStatementValue(clan.getId())));
    }

    //region Clan territory
    public void saveClanTerritory(IClan clan, String chunk) {
        String query = "INSERT INTO clan_territory (Clan, Chunk) VALUES (?, ?);";
        database.executeUpdateAsync(new Statement(query, new UuidStatementValue(clan.getId()), new StringStatementValue(chunk)));
    }

    public void deleteClanTerritory(IClan clan, String chunk) {
        String query = "DELETE FROM clan_territory WHERE Clan = ? AND Chunk = ?;";
        database.executeUpdateAsync(new Statement(query, new UuidStatementValue(clan.getId()), new StringStatementValue(chunk)));
    }

    public List<ClanTerritory> getTerritory(Clan clan) {
        List<ClanTerritory> territory = new ArrayList<>();
        String query = "SELECT * FROM clan_territory WHERE Clan = ?;";

        try (CachedRowSet result = database.executeQuery(new Statement(query, new UuidStatementValue(clan.getId()))).join()) {
            while (result.next()) {
                String chunk = result.getString(3);
                territory.add(new ClanTerritory(chunk));
            }
        } catch (SQLException ex) {
            log.error("Failed to load clan territory for {}", clan.getId(), ex).submit();
        }

        return territory;
    }
    //endregion

    //region Clan members
    public void saveClanMember(Clan clan, ClanMember member) {
        String query = "INSERT INTO clan_members (Clan, Member, `Rank`) VALUES (?, ?, ?);";
        database.executeUpdateAsync(new Statement(query,
                new UuidStatementValue(clan.getId()),
                new StringStatementValue(member.getUuid()),
                new StringStatementValue(member.getRank().name())));
    }

    public void deleteClanMember(Clan clan, ClanMember member) {
        String deleteMembersQuery = "DELETE FROM clan_members WHERE Clan = ? AND Member = ?;";
        database.executeUpdateAsync(new Statement(deleteMembersQuery, new UuidStatementValue(clan.getId()),
                new StringStatementValue(member.getUuid())));
    }

    public List<ClanMember> getMembers(Clan clan) {
        List<ClanMember> members = new ArrayList<>();
        String query = "SELECT * FROM clan_members WHERE Clan = ?;";

        try (CachedRowSet result = database.executeQuery(new Statement(query, new UuidStatementValue(clan.getId()))).join()) {
            while (result.next()) {
                String uuid = result.getString(3);
                ClanMember.MemberRank rank = ClanMember.MemberRank.valueOf(result.getString(4));

                String name = "";
                // Doing it this way so we don't load the client into the cache unnecessarily, we'll remove this when we merge the databases
                try(CachedRowSet nameResult =  database.executeQuery(new Statement("SELECT Name FROM clients WHERE UUID = ?",
                        new UuidStatementValue(UUID.fromString(uuid))), TargetDatabase.GLOBAL).join()) {
                    while(nameResult.next()) {
                        name = nameResult.getString(1);
                    }
                }
                members.add(new ClanMember(uuid, rank, name));
            }
        } catch (SQLException ex) {
            log.error("Failed to load clan members for {}", clan.getId(), ex).submit();
        }

        return members;
    }

    public void updateClanMemberRank(Clan clan, ClanMember member) {
        String query = "UPDATE clan_members SET `Rank` = ? WHERE Clan = ? AND Member = ?;";
        database.executeUpdateAsync(new Statement(query,
                new StringStatementValue(member.getRank().name()),
                new UuidStatementValue(clan.getId()),
                new StringStatementValue(member.getUuid())));
    }

    //endregion

    //region Clan alliances
    public void saveClanAlliance(IClan clan, ClanAlliance alliance) {
        String query = "INSERT INTO clan_alliances (Clan, AllyClan, Trusted) VALUES (?, ?, ?);";
        database.executeUpdateAsync(new Statement(query,
                new UuidStatementValue(clan.getId()),
                new UuidStatementValue(alliance.getClan().getId()),
                new BooleanStatementValue(alliance.isTrusted())));
    }

    public void deleteClanAlliance(IClan clan, ClanAlliance alliance) {
        String query = "DELETE FROM clan_alliances WHERE Clan = ? AND AllyClan = ?;";
        database.executeUpdateAsync(new Statement(query,
                new UuidStatementValue(clan.getId()),
                new UuidStatementValue(alliance.getClan().getId())));
    }

    public List<ClanAlliance> getAlliances(ClanManager clanManager, Clan clan) {
        List<ClanAlliance> alliances = new ArrayList<>();
        String query = "SELECT * FROM clan_alliances WHERE Clan = ?;";

        try (CachedRowSet result = database.executeQuery(new Statement(query, new UuidStatementValue(clan.getId()))).join()) {
            while (result.next()) {
                var otherClan = clanManager.getClanById(UUID.fromString(result.getString(3)));
                if (otherClan.isPresent()) {
                    boolean trusted = result.getBoolean(4);
                    alliances.add(new ClanAlliance(otherClan.get(), trusted));
                } else {
                    log.warn("Could not find clan with id {}", result.getInt(3)).submit();
                }
            }
        } catch (SQLException ex) {
            log.error("Failed to load clan alliances for {}", clan.getId(), ex).submit();
        }

        return alliances;
    }

    public void saveTrust(IClan clan, ClanAlliance alliance) {
        String query = "UPDATE clan_alliances SET Trusted = ? WHERE Clan = ? AND AllyClan = ?;";
        database.executeUpdateAsync(new Statement(query,
                new BooleanStatementValue(alliance.isTrusted()),
                new UuidStatementValue(clan.getId()),
                new UuidStatementValue(alliance.getClan().getId())));
    }
    //endregion

    //region Clan enemies
    public void saveClanEnemy(IClan clan, ClanEnemy enemy) {
        String query = "INSERT INTO clan_enemies (Clan, EnemyClan, Dominance) VALUES (?, ?, ?);";
        database.executeUpdateAsync(new Statement(query,
                new UuidStatementValue(clan.getId()),
                new UuidStatementValue(enemy.getClan().getId()),
                new DoubleStatementValue(enemy.getDominance())));
    }

    public void deleteClanEnemy(IClan clan, ClanEnemy enemy) {
        String query = "DELETE FROM clan_enemies WHERE Clan = ? AND EnemyClan = ?;";
        database.executeUpdateAsync(new Statement(query,
                new UuidStatementValue(clan.getId()),
                new UuidStatementValue(enemy.getClan().getId())));
    }

    public void updateDominance(IClan clan, ClanEnemy enemy) {
        String query = "UPDATE clan_enemies SET Dominance = ? WHERE Clan = ? AND EnemyClan = ?;";
        database.executeUpdateAsync(new Statement(query,
                new DoubleStatementValue(enemy.getDominance()),
                new UuidStatementValue(clan.getId()),
                new UuidStatementValue(enemy.getClan().getId())));
    }

    public List<ClanEnemy> getEnemies(ClanManager clanManager, Clan clan) {
        List<ClanEnemy> enemies = new ArrayList<>();
        String query = "SELECT * FROM clan_enemies WHERE Clan = ?;";

        try (CachedRowSet result = database.executeQuery(new Statement(query, new UuidStatementValue(clan.getId()))).join()) {
            while (result.next()) {
                var otherClan = clanManager.getClanById(UUID.fromString(result.getString(3)));
                if (otherClan.isPresent()) {
                    int dominance = result.getInt(4);
                    enemies.add(new ClanEnemy(otherClan.get(), dominance));
                }
            }
        } catch (SQLException ex) {
            log.error("Failed to load clan enemies for {}", clan.getId(), ex).submit();
        }

        return enemies;
    }
    //endregion

    public Map<Integer, Double> getDominanceScale() {
        HashMap<Integer, Double> dominanceScale = new HashMap<>();
        String query = "SELECT * FROM clans_dominance_scale;";

        try (CachedRowSet result = database.executeQuery(new Statement(query)).join()) {
            while (result.next()) {
                dominanceScale.put(result.getInt(1), result.getDouble(2));
            }
        } catch (SQLException ex) {
            log.error("Failed to load dominance scale", ex).submit();
        }

        return dominanceScale;
    }

    //region Insurance
    public void deleteExpiredInsurance(long duration) {
        String query = "DELETE FROM clan_insurance WHERE ((Time+?) - ?) <= 0";
        database.executeUpdate(new Statement(query, new LongStatementValue(duration), new LongStatementValue(System.currentTimeMillis())));
    }

    public void deleteInsuranceForClan(Clan clan) {
        String query = "DELETE FROM clan_insurance WHERE Clan = ?";
        database.executeUpdate(new Statement(query, new UuidStatementValue(clan.getId())));
    }

    public void saveInsurance(Clan clan, Insurance insurance) {
        String query = "INSERT INTO clan_insurance VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        Location location = insurance.getBlockLocation();
        database.executeUpdateAsync(new Statement(query,
                new UuidStatementValue(clan.getId()), new StringStatementValue(insurance.getInsuranceType().name()),
                new StringStatementValue(insurance.getBlockMaterial().name()), new StringStatementValue(insurance.getBlockData()),
                new LongStatementValue(insurance.getTime()), new IntegerStatementValue(location.getBlockX()),
                new IntegerStatementValue(location.getBlockY()), new IntegerStatementValue(location.getBlockZ())
        ));
    }

    public List<Insurance> getInsurance(Clan clan) {
        World world = Bukkit.getWorld(BPvPWorld.MAIN_WORLD_NAME);
        List<Insurance> insurance = Collections.synchronizedList(new ArrayList<>());
        String query = "SELECT * FROM clan_insurance WHERE Clan = ? ORDER BY Time ASC";
        try (CachedRowSet result = database.executeQuery(new Statement(query, new UuidStatementValue(clan.getId()))).join()) {
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
            log.error("Failed to load insurance for {}", clan.getId(), ex).submit();
        }

        return insurance;
    }
    //endregion

    public List<UUID> getPlayersByClan(UUID clanID) {
        List<UUID> playerIDs = new ArrayList<>();

        List<CachedLog> logs = logRepository.getLogsWithContextAndAction(LogContext.CLAN, clanID.toString(), "CLAN_");
        logs.removeIf(cachedLog -> !cachedLog.getAction().equalsIgnoreCase("CLAN_CREATE")
                && !cachedLog.getAction().equalsIgnoreCase("CLAN_JOIN"));

        logs.forEach(cachedLog -> {
            String playerID = cachedLog.getContext().get(LogContext.CLIENT);
            playerIDs.add(UUID.fromString(playerID));
        });

        return playerIDs;
    }

    public Map<UUID, String> getClansByPlayer(UUID playerID) {
        Map<UUID, String> clans = new HashMap<>();

        List<CachedLog> logs = logRepository.getLogsWithContextAndAction(LogContext.CLIENT, playerID.toString(), "CLAN_");
        logs.removeIf(cachedLog -> !cachedLog.getAction().equalsIgnoreCase("CLAN_CREATE")
                && !cachedLog.getAction().equalsIgnoreCase("CLAN_JOIN"));

        logs.forEach(cachedLog -> {
            String clanName = cachedLog.getContext().get(LogContext.CLAN_NAME);
            String clanID = cachedLog.getContext().get(LogContext.CLAN);
            clans.put(UUID.fromString(clanID), clanName);
        });

        return clans;
    }

    public void addClanKill(UUID killID, @Nullable Clan killerClan, @Nullable Clan victimClan, double dominance) {
        UtilServer.runTaskAsync(JavaPlugin.getPlugin(Clans.class), () -> {

            String query = "INSERT INTO clans_kills (KillId, KillerClan, VictimClan, Dominance) VALUES (?, ?, ?, ?)";
            database.executeUpdate(new Statement(query,
                    new UuidStatementValue(killID),
                    new UuidStatementValue(killerClan != null ? killerClan.getId() : null),
                    new UuidStatementValue(victimClan != null ? victimClan.getId() : null),
                    new DoubleStatementValue(dominance)
            ));
        });
    }


    /**
     * Gets the related ClanKillLogs for the specified Clan
     * Should be called async
     * @param clan
     * @param clanManager
     * @param clientManager
     * @return
     */
    public List<KillClanLog> getClanKillLogs(Clan clan, ClanManager clanManager, ClientManager clientManager) {
        //Completable future handling from https://www.baeldung.com/java-completablefuture-list-convert
        String query = "CALL GetClanKillLogs(?)";

        CompletableFuture<List<KillClanLog>> listFuture = CompletableFuture.supplyAsync(() -> {
            List<CompletableFuture<KillClanLog>> futures = Collections.synchronizedList(new ArrayList<>());
            try (CachedRowSet result = database.executeQuery(new Statement(query, new UuidStatementValue(clan.getId()))).join()) {
                while (result.next()) {

                    CompletableFuture<KillClanLog> killLogFuture = new CompletableFuture<>();
                    futures.add(killLogFuture);

                    CompletableFuture<Boolean> killerNameFuture = new CompletableFuture<>();

                    UUID killer = UUID.fromString(result.getString(1));

                    AtomicReference<String> killerName = new AtomicReference<>("Unknown Player");
                    clientManager.search().offline(killer).thenAcceptAsync((clientOptional) -> {
                        clientOptional.ifPresent(client -> {
                            killerName.set(client.getName());
                        });
                        killerNameFuture.complete(true);
                    });

                    @Nullable String killerClanId = result.getString(2);
                    UUID killerClan = killerClanId == null ? null : UUID.fromString(killerClanId);

                    AtomicReference<String> killerClanName = new AtomicReference<>("");
                    clanManager.getClanById(killerClan).ifPresent(clanName -> {
                        killerClanName.set(clanName.getName());
                    });

                    CompletableFuture<Boolean> victimNameFuture = new CompletableFuture<>();
                    UUID victim = UUID.fromString(result.getString(3));
                    AtomicReference<String> victimName = new AtomicReference<>("Unknown Player");
                    clientManager.search().offline(victim).thenAcceptAsync((clientOptional) -> {
                        clientOptional.ifPresent(client -> {
                            victimName.set(client.getName());
                        });
                        victimNameFuture.complete(true);
                    });

                    String victimClanId = result.getString(4);
                    //data can be null or empty to indicate no clan, remove after Beta 2
                    //TODO remove after beta 2
                    if (victimClanId != null && victimClanId.isEmpty()) {
                        victimClanId = null;
                    }
                    UUID victimClan = victimClanId == null ? null : UUID.fromString(victimClanId);

                    AtomicReference<String> victimClanName = new AtomicReference<>("");
                    clanManager.getClanById(victimClan).ifPresent(clanName -> {
                        victimClanName.set(clanName.getName());
                    });

                    double dominance = result.getDouble(5);
                    long time = result.getLong(6);

                    CompletableFuture.allOf(killerNameFuture, victimNameFuture).whenComplete((unused, throwable) -> {
                        killLogFuture.complete(new KillClanLog(killerName.get(), killer, killerClanName.get(), killerClan,
                                victimName.get(), victim, victimClanName.get(), victimClan,
                                dominance, time));
                    });
                }
            } catch (SQLException ex) {
                log.error("Failed to get ClanUUID logs", ex).submit();
            }
            CompletableFuture<?>[] futuresArray = futures.toArray(new CompletableFuture<?>[0]);
            return CompletableFuture.allOf(futuresArray).thenApply(v -> futures.stream()
                    .map(CompletableFuture::join)
                    .toList()).join();
        });
        return listFuture.join();
    }
}
