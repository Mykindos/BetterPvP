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
import me.mykindos.betterpvp.core.database.repository.IRepository;
import org.jetbrains.annotations.NotNull;
import org.jooq.DSLContext;
import org.jooq.Query;
import org.reflections.Reflections;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import static me.mykindos.betterpvp.core.database.jooq.Tables.CLANS_FIELDS_ORES;

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
        try {
            var records = database.getDslContext()
                    .selectFrom(CLANS_FIELDS_ORES)
                    .where(CLANS_FIELDS_ORES.REALM.eq(Core.getCurrentRealm()))
                    .fetch();

            for (var fieldsRecord : records) {
                final String world = fieldsRecord.get(CLANS_FIELDS_ORES.WORLD);
                final int x = fieldsRecord.get(CLANS_FIELDS_ORES.X);
                final int y = fieldsRecord.get(CLANS_FIELDS_ORES.Y);
                final int z = fieldsRecord.get(CLANS_FIELDS_ORES.Z);
                final String typeName = fieldsRecord.get(CLANS_FIELDS_ORES.TYPE);
                final String blockData = fieldsRecord.get(CLANS_FIELDS_ORES.DATA);

                FieldsInteractable type = types.stream()
                        .filter(t -> t.getName().equalsIgnoreCase(typeName))
                        .findFirst()
                        .orElseThrow(() -> new IllegalStateException("Unknown block type: " + typeName));

                ores.add(new FieldsBlockEntry(type, world, x, y, z, blockData == null ? "" : blockData));
            }
        } catch (Exception ex) {
            log.error("Failed to load fields ores", ex).submit();
        }

        return ores;
    }

    public void delete(String world, int x, int y, int z) {
        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            ctx.deleteFrom(CLANS_FIELDS_ORES)
                    .where(CLANS_FIELDS_ORES.REALM.eq(Core.getCurrentRealm()))
                    .and(CLANS_FIELDS_ORES.WORLD.eq(world))
                    .and(CLANS_FIELDS_ORES.X.eq(x))
                    .and(CLANS_FIELDS_ORES.Y.eq(y))
                    .and(CLANS_FIELDS_ORES.Z.eq(z))
                    .execute();
        });
    }

    @Override
    public void save(@NotNull FieldsBlockEntry ore) {
        if (ore.getType() == null) {
            delete(ore.getWorld(), ore.getX(), ore.getY(), ore.getZ());
            return;
        }

        database.getAsyncDslContext().executeAsyncVoid(ctx -> {
            getQueryForBlockEntry(ctx, ore).execute();
        });
    }

    @SneakyThrows
    public void saveBatch(@NotNull Collection<@NotNull FieldsBlockEntry> ores) {
        List<Query> statements = new ArrayList<>();

        try {
            DSLContext ctx = database.getDslContext();
            for (FieldsBlockEntry ore : ores) {
                if (ore.getType() == null) {
                    delete(ore.getWorld(), ore.getX(), ore.getY(), ore.getZ());
                    continue;
                }

                statements.add(getQueryForBlockEntry(ctx, ore));
            }

            database.getAsyncDslContext().executeAsyncVoid(actx -> {
                actx.batch(statements).execute();
            });
        } catch (Exception ex) {
            log.error("Failed saving fields batch", ex).submit();
        }
    }

    private Query getQueryForBlockEntry(DSLContext ctx, FieldsBlockEntry entry) {
        return ctx.insertInto(CLANS_FIELDS_ORES)
                .set(CLANS_FIELDS_ORES.REALM, Core.getCurrentRealm())
                .set(CLANS_FIELDS_ORES.WORLD, entry.getWorld())
                .set(CLANS_FIELDS_ORES.X, entry.getX())
                .set(CLANS_FIELDS_ORES.Y, entry.getY())
                .set(CLANS_FIELDS_ORES.Z, entry.getZ())
                .set(CLANS_FIELDS_ORES.TYPE, Objects.requireNonNull(entry.getType()).getName())
                .set(CLANS_FIELDS_ORES.DATA, entry.getData())
                .onDuplicateKeyUpdate()
                .set(CLANS_FIELDS_ORES.TYPE, entry.getType().getName())
                .set(CLANS_FIELDS_ORES.DATA, entry.getData());
    }
}
