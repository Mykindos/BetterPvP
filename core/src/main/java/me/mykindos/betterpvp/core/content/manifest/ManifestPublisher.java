package me.mykindos.betterpvp.core.content.manifest;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.client.Rank;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.quest.primitive.QuestPrimitive;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.ServerLoadEvent;
import org.jooq.DSLContext;
import org.jooq.Field;
import org.jooq.InsertOnDuplicateSetMoreStep;
import org.jooq.JSONB;
import org.jooq.Query;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.jooq.impl.DSL.*;

/**
 * Publishes the game's manifest (items/zones/npcs/professions/primitives) to the
 * DB on boot — code is the source of truth. It upserts contributed rows and
 * prunes rows that are no longer contributed, so removing e.g. an item or NPC
 * type from code removes it from the DB on the next boot. Runs once, shortly
 * after startup, so all modules have registered their content.
 */
@Singleton
@BPvPListener
@CustomLog
public class ManifestPublisher implements Listener {

    private final Core core;
    private final ClientManager clientManager;
    private final Database database;
    private final Gson gson = new Gson();

    @Inject
    public ManifestPublisher(Core core, ClientManager clientManager, Database database) {
        this.core = core;
        this.clientManager = clientManager;
        this.database = database;
    }

    @EventHandler
    public void onServerStart(final ServerLoadEvent ignored) {
        UtilServer.runTaskLater(core, this::publish, 1500L);
    }

    /**
     * Collect the manifest on the main thread (listeners run sync), then write it
     * to the DB off-thread. Safe to call manually to re-publish on demand.
     */
    public void publish() {
        final ManifestCollectEvent event = UtilServer.callEvent(new ManifestCollectEvent());
        UtilServer.runTaskAsync(core, () -> writeManifest(event));
    }

    private void writeManifest(ManifestCollectEvent event) {
        Component text = Component.text("Game manifest was published successfully.", NamedTextColor.GREEN);
        clientManager.sendMessageToRank("Core",
                Component.text("Publishing game manifest...", NamedTextColor.YELLOW),
                Rank.ADMIN);

        try {
            DSLContext dsl = database.getDslContext();
            upsertItems(dsl, event.getItems());
            upsertZones(dsl, event.getZones());
            upsertSimple(dsl, "game_professions", event.getProfessions(), List.of("display_name", "max_level"));
            upsertPrimitives(dsl, event.getPrimitives());
            rebuildNpcFactories(dsl, event.getNpcFactories());
            log.info("Published manifest: {} items, {} zones, {} professions, {} primitives, {} npc-factory types",
                    event.getItems().size(), event.getZones().size(), event.getProfessions().size(),
                    event.getPrimitives().size(), event.getNpcFactories().size()).submit();
        } catch (Exception ex) {
            log.error("Failed to publish game manifest", ex).submit();
            text = Component.text("Failed to publish game manifest! Check console for details.", NamedTextColor.RED);
        } finally {
            clientManager.sendMessageToRank("Core",
                    text,
                    Rank.ADMIN);
        }
    }

    @SuppressWarnings("unchecked")
    private void upsertItems(DSLContext dsl, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return;
        final List<Query> batch = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            String[] tags = ((List<String>) r.get("tags")).toArray(new String[0]);
            final InsertOnDuplicateSetMoreStep<Record> record = dsl.insertInto(table(name("game_items")),
                            field(name("key"), String.class), field(name("display_name"), String.class),
                            field(name("source"), String.class), field(name("material"), String.class),
                            field(name("tags"), String[].class))
                    .values((String) r.get("key"), (String) r.get("display_name"),
                            (String) r.get("source"), (String) r.get("material"), tags)
                    .onConflict(field(name("key"), String.class))
                    .doUpdate()
                    .set(field(name("display_name"), String.class), (String) r.get("display_name"))
                    .set(field(name("source"), String.class), (String) r.get("source"))
                    .set(field(name("material"), String.class), (String) r.get("material"))
                    .set(field(name("tags"), String[].class), tags);
            batch.add(record);
        }

        dsl.transaction(configuration -> {
            final DSLContext txn = DSL.using(configuration);
            txn.batch(batch).execute();
            prune(txn, "game_items", keys(rows));
        });
    }

    @SuppressWarnings("unchecked")
    private void upsertZones(DSLContext dsl, List<Map<String, Object>> rows) {
        if (rows.isEmpty()) return;
        for (Map<String, Object> r : rows) {
            String[] tags = ((List<String>) r.get("tags")).toArray(new String[0]);
            dsl.insertInto(table(name("game_zones")),
                            field(name("key"), String.class), field(name("display_name"), String.class),
                            field(name("world"), String.class), field(name("tags"), String[].class))
                    .values((String) r.get("key"), (String) r.get("display_name"), (String) r.get("world"), tags)
                    .onConflict(field(name("key"), String.class))
                    .doUpdate()
                    .set(field(name("display_name"), String.class), (String) r.get("display_name"))
                    .set(field(name("world"), String.class), (String) r.get("world"))
                    .set(field(name("tags"), String[].class), tags)
                    .execute();
        }
        prune(dsl, "game_zones", keys(rows));
    }

    /** Upsert tables whose non-key columns are plain scalar values. */
    private void upsertSimple(DSLContext dsl, String tableName, List<Map<String, Object>> rows, List<String> columns) {
        if (rows.isEmpty()) return;
        for (Map<String, Object> r : rows) {
            Map<Field<?>, Object> values = new LinkedHashMap<>();
            values.put(field(name("key")), r.get("key"));
            Map<Field<?>, Object> updates = new LinkedHashMap<>();
            for (String col : columns) {
                values.put(field(name(col)), r.get(col));
                updates.put(field(name(col)), r.get(col));
            }
            dsl.insertInto(table(name(tableName)))
                    .set(values)
                    .onConflict(field(name("key"), String.class))
                    .doUpdate()
                    .set(updates)
                    .execute();
        }
        prune(dsl, tableName, keys(rows));
    }

    private void upsertPrimitives(DSLContext dsl, List<QuestPrimitive> primitives) {
        if (primitives.isEmpty()) return;
        for (QuestPrimitive p : primitives) {
            JSONB schema = JSONB.valueOf(gson.toJson(p.getParamSchema()));
            dsl.insertInto(table(name("quest_primitives")),
                            field(name("id"), String.class), field(name("category"), String.class),
                            field(name("label"), String.class), field(name("param_schema"), JSONB.class))
                    .values(p.getId(), p.getCategory(), p.getLabel(), schema)
                    .onConflict(field(name("id"), String.class))
                    .doUpdate()
                    .set(field(name("category"), String.class), p.getCategory())
                    .set(field(name("label"), String.class), p.getLabel())
                    .set(field(name("param_schema"), JSONB.class), schema)
                    .execute();
        }
        List<String> ids = primitives.stream().map(QuestPrimitive::getId).collect(Collectors.toList());
        prune(dsl, "quest_primitives", ids);
    }

    /** Full rebuild of the factory manifest (composite key, purely code-derived). */
    private void rebuildNpcFactories(DSLContext dsl, List<Map<String, Object>> rows) {
        dsl.deleteFrom(table(name("game_npc_factories"))).execute();
        for (Map<String, Object> r : rows) {
            dsl.insertInto(table(name("game_npc_factories")),
                            field(name("factory"), String.class), field(name("type"), String.class))
                    .values((String) r.get("factory"), (String) r.get("type"))
                    .onConflict(field(name("factory"), String.class), field(name("type"), String.class))
                    .doNothing()
                    .execute();
        }
    }

    private List<String> keys(List<Map<String, Object>> rows) {
        return rows.stream().map(r -> (String) r.get("key")).collect(Collectors.toList());
    }

    /** Delete rows whose key/id is no longer contributed by code. */
    private void prune(DSLContext dsl, String tableName, List<String> presentKeys) {
        if (presentKeys.isEmpty()) return;
        String keyColumn = tableName.equals("quest_primitives") ? "id" : "key";
        dsl.deleteFrom(table(name(tableName)))
                .where(field(name(keyColumn), String.class).notIn(presentKeys))
                .execute();
    }
}
