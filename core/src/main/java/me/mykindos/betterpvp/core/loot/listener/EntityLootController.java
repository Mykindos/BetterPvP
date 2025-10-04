package me.mykindos.betterpvp.core.loot.listener;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.loot.LootBundle;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import me.mykindos.betterpvp.core.loot.session.LootSessionController;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.metadata.FixedMetadataValue;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@BPvPListener
@Singleton
public class EntityLootController implements Listener {

    private final LootSessionController sessionController;

    @Inject
    private EntityLootController(LootSessionController sessionController) {
        this.sessionController = sessionController;
    }

    public static void bind(@NotNull LivingEntity livingEntity, @NotNull LootTable lootTable) {
        final Core plugin = JavaPlugin.getPlugin(Core.class);
        livingEntity.setMetadata("loot-table", new FixedMetadataValue(plugin, lootTable));
    }

    @EventHandler
    void onDeath(EntityDeathEvent event) {
        final LivingEntity entity = event.getEntity();
        if (!entity.hasMetadata("loot-table")) {
            return; // Not a lootable entity
        }

        final Player killer = entity.getKiller();
        entity.getMetadata("loot-table").forEach(metadataValue -> awardLoot(metadataValue, killer, entity));
    }

    private void awardLoot(MetadataValue metadataValue, Player killer, LivingEntity entity) {
        final LootTable lootTable = (LootTable) metadataValue.value();
        Preconditions.checkNotNull(lootTable, "Loot table metadata value is null");
        final LootSession lootSession;
        if (killer == null) {
            lootSession =  sessionController.getGlobalScope(lootTable);
        } else {
            lootSession = sessionController.resolve(killer, lootTable, () -> LootSession.newSession(lootTable, killer));
        }

        final LootContext context = new LootContext(lootSession, entity.getLocation(), entity.getName());
        final LootBundle bundle = lootTable.generateLoot(context);
        bundle.award();
    }

}
