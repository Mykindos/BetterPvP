package me.mykindos.betterpvp.core.npc;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerPriority;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.npc.listener.PlayerListPacketController;
import me.mykindos.betterpvp.core.npc.model.NPC;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Singleton
public final class NPCRegistry {

    private final Map<Integer, NPC> objects = new HashMap<>();

    @Inject
    private NPCRegistry() {
        PacketEvents.getAPI().getEventManager().registerListener(new PlayerListPacketController(this), PacketListenerPriority.NORMAL);
    }

    public void unregister(@NotNull NPC npc) {
        this.objects.remove(npc.getId());
    }

    public void register(@NotNull NPC npc) {
        npc.getEntity().getPersistentDataContainer().set(CoreNamespaceKeys.NPC, PersistentDataType.BOOLEAN, true);
        this.objects.put(npc.getId(), npc);
    }

    public Collection<NPC> getNPCs() {
        return Collections.unmodifiableCollection(this.objects.values());
    }

    public NPC getNPC(int id) {
        return this.objects.get(id);
    }

    public NPC getNPC(Entity entity) {
        return getNPC(entity.getUniqueId());
    }

    public NPC getNPC(UUID uuid) {
        return this.objects.values().stream()
                .filter(npc -> npc.getEntity().getUniqueId().equals(uuid))
                .findFirst()
                .orElse(null);
    }

}
