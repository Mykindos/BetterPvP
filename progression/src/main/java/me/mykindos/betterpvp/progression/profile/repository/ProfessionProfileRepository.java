package me.mykindos.betterpvp.progression.profile.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.mappers.PropertyMapper;
import me.mykindos.betterpvp.progression.database.jooq.tables.records.ProgressionBuildsRecord;
import me.mykindos.betterpvp.progression.database.jooq.tables.records.ProgressionExpRecord;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.ProgressionBuild;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.jooq.Record2;
import org.jooq.Result;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import static me.mykindos.betterpvp.progression.database.jooq.Tables.PROGRESSION_BUILDS;
import static me.mykindos.betterpvp.progression.database.jooq.Tables.PROGRESSION_EXP;
import static me.mykindos.betterpvp.progression.database.jooq.Tables.PROGRESSION_PROPERTIES;

@Singleton
@CustomLog
public class ProfessionProfileRepository {

    private final Database database;
    private final ClientManager clientManager;
    private final ProgressionSkillManager skillManager;
    private final PropertyMapper propertyMapper;
    private final AtomicReference<ConcurrentHashMap<String, Query>> queuedStatUpdates =
            new AtomicReference<>(new ConcurrentHashMap<>());
    private final AtomicReference<ConcurrentHashMap<String, Query>> queuedExpUpdates =
            new AtomicReference<>(new ConcurrentHashMap<>());

    @Inject
    public ProfessionProfileRepository(Database database, ClientManager clientManager,
                                       ProgressionSkillManager skillManager, PropertyMapper propertyMapper) {
        this.database = database;
        this.clientManager = clientManager;
        this.skillManager = skillManager;
        this.propertyMapper = propertyMapper;

        createPartitions();
    }

    public void createPartitions() {
        int season = Core.getCurrentRealm().getSeason().getId();
        String partitionTableName = "progression_builds_season_" + season;
        try {
            database.getDslContext().execute(DSL.sql(String.format(
                    "CREATE TABLE IF NOT EXISTS %s PARTITION OF progression_builds FOR VALUES IN (%d)",
                    partitionTableName, season
            )));
            log.info("Created partition {} for season {}", partitionTableName, season).submit();
        } catch (Exception e) {
            log.info("Partition {} may already exist", partitionTableName).submit();
        }
    }

    public ProfessionProfile loadProfileForGamer(UUID uuid) {
        ProfessionProfile profile = new ProfessionProfile(uuid);

        Client client = clientManager.search().online(uuid).orElseThrow();
        loadExperience(client, profile);
        loadBuilds(client, profile);

        return profile;
    }

    public void saveExperience(UUID gamer, String profession, double experience) {
        CompletableFuture.runAsync(() -> {
            Client client = clientManager.search().offline(gamer).join().orElse(null);
            if (client == null) return;

            DSLContext ctx = database.getDslContext();

            org.jooq.Query query = ctx.insertInto(PROGRESSION_EXP)
                    .set(PROGRESSION_EXP.CLIENT, client.getId())
                    .set(PROGRESSION_EXP.SEASON, Core.getCurrentRealm().getSeason().getId())
                    .set(PROGRESSION_EXP.PROFESSION, profession)
                    .set(PROGRESSION_EXP.EXPERIENCE, (long) experience)
                    .onConflict()
                    .doUpdate()
                    .set(PROGRESSION_EXP.EXPERIENCE, (long) experience);

            queuedExpUpdates.get().put(gamer + profession, query);
        });


    }

    private void loadExperience(Client client, ProfessionProfile profile) {

        try {
            DSLContext ctx = database.getDslContext();
            Result<ProgressionExpRecord> results = ctx.selectFrom(PROGRESSION_EXP)
                    .where(PROGRESSION_EXP.CLIENT.eq(client.getId()))
                    .and(PROGRESSION_EXP.SEASON.eq(Core.getCurrentRealm().getSeason().getId()))
                    .fetch();
            results.forEach(result -> {
                String profession = result.getProfession();
                double experience = result.getExperience();

                ProfessionData professionData = profile.getProfessionDataMap().computeIfAbsent(profession, key -> new ProfessionData(profile.getGamerUUID(), profession));
                professionData.setExperience(experience);

                loadProperties(client, profile, professionData);

            });
        } catch (Exception ex) {
            log.error("Failed to load progression experience for {}", profile.getGamerUUID(), ex).submit();
        }
    }

    private void loadProperties(Client client, ProfessionProfile profile, ProfessionData data) {

        try {
            Result<Record2<String, String>> result = database.getDslContext()
                    .select(PROGRESSION_PROPERTIES.PROPERTY, PROGRESSION_PROPERTIES.VALUE)
                    .from(PROGRESSION_PROPERTIES)
                    .where(PROGRESSION_PROPERTIES.CLIENT.eq(client.getId()))
                    .and(PROGRESSION_PROPERTIES.SEASON.eq(Core.getCurrentRealm().getSeason().getId()))
                    .and(PROGRESSION_PROPERTIES.PROFESSION.eq(data.getProfession()))
                    .fetch();

            propertyMapper.parseProperties(result, data);
        } catch (Exception ex) {
            log.error("Failed to load progression properties for {}", profile.getGamerUUID()).submit();
        }
    }


    private void loadBuilds(Client client, ProfessionProfile profile) {
        Map<String, ProfessionData> professionDataMap = profile.getProfessionDataMap();

        try {
            DSLContext ctx = database.getDslContext();
            Result<ProgressionBuildsRecord> results = ctx.selectFrom(PROGRESSION_BUILDS)
                    .where(PROGRESSION_BUILDS.CLIENT.eq(client.getId()))
                    .and(PROGRESSION_BUILDS.SEASON.eq(Core.getCurrentRealm().getSeason().getId())).fetch();

            results.forEach(buildRecord -> {
                String profession = buildRecord.getProfession();
                String skillName = buildRecord.getSkill();
                int level = buildRecord.getLevel();

                ProfessionData professionData = professionDataMap.computeIfAbsent(profession, k -> new ProfessionData(profile.getGamerUUID(), profession));
                ProgressionBuild build = professionData.getBuild();

                skillManager.getSkill(skillName).ifPresent(skill -> build.getSkills().put(skill, level));
            });
        } catch (Exception ex) {
            log.error("Failed to load progression builds for {}", profile.getGamerUUID()).submit();
        }

    }

    public void updateBuildForGamer(UUID uuid, ProgressionBuild build) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            Client client = clientManager.search().offline(uuid).join().orElse(null);
            if (client == null) return;

            List<Query> queries = new ArrayList<>();

            build.getSkills().forEach((skill, level) -> {
                Query query = ctx.insertInto(PROGRESSION_BUILDS)
                        .set(PROGRESSION_BUILDS.CLIENT, client.getId())
                        .set(PROGRESSION_BUILDS.SEASON, Core.getCurrentRealm().getSeason().getId())
                        .set(PROGRESSION_BUILDS.PROFESSION, build.getProfession())
                        .set(PROGRESSION_BUILDS.SKILL, skill.getName())
                        .set(PROGRESSION_BUILDS.LEVEL, level)
                        .onConflict()
                        .doUpdate()
                        .set(PROGRESSION_BUILDS.LEVEL, level);
                queries.add(query);
            });

            if (!queries.isEmpty()) {
                ctx.batch(queries).execute();
            }
        });

    }

    public void saveProperty(UUID gamer, String profession, String property, Object value) {
        CompletableFuture.runAsync(() -> {
            Client client = clientManager.search().offline(gamer).join().orElse(null);
            if (client == null) return;

            DSLContext ctx = database.getDslContext();

            Query query = ctx.insertInto(PROGRESSION_PROPERTIES)
                    .set(PROGRESSION_PROPERTIES.CLIENT, client.getId())
                    .set(PROGRESSION_PROPERTIES.SEASON, Core.getCurrentRealm().getSeason().getId())
                    .set(PROGRESSION_PROPERTIES.PROFESSION, profession)
                    .set(PROGRESSION_PROPERTIES.PROPERTY, property)
                    .set(PROGRESSION_PROPERTIES.VALUE, value.toString())
                    .onConflict()
                    .doUpdate()
                    .set(PROGRESSION_PROPERTIES.VALUE, value.toString());

            queuedStatUpdates.get().put(gamer + property, query);
        });
    }

    public void processStatUpdates(boolean async) {
        if (async) {
            database.getAsyncDslContext().executeAsyncVoid(this::performStatUpdates);
        } else {
            performStatUpdates(database.getDslContext());
        }
    }

    private void performStatUpdates(DSLContext ctx) {
        ConcurrentHashMap<String, Query> statUpdates = queuedStatUpdates.getAndSet(new ConcurrentHashMap<>());
        ConcurrentHashMap<String, Query> expUpdates = queuedExpUpdates.getAndSet(new ConcurrentHashMap<>());

        if (!statUpdates.isEmpty()) {
            ctx.batch(statUpdates.values().stream().toList()).execute();
            log.info("Updated stats with {} queries", statUpdates.size()).submit();
        }
        if (!expUpdates.isEmpty()) {
            ctx.batch(expUpdates.values().stream().toList()).execute();
            log.info("Updated experience with {} queries", expUpdates.size()).submit();
        }
    }

}
