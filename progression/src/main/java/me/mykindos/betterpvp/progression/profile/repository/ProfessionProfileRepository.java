package me.mykindos.betterpvp.progression.profile.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.mappers.PropertyMapper;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.DoubleStatementValue;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.query.values.UuidStatementValue;
import me.mykindos.betterpvp.progression.profession.skill.ProgressionSkillManager;
import me.mykindos.betterpvp.progression.profession.skill.builds.ProgressionBuild;
import me.mykindos.betterpvp.progression.profile.ProfessionData;
import me.mykindos.betterpvp.progression.profile.ProfessionProfile;

import javax.sql.rowset.CachedRowSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Singleton
@CustomLog
public class ProfessionProfileRepository {

    private final Database database;
    private final ProgressionSkillManager skillManager;
    private final PropertyMapper propertyMapper;
    private final ConcurrentHashMap<String, Statement> queuedStatUpdates;
    private final ConcurrentHashMap<String, Statement> queuedExpUpdates;

    @Inject
    public ProfessionProfileRepository(Database database, ProgressionSkillManager skillManager, PropertyMapper propertyMapper) {
        this.database = database;
        this.skillManager = skillManager;
        this.propertyMapper = propertyMapper;
        this.queuedStatUpdates = new ConcurrentHashMap<>();
        this.queuedExpUpdates = new ConcurrentHashMap<>();
    }

    public ProfessionProfile loadProfileForGamer(UUID uuid) {
        ProfessionProfile profile = new ProfessionProfile(uuid);

        loadExperience(profile);
        loadBuilds(profile);

        return profile;
    }

    public void saveExperience(UUID gamer, String profession, double experience) {
        String saveExperienceQuery = "INSERT INTO progression_exp (Gamer, Profession, Experience) VALUES (?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE Experience = ?";
        Statement statement = new Statement(saveExperienceQuery,
                new UuidStatementValue(gamer),
                new StringStatementValue(profession),
                new DoubleStatementValue(experience),
                new DoubleStatementValue(experience));
        queuedExpUpdates.put(gamer + profession, statement);
    }

    private void loadExperience(ProfessionProfile profile) {
        String experienceQuery = "SELECT * FROM progression_exp WHERE Gamer = ?";
        Statement experienceStatement = new Statement(experienceQuery, new UuidStatementValue(profile.getGamerUUID()));

        try (CachedRowSet result = database.executeQuery(experienceStatement)) {
            while (result.next()) {
                String profession = result.getString(2);
                double experience = result.getDouble(3);

                ProfessionData professionData = profile.getProfessionDataMap().computeIfAbsent(profession, key -> new ProfessionData(profile.getGamerUUID(), profession));
                professionData.setExperience(experience);

                loadProperties(profile, professionData);

            }
        } catch (Exception ex) {
            log.error("Failed to load progression experience for {}", profile.getGamerUUID(), ex).submit();
        }
    }

    private void loadProperties(ProfessionProfile profile, ProfessionData data) {
        String query = "SELECT * FROM progression_properties WHERE Gamer = ? AND Profession = ?";
        Statement statement = new Statement(query, new UuidStatementValue(profile.getGamerUUID()), new StringStatementValue(data.getProfession()));

        try (CachedRowSet result = database.executeQuery(statement)) {
            while (result.next()) {
                String property = result.getString(3);
                String value = result.getString(4);

                propertyMapper.parseProperty(property, value, data);
            }
        } catch (Exception ex) {
            log.error("Failed to load progression properties for {}", profile.getGamerUUID()).submit();
        }
    }


    private void loadBuilds(ProfessionProfile profile) {

        String query = "SELECT * FROM progression_builds WHERE Gamer = ?";
        Statement statement = new Statement(query, new UuidStatementValue(profile.getGamerUUID()));

        Map<String, ProfessionData> professionDataMap = profile.getProfessionDataMap();

        try (CachedRowSet result = database.executeQuery(statement)) {
            while (result.next()) {
                String profession = result.getString(2);
                String skillName = result.getString(3);
                int level = result.getInt(4);

                ProfessionData professionData = professionDataMap.computeIfAbsent(profession, k -> new ProfessionData(profile.getGamerUUID(), profession));
                ProgressionBuild build = professionData.getBuild();

                skillManager.getSkill(skillName).ifPresent(skill -> build.getSkills().put(skill, level));
            }
        } catch (Exception ex) {
            log.error("Failed to load progression builds for {}", profile.getGamerUUID()).submit();
        }

    }

    public void updateBuildForGamer(UUID uuid, ProgressionBuild build) {
        String query = "INSERT INTO progression_builds (Gamer, Profession, Skill, Level) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE Level = ?";
        List<Statement> batch = new ArrayList<>();
        build.getSkills().forEach((skill, level) -> {
            batch.add(new Statement(query, new UuidStatementValue(uuid),
                    new StringStatementValue(build.getProfession()),
                    new StringStatementValue(skill.getName()),
                    new IntegerStatementValue(level),
                    new IntegerStatementValue(level)));
        });

        database.executeBatch(batch, true);

    }

    public void saveProperty(UUID gamer, String profession, String property, Object value) {
        String savePropertyQuery = "INSERT INTO progression_properties (Gamer, Profession, Property, Value) VALUES (?, ?, ?, ?)"
                + " ON DUPLICATE KEY UPDATE Value = ?";

        Statement statement = new Statement(savePropertyQuery,
                new UuidStatementValue(gamer),
                new StringStatementValue(profession),
                new StringStatementValue(property),
                new StringStatementValue(value.toString()),
                new StringStatementValue(value.toString()));
        queuedStatUpdates.put(gamer + property, statement);
    }

    public void processStatUpdates(boolean async) {
        ConcurrentHashMap<String, Statement> statements = new ConcurrentHashMap<>(queuedStatUpdates);
        queuedStatUpdates.clear();
        database.executeBatch(statements.values().stream().toList(), async);
        log.info("Updated gamer profession stats with {} queries", statements.size()).submit();

        statements = new ConcurrentHashMap<>(queuedExpUpdates);
        queuedExpUpdates.clear();
        database.executeBatch(statements.values().stream().toList(), async);
        log.info("Updated gamer profession experience with {} queries", statements.size()).submit();

    }
}
