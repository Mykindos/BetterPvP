package me.mykindos.betterpvp.core.npc;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.framework.CoreNamespaceKeys;
import me.mykindos.betterpvp.core.framework.manager.Manager;
import me.mykindos.betterpvp.core.npc.controller.PlayerListPacketController;
import me.mykindos.betterpvp.core.npc.model.NPC;
import org.bukkit.entity.Entity;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

@Singleton
public final class NPCRegistry extends Manager<NPC> {

    @Inject
    private NPCRegistry() {
        ProtocolLibrary.getProtocolManager().removePacketListener(new PlayerListPacketController(this));
    }

    public void register(@NotNull NPC npc) {
        npc.getEntity().getPersistentDataContainer().set(CoreNamespaceKeys.NPC, PersistentDataType.BOOLEAN, true);
        this.objects.put(Integer.toString(npc.getId()), npc);
    }

    public Collection<NPC> getNPCs() {
        return Collections.unmodifiableCollection(this.objects.values());
    }

    public NPC getNPC(int id) {
        return this.objects.get(Integer.toString(id));
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
