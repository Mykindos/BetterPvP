package me.mykindos.betterpvp.clans.progression.listener;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.fields.event.FieldsInteractableUseEvent;
import me.mykindos.betterpvp.clans.fields.model.FieldsInteractable;
import me.mykindos.betterpvp.clans.fields.model.FieldsOre;
import me.mykindos.betterpvp.clans.progression.ProgressionAdapter;
import me.mykindos.betterpvp.core.config.ExtendedYamlConfiguration;
import me.mykindos.betterpvp.core.utilities.model.ConfigAccessor;
import me.mykindos.betterpvp.progression.profession.mining.MiningHandler;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

@CustomLog
@Singleton
public class MiningListener implements Listener, ConfigAccessor {

    private double xpMultiplier;
    private final MiningHandler miningHandler;

    @Inject
    public MiningListener(ProgressionAdapter adapter) {
        this.miningHandler = adapter.getProgression().getInjector().getInstance(MiningHandler.class);
    }

    @EventHandler
    public void onFieldsOreMine(FieldsInteractableUseEvent event) {
        final FieldsInteractable type = event.getType();
        if (!(type instanceof FieldsOre)) {
            return;
        }

        final Player player = event.getPlayer();
        final Block block = event.getBlock().getBlock();
        miningHandler.attemptMineOre(player, block, experience -> (long) (experience * xpMultiplier));
    }

    @Override
    public void loadConfig(@NotNull ExtendedYamlConfiguration config) {
        this.xpMultiplier = config.getOrSaveObject("fields.fieldsXpMultiplier", 5d, Double.class);
    }
}
