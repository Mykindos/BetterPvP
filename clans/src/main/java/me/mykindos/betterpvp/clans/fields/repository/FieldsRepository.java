package me.mykindos.betterpvp.clans.fields.repository;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Getter;
import lombok.SneakyThrows;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.fields.Fields;
import me.mykindos.betterpvp.clans.fields.block.SimpleOre;
import me.mykindos.betterpvp.clans.fields.model.FieldsInteractable;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.database.connection.TargetDatabase;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.database.query.values.IntegerStatementValue;
import me.mykindos.betterpvp.core.database.query.values.StringStatementValue;
import me.mykindos.betterpvp.core.database.repository.IRepository;
import org.jetbrains.annotations.NotNull;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Singleton
@CustomLog
public class FieldsRepository implements IRepository<FieldsBlockEntry> {

    @Getter
    private final Set<FieldsInteractable> types = new HashSet<>();
    private final Database database;

    @Inject
    public FieldsRepository(Clans clans, Database database) {
        this.database = database;

        Reflections reflections = new Reflections(Fields.class.getPackageName());
        Set<Class<? extends FieldsInteractable>> classes = reflections.getSubTypesOf(FieldsInteractable.class);
        for (var clazz : classes) {
            if (clazz.isInterface() || Modifier.isAbstract(clazz.getModifiers()) || clazz.isEnum()) continue;
            if (clazz.isAnnotationPresent(Deprecated.class)) continue;
            FieldsInteractable interactable = clans.getInjector().getInstance(clazz);
            clans.getInjector().injectMembers(interactable);
            types.add(interactable);
        }
        types.addAll(List.of(SimpleOre.values()));
        log.info("Loaded " + types.size() + " ore types").submit();
    }

    @Override
    public List<FieldsBlockEntry> getAll() {
        List<FieldsBlockEntry> ores = new ArrayList<>();
        String query = "SELECT * FROM clans_fields_ores WHERE Server = ? AND Season = ?;";

        try (ResultSet result = database.executeQuery(new Statement(query,
                new StringStatementValue(Core.getCurrentServer()),
                new StringStatementValue(Core.getCurrentSeason())
        ), TargetDatabase.GLOBAL).join()) {
            while (result.next()) {
                final String world = result.getString("World");
                final int x = result.getInt("X");
                final int y = result.getInt("Y");
                final int z = result.getInt("Z");
                final String typeName = result.getString("Type");
                final String blockData = result.getString("Data");
                FieldsInteractable type = types.stream()
                        .filter(t -> t.getName().equalsIgnoreCase(typeName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unknown block type: " + typeName));

                ores.add(new FieldsBlockEntry(type, world, x, y, z, blockData == null ? "" : blockData));
            }
        } catch (SQLException | IllegalStateException ex) {
            log.error("Failed to load fields ores", ex).submit();
        }

        return ores;
    }

    public void delete(String world, int x, int y, int z) {
        String stmt = "DELETE FROM clans_fields_ores WHERE Server = ? AND Season = ? AND World = ? AND X = ? AND Y = ? AND Z = ?;";
        database.executeUpdate(new Statement(stmt,
                new StringStatementValue(Core.getCurrentServer()),
                new StringStatementValue(Core.getCurrentSeason()),
                new StringStatementValue(world),
                new IntegerStatementValue(x),
                new IntegerStatementValue(y),
                new IntegerStatementValue(z)), TargetDatabase.GLOBAL);
    }

    @Override
    public void save(@NotNull FieldsBlockEntry ore) {
        if (ore.getType() == null) {
            delete(ore.getWorld(), ore.getX(), ore.getY(), ore.getZ());
            return;
        }

        String stmt = "INSERT INTO clans_fields_ores (Server, Season, World, X, Y, Z, Type, Data) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
        database.executeUpdate(new Statement(stmt,
                new StringStatementValue(Core.getCurrentServer()),
                new StringStatementValue(Core.getCurrentSeason()),
                new StringStatementValue(ore.getWorld()),
                new IntegerStatementValue(ore.getX()),
                new IntegerStatementValue(ore.getY()),
                new IntegerStatementValue(ore.getZ()),
                new StringStatementValue(ore.getType().getName()),
                new StringStatementValue(ore.getData())), TargetDatabase.GLOBAL);
    }

    @SneakyThrows
    public void saveBatch(@NotNull Collection<@NotNull FieldsBlockEntry> ores) {
        String stmt = "INSERT INTO clans_fields_ores (Server, Season, World, X, Y, Z, Type, Data) VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE type = VALUES(type), data = VALUES(data);";
        List<Statement> statements = new ArrayList<>();
        for (FieldsBlockEntry ore : ores) {
            if (ore.getType() == null) {
                delete(ore.getWorld(), ore.getX(), ore.getY(), ore.getZ());
                continue;
            }

            Statement statement = new Statement(stmt,
                    new StringStatementValue(Core.getCurrentServer()),
                    new StringStatementValue(Core.getCurrentSeason()),
                    new StringStatementValue(ore.getWorld()),
                    new IntegerStatementValue(ore.getX()),
                    new IntegerStatementValue(ore.getY()),
                    new IntegerStatementValue(ore.getZ()),
                    new StringStatementValue(ore.getType().getName()),
                    new StringStatementValue(ore.getData()));
            statements.add(statement);
        }
        database.executeBatch(statements, TargetDatabase.GLOBAL);
    }
}
