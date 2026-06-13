package me.mykindos.betterpvp.core.content.manifest;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.quest.primitive.QuestPrimitiveRegistry;
import me.mykindos.betterpvp.core.scene.SceneObjectFactoryManager;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

import java.util.List;

/**
 * Contributes core-owned manifest data (items, zones, quest primitives) when the
 * manifest is collected. Leaf modules (e.g. progression's professions) add their
 * own contributors the same way, so core never depends on them.
 */
@Singleton
@BPvPListener
public class CoreManifestContributor implements Listener {

    private final ItemRegistry itemRegistry;
    private final ZoneManager zoneManager;
    private final QuestPrimitiveRegistry primitiveRegistry;
    private final SceneObjectFactoryManager factoryManager;

    @Inject
    public CoreManifestContributor(ItemRegistry itemRegistry, ZoneManager zoneManager,
                                   QuestPrimitiveRegistry primitiveRegistry, SceneObjectFactoryManager factoryManager) {
        this.itemRegistry = itemRegistry;
        this.zoneManager = zoneManager;
        this.primitiveRegistry = primitiveRegistry;
        this.factoryManager = factoryManager;
    }

    @EventHandler
    public void onCollect(ManifestCollectEvent event) {
        final PlainTextComponentSerializer plain = PlainTextComponentSerializer.plainText();

        itemRegistry.getItemsSorted().forEach((key, item) -> {
            boolean vanilla = "minecraft".equals(key.getNamespace());
            event.addItem(
                    key.toString(),
                    prettify(key.getKey()),
                    vanilla ? "vanilla" : "custom",
                    vanilla ? key.getKey().toUpperCase() : null,
                    List.of());
        });

        for (Zone zone : zoneManager.getAllZones()) {
            event.addZone(
                    zone.getKey().asString(),
                    plain.serialize(zone.getDisplayName()),
                    null,
                    List.copyOf(zone.getTags()));
        }

        primitiveRegistry.all().forEach(event::addPrimitive);

        factoryManager.getObjects().forEach((factoryName, factory) -> {
            for (String type : factory.getTypes()) {
                event.addNpcFactory(factoryName, type);
            }
        });
    }

    private static String prettify(String key) {
        String[] parts = key.replace('_', ' ').split(" ");
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) continue;
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1)).append(' ');
        }
        return sb.toString().trim();
    }
}
