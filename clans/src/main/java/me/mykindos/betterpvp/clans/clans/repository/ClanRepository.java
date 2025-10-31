package me.mykindos.betterpvp.clans.clans.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.clans.Clan;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.clans.core.ClanCore;
import me.mykindos.betterpvp.clans.clans.insurance.Insurance;
import me.mykindos.betterpvp.clans.clans.insurance.InsuranceType;
import me.mykindos.betterpvp.clans.logging.KillClanLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.components.clans.IClan;
import me.mykindos.betterpvp.core.components.clans.data.ClanAlliance;
import me.mykindos.betterpvp.core.components.clans.data.ClanEnemy;
import me.mykindos.betterpvp.core.components.clans.data.ClanMember;
import me.mykindos.betterpvp.core.components.clans.data.ClanTerritory;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.jooq.tables.records.ClanMetadataRecord;
import me.mykindos.betterpvp.core.database.jooq.tables.records.GetClanKillLogsRecord;
import me.mykindos.betterpvp.core.database.mappers.PropertyMapper;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import me.mykindos.betterpvp.core.logging.CachedLog;
import me.mykindos.betterpvp.core.logging.LogContext;
import me.mykindos.betterpvp.core.logging.repository.LogRepository;
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
import org.jetbrains.annotations.Nullable;
import org.jooq.Query;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.exception.DataAccessException;

import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CLANS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLANS_DOMINANCE_SCALE;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLANS_KILLS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLAN_ALLIANCES;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLAN_ENEMIES;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLAN_INSURANCE;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLAN_MEMBERS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLAN_METADATA;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLAN_PROPERTIES;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLAN_TERRITORY;
import static me.mykindos.betterpvp.core.database.jooq.Tables.CLIENTS;
import static me.mykindos.betterpvp.core.database.jooq.Tables.GET_CLAN_KILL_LOGS;

@CustomLog
@Singleton
public class ClanRepository implements IRepository<Clan> {

    private final Database database;
    private final PropertyMapper propertyMapper;
    private final LogRepository logRepository;

    private final ConcurrentHashMap<String, Query> queuedPropertyUpdates;

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

        try {
            var records = database.getDslContext()
                    .selectFrom(CLANS)
                    .where(CLANS.REALM.eq(Core.getCurrentRealm()))
                    .fetch();

            for (var clanRecord : records) {
                long clanId = clanRecord.get(CLANS.ID);
                String name = clanRecord.get(CLANS.NAME);
                String coreLocString = clanRecord.get(CLANS.HOME);
                Location coreLoc = coreLocString == null || coreLocString.isEmpty() ? null : UtilWorld.stringToLocation(coreLocString);
                boolean admin = clanRecord.get(CLANS.ADMIN) != null && clanRecord.get(CLANS.ADMIN) != 0;
                boolean safe = clanRecord.get(CLANS.SAFE) != null && clanRecord.get(CLANS.SAFE) != 0;


                Clan clan = new Clan(clanId);
                clan.setName(name);
                clan.getCore().setPosition(coreLoc);
                clan.setAdmin(admin);
                clan.setSafe(safe);

                loadMetadata(clan);
                loadProperties(clan);
                clanList.add(clan);
            }
        } catch (Exception ex) {
            log.error("Failed to load clans", ex).submit();
        }


        return clanList;
    }

    private void loadMetadata(Clan clan) {
        try {
            ClanMetadataRecord result = database.getDslContext().selectFrom(CLAN_METADATA)
                    .where(CLAN_METADATA.CLAN.eq(clan.getId()))
                    .fetchOne();

            if (result == null) return;

            String banner = result.getBanner();
            String vault = result.getVault();
            String mailbox = result.getMailbox();

            if (vault != null) {
                clan.getCore().getVault().read(vault);
            }

            if (mailbox != null) {
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
        } catch (DataAccessException ex) {
            log.error("Failed to load clan metadata for {}", clan.getId(), ex).submit();
        }
    }

    private void loadProperties(Clan clan) {

        try {
            Result<Record2<String, String>> result = database.getDslContext()
                    .select(CLAN_PROPERTIES.PROPERTY, CLAN_PROPERTIES.VALUE)
                    .from(CLAN_PROPERTIES)
                    .where(CLAN_PROPERTIES.CLAN.eq(clan.getId()))
                    .fetch();

            propertyMapper.parseProperties(result, clan);

        } catch (DataAccessException ex) {
            log.error("Failed to load clan properties for {}", clan.getId(), ex).submit();
        }

        clan.getProperties().registerListener(clan);
    }

    public void saveProperty(Clan clan, String property, Object value) {
        Query query = database.getDslContext()
                .insertInto(CLAN_PROPERTIES)
                .set(CLAN_PROPERTIES.CLAN, clan.getId())
                .set(CLAN_PROPERTIES.PROPERTY, property)
                .set(CLAN_PROPERTIES.VALUE, value.toString())
                .onDuplicateKeyUpdate()
                .set(CLAN_PROPERTIES.VALUE, value.toString());

        queuedPropertyUpdates.put(clan.getId() + property, query);
    }

    public void processPropertyUpdates(boolean async) {
        ConcurrentHashMap<String, Query> statements = new ConcurrentHashMap<>(queuedPropertyUpdates);
        queuedPropertyUpdates.clear();

        List<Query> statementList = statements.values().stream().toList();
        if (async) {
            database.getAsyncDslContext().executeAsyncVoid(ctx -> ctx.batch(statementList).execute());
        } else {
            database.getDslContext().batch(statementList).execute();
        }

        log.info("Updated clan properties with {} queries", statements.size()).submit();
    }

    @Override
    public void save(Clan clan) {

        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.insertInto(CLANS)
                    .set(CLANS.ID, clan.getId())
                    .set(CLANS.REALM, Core.getCurrentRealm())
                    .set(CLANS.NAME, clan.getName())
                    .set(CLANS.ADMIN, clan.isAdmin() ? 1 : 0)
                    .execute();

            ctx.insertInto(CLAN_METADATA)
                    .set(CLAN_METADATA.CLAN, clan.getId())
                    .set(CLAN_METADATA.BANNER, "")
                    .set(CLAN_METADATA.VAULT, "")
                    .set(CLAN_METADATA.MAILBOX, "")
                    .execute();

        });

        for (var member : clan.getMembers()) {
            saveClanMember(clan, member);
        }


    }

    public void delete(Clan clan) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.deleteFrom(CLANS)
                        .where(CLANS.ID.eq(clan.getId()))
                        .execute());
        // The other tables cascade deletes
    }

    public void updateClanCore(Clan clan) {
        ClanCore core = clan.getCore();

        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.update(CLANS)
                        .set(CLANS.HOME, core.getPosition() == null ? "" : UtilWorld.locationToString(core.getPosition(), false))
                        .where(CLANS.ID.eq(clan.getId()))
                        .execute());
    }

    public void updateClanName(Clan clan) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.update(CLANS)
                        .set(CLANS.NAME, clan.getName())
                        .where(CLANS.ID.eq(clan.getId()))
                        .execute());
    }

    public void updateClanSafe(Clan clan) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.update(CLANS)
                        .set(CLANS.SAFE, clan.isSafe() ? 1 : 0)
                        .where(CLANS.ID.eq(clan.getId()))
                        .execute());
    }

    public void updateClanAdmin(Clan clan) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.update(CLANS)
                        .set(CLANS.ADMIN, clan.isAdmin() ? 1 : 0)
                        .where(CLANS.ID.eq(clan.getId()))
                        .execute());
    }

    public void updateClanBanner(Clan clan) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.update(CLAN_METADATA)
                        .set(CLAN_METADATA.BANNER, Base64.getEncoder().encodeToString(clan.getBanner().get().serializeAsBytes()))
                        .where(CLAN_METADATA.CLAN.eq(clan.getId()))
                        .execute());
    }

    public void updateClanVault(Clan clan) {

        var records = database.getDslContext()
                .selectFrom(CLANS)
                .where(CLANS.REALM.eq(Core.getCurrentRealm()))
                .fetch();

        System.out.println(records.size());
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> {
                    ctx.update(CLAN_METADATA)
                            .set(CLAN_METADATA.VAULT, clan.getCore().getVault().serialize())
                            .where(CLAN_METADATA.CLAN.eq(clan.getId()))
                            .execute();

                });
    }

    public CompletableFuture<Void> updateClanMailbox(Clan clan) {
        return database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.update(CLAN_METADATA)
                        .set(CLAN_METADATA.MAILBOX, clan.getCore().getMailbox().serialize())
                        .where(CLAN_METADATA.CLAN.eq(clan.getId()))
                        .execute());
    }

    //region Clan territory
    public void saveClanTerritory(IClan clan, String chunk) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.insertInto(CLAN_TERRITORY)
                        .set(CLAN_TERRITORY.CLAN, clan.getId())
                        .set(CLAN_TERRITORY.CHUNK, chunk)
                        .execute());
    }

    public void deleteClanTerritory(IClan clan, String chunk) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.deleteFrom(CLAN_TERRITORY)
                        .where(CLAN_TERRITORY.CLAN.eq(clan.getId()))
                        .and(CLAN_TERRITORY.CHUNK.eq(chunk))
                        .execute());
    }

    public List<ClanTerritory> getTerritory(Clan clan) {
        List<ClanTerritory> territory = new ArrayList<>();

        try {
            var records = database.getDslContext()
                    .selectFrom(CLAN_TERRITORY)
                    .where(CLAN_TERRITORY.CLAN.eq(clan.getId()))
                    .fetch();

            for (var territoryRecord : records) {
                String chunk = territoryRecord.get(CLAN_TERRITORY.CHUNK);
                territory.add(new ClanTerritory(chunk));
            }
        } catch (Exception ex) {
            log.error("Failed to load clan territory for {}", clan.getId(), ex).submit();
        }

        return territory;
    }
//endregion

    //region Clan members
    public void saveClanMember(Clan clan, ClanMember member) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.insertInto(CLAN_MEMBERS)
                        .set(CLAN_MEMBERS.CLAN, clan.getId())
                        .set(CLAN_MEMBERS.MEMBER, member.getUuid())
                        .set(CLAN_MEMBERS.RANK, member.getRank().name())
                        .execute());
    }

    public void deleteClanMember(Clan clan, ClanMember member) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.deleteFrom(CLAN_MEMBERS)
                        .where(CLAN_MEMBERS.CLAN.eq(clan.getId()))
                        .and(CLAN_MEMBERS.MEMBER.eq(member.getUuid()))
                        .execute());
    }

    public List<ClanMember> getMembers(Clan clan) {
        List<ClanMember> members = new ArrayList<>();

        try {
            var records = database.getDslContext()
                    .select(CLAN_MEMBERS.asterisk(), CLIENTS.NAME, CLIENTS.UUID)
                    .from(CLAN_MEMBERS)
                    .innerJoin(CLIENTS)
                    .on(CLIENTS.UUID.eq(CLAN_MEMBERS.MEMBER))
                    .where(CLAN_MEMBERS.CLAN.eq(clan.getId()))
                    .fetch();

            for (var memberRecord : records) {
                String uuid = memberRecord.get(CLIENTS.UUID);
                ClanMember.MemberRank rank = ClanMember.MemberRank.valueOf(memberRecord.get(CLAN_MEMBERS.RANK));
                String name = memberRecord.get(CLIENTS.NAME);

                members.add(new ClanMember(uuid, rank, name));
            }
        } catch (Exception ex) {
            log.error("Failed to load clan members for {}", clan.getId(), ex).submit();
        }

        return members;
    }

    public void updateClanMemberRank(Clan clan, ClanMember member) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.update(CLAN_MEMBERS)
                    .set(CLAN_MEMBERS.RANK, member.getRank().name())
                    .where(CLAN_MEMBERS.CLAN.eq(clan.getId()))
                    .and(CLAN_MEMBERS.MEMBER.eq(member.getUuid()))
                    .execute();
        });
    }

    //endregion

    //region Clan alliances
    public void saveClanAlliance(IClan clan, ClanAlliance alliance) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.insertInto(CLAN_ALLIANCES)
                        .set(CLAN_ALLIANCES.CLAN, clan.getId())
                        .set(CLAN_ALLIANCES.ALLY_CLAN, alliance.getClan().getId())
                        .set(CLAN_ALLIANCES.TRUSTED, alliance.isTrusted() ? 1 : 0)
                        .execute());
    }

    public void deleteClanAlliance(IClan clan, ClanAlliance alliance) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.deleteFrom(CLAN_ALLIANCES)
                        .where(CLAN_ALLIANCES.CLAN.eq(clan.getId()))
                        .and(CLAN_ALLIANCES.ALLY_CLAN.eq(alliance.getClan().getId()))
                        .execute());
    }

    public List<ClanAlliance> getAlliances(ClanManager clanManager, Clan clan) {
        List<ClanAlliance> alliances = new ArrayList<>();

        try {
            var records = database.getDslContext()
                    .selectFrom(CLAN_ALLIANCES)
                    .where(CLAN_ALLIANCES.CLAN.eq(clan.getId()))
                    .fetch();

            for (var alliancesRecord : records) {
                long allyId = alliancesRecord.get(CLAN_ALLIANCES.ALLY_CLAN);
                var otherClan = clanManager.getClanById(allyId);

                if (otherClan.isPresent()) {
                    boolean trusted = alliancesRecord.get(CLAN_ALLIANCES.TRUSTED) != 0;
                    alliances.add(new ClanAlliance(otherClan.get(), trusted));
                } else {
                    log.warn("Could not find clan with id {}", allyId).submit();
                }
            }
        } catch (Exception ex) {
            log.error("Failed to load clan alliances for {}", clan.getId(), ex).submit();
        }

        return alliances;
    }

    public void saveTrust(IClan clan, ClanAlliance alliance) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.update(CLAN_ALLIANCES)
                        .set(CLAN_ALLIANCES.TRUSTED, alliance.isTrusted() ? 1 : 0)
                        .where(CLAN_ALLIANCES.CLAN.eq(clan.getId()))
                        .and(CLAN_ALLIANCES.ALLY_CLAN.eq(alliance.getClan().getId()))
                        .execute());
    }
    //endregion

    //region Clan enemies
    public void saveClanEnemy(IClan clan, ClanEnemy enemy) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.insertInto(CLAN_ENEMIES)
                        .set(CLAN_ENEMIES.CLAN, clan.getId())
                        .set(CLAN_ENEMIES.ENEMY_CLAN, enemy.getClan().getId())
                        .set(CLAN_ENEMIES.DOMINANCE, (int) enemy.getDominance())
                        .execute());
    }

    public void deleteClanEnemy(IClan clan, ClanEnemy enemy) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.deleteFrom(CLAN_ENEMIES)
                        .where(CLAN_ENEMIES.CLAN.eq(clan.getId()))
                        .and(CLAN_ENEMIES.ENEMY_CLAN.eq(enemy.getClan().getId()))
                        .execute());
    }

    public void updateDominance(IClan clan, ClanEnemy enemy) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.update(CLAN_ENEMIES)
                        .set(CLAN_ENEMIES.DOMINANCE, (int) enemy.getDominance())
                        .where(CLAN_ENEMIES.CLAN.eq(clan.getId()))
                        .and(CLAN_ENEMIES.ENEMY_CLAN.eq(enemy.getClan().getId()))
                        .execute());
    }

    public List<ClanEnemy> getEnemies(ClanManager clanManager, Clan clan) {
        List<ClanEnemy> enemies = new ArrayList<>();

        try {
            var records = database.getDslContext()
                    .selectFrom(CLAN_ENEMIES)
                    .where(CLAN_ENEMIES.CLAN.eq(clan.getId()))
                    .fetch();

            for (var enemiesRecord : records) {
                long enemyId = enemiesRecord.get(CLAN_ENEMIES.ENEMY_CLAN);
                var otherClan = clanManager.getClanById(enemyId);

                if (otherClan.isPresent()) {
                    int dominance = enemiesRecord.get(CLAN_ENEMIES.DOMINANCE);
                    enemies.add(new ClanEnemy(otherClan.get(), dominance));
                }
            }
        } catch (Exception ex) {
            log.error("Failed to load clan enemies for {}", clan.getId(), ex).submit();
        }

        return enemies;
    }
//endregion

    public Map<Integer, Double> getDominanceScale() {
        HashMap<Integer, Double> dominanceScale = new HashMap<>();

        try {
            database.getDslContext()
                    .selectFrom(CLANS_DOMINANCE_SCALE)
                    .fetch()
                    .forEach(dominanceScaleRecord -> dominanceScale.put(
                            dominanceScaleRecord.get(CLANS_DOMINANCE_SCALE.CLAN_SIZE),
                            dominanceScaleRecord.get(CLANS_DOMINANCE_SCALE.DOMINANCE)
                    ));
        } catch (Exception ex) {
            log.error("Failed to load dominance scale", ex).submit();
        }

        return dominanceScale;
    }

    //region Insurance
    public void deleteExpiredInsurance(long duration) {
        long currentTime = System.currentTimeMillis();

        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.deleteFrom(CLAN_INSURANCE)
                    .where(CLAN_INSURANCE.TIME.add(duration).le(currentTime))
                    .execute();
        });
    }

    public void deleteInsuranceForClan(Clan clan) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.deleteFrom(CLAN_INSURANCE)
                    .where(CLAN_INSURANCE.CLAN.eq(clan.getId()))
                    .execute();
        });
    }

    public void saveInsurance(Clan clan, Insurance insurance) {
        Location location = insurance.getBlockLocation();

        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.insertInto(CLAN_INSURANCE)
                        .set(CLAN_INSURANCE.CLAN, clan.getId())
                        .set(CLAN_INSURANCE.INSURANCE_TYPE, insurance.getInsuranceType().name())
                        .set(CLAN_INSURANCE.MATERIAL, insurance.getBlockMaterial().name())
                        .set(CLAN_INSURANCE.DATA, insurance.getBlockData())
                        .set(CLAN_INSURANCE.TIME, insurance.getTime())
                        .set(CLAN_INSURANCE.X, location.getBlockX())
                        .set(CLAN_INSURANCE.Y, location.getBlockY())
                        .set(CLAN_INSURANCE.Z, location.getBlockZ())
                        .execute());
    }

    public List<Insurance> getInsurance(Clan clan) {
        World world = Bukkit.getWorld(BPvPWorld.MAIN_WORLD_NAME);
        List<Insurance> insurance = Collections.synchronizedList(new ArrayList<>());

        try {
            database.getDslContext()
                    .selectFrom(CLAN_INSURANCE)
                    .where(CLAN_INSURANCE.CLAN.eq(clan.getId()))
                    .orderBy(CLAN_INSURANCE.TIME.asc())
                    .fetch()
                    .forEach(insuranceRecord -> {
                        InsuranceType insuranceType = InsuranceType.valueOf(insuranceRecord.get(CLAN_INSURANCE.INSURANCE_TYPE));
                        Material material = Material.valueOf(insuranceRecord.get(CLAN_INSURANCE.MATERIAL));
                        String blockData = insuranceRecord.get(CLAN_INSURANCE.DATA);
                        long time = insuranceRecord.get(CLAN_INSURANCE.TIME);
                        int blockX = insuranceRecord.get(CLAN_INSURANCE.X);
                        int blockY = insuranceRecord.get(CLAN_INSURANCE.Y);
                        int blockZ = insuranceRecord.get(CLAN_INSURANCE.Z);

                        Location blockLocation = new Location(world, blockX, blockY, blockZ);
                        insurance.add(new Insurance(time, material, blockData, insuranceType, blockLocation));
                    });
        } catch (Exception ex) {
            log.error("Failed to load insurance for {}", clan.getId(), ex).submit();
        }

        return insurance;
    }
//endregion

    public List<UUID> getPlayersByClan(long clanID) {
        List<UUID> playerIDs = new ArrayList<>();

        List<CachedLog> logs = logRepository.getLogsWithContextAndAction(LogContext.CLAN, clanID + "", "CLAN_");
        logs.removeIf(cachedLog -> !cachedLog.getAction().equalsIgnoreCase("CLAN_CREATE")
                && !cachedLog.getAction().equalsIgnoreCase("CLAN_JOIN"));

        logs.forEach(cachedLog -> {
            String playerID = cachedLog.getContext().get(LogContext.CLIENT);
            playerIDs.add(UUID.fromString(playerID));
        });

        return playerIDs;
    }

    public Map<Long, String> getClansByPlayer(UUID playerID) {
        Map<Long, String> clans = new HashMap<>();

        List<CachedLog> logs = logRepository.getLogsWithContextAndAction(LogContext.CLIENT, playerID.toString(), "CLAN_");
        logs.removeIf(cachedLog -> !cachedLog.getAction().equalsIgnoreCase("CLAN_CREATE")
                && !cachedLog.getAction().equalsIgnoreCase("CLAN_JOIN"));

        logs.forEach(cachedLog -> {
            String clanName = cachedLog.getContext().get(LogContext.CLAN_NAME);
            String clanID = cachedLog.getContext().get(LogContext.CLAN);
            clans.put(Long.parseLong(clanID), clanName);
        });

        return clans;
    }

    public void addClanKill(long killID, @Nullable Clan killerClan, @Nullable Clan victimClan, double dominance) {
        database.getAsyncDslContext()
                .executeAsyncVoid(ctx -> ctx.insertInto(CLANS_KILLS)
                        .set(CLANS_KILLS.KILL_ID, killID)
                        .set(CLANS_KILLS.KILLER_CLAN, killerClan != null ? killerClan.getId() : null)
                        .set(CLANS_KILLS.VICTIM_CLAN, victimClan != null ? victimClan.getId() : null)
                        .set(CLANS_KILLS.DOMINANCE, dominance)
                        .execute());
    }


    /**
     * Retrieves a list of kill logs associated with a specific clan.
     * The logs include details about killings such as killer, victim, their respective clans, dominance, and the timestamp.
     *
     * @param clan        the clan for which the kill logs are to be retrieved
     * @param clanManager the ClanManager instance used to fetch clan details and names
     * @return a list of {@code KillClanLog} objects containing detailed information about the clan's kill history
     */
    public List<KillClanLog> getClanKillLogs(Clan clan, ClanManager clanManager) {

        List<KillClanLog> killLogs = Collections.synchronizedList(new ArrayList<>());

        try {
            Result<GetClanKillLogsRecord> killLogsRecords = GET_CLAN_KILL_LOGS(database.getDslContext().configuration(), clan.getId());

            killLogsRecords.forEach(killLogRecord -> {
                long killerId = killLogRecord.getKiller();
                String killerName = killLogRecord.getKillerName();
                long killerClanId = killLogRecord.getKillerClan();

                Optional<Clan> killerClan = clanManager.getClanById(killerClanId);
                String killerClanName = killerClan
                        .map(Clan::getName)
                        .orElse("");

                long victimId = killLogRecord.getVictim();
                String victimName = killLogRecord.getVictimName();
                long victimClanId = killLogRecord.getVictimClan();

                Optional<Clan> victimClan = clanManager.getClanById(victimClanId);
                String victimClanName = victimClan
                        .map(Clan::getName)
                        .orElse("");

                double dominance = killLogRecord.getDominance();
                long time = killLogRecord.getTimeVal();

                killLogs.add(new KillClanLog(killerName, killerId, killerClanName, killerClan.orElse(null),
                        victimName, victimId, victimClanName, victimClan.orElse(null),
                        dominance, time));
            });


        } catch (DataAccessException ex) {
            log.error("Failed to get ClanUUID logs", ex).submit();
        }

        return killLogs;
    }
}
