package me.mykindos.betterpvp.champions.stats.repository.weapon;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.combat.stats.model.CombatData;
import me.mykindos.betterpvp.core.combat.stats.model.IAttachmentLoader;
import me.mykindos.betterpvp.core.database.Database;
import me.mykindos.betterpvp.core.items.ItemHandler;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

@Singleton
public class ChampionItemDataAttachmentLoader implements IAttachmentLoader<ChampionItemDataAttachment> {
    private final ItemHandler itemHandler;
    private final Set<String> allowedIdentifiers;

    @Inject
    public ChampionItemDataAttachmentLoader(ItemHandler itemHandler) {
        this.itemHandler = itemHandler;
        this.allowedIdentifiers = getAllowedIdentifiers();

    }

    private Set<String> getAllowedIdentifiers() {
        Champions champions = JavaPlugin.getPlugin(Champions.class);

        Set<String> identifiers = new HashSet<>(
                champions.getConfig().getOrSaveStringList("combat.log.items",
                        List.of(
                            "champions:alligators_tooth",
                            "champions:giants_broadsword",
                            "champions:hyper_axe",
                            "champions:magnetic_maul",
                            "champions:thunderclap_aegis",
                            "champions:wind_blade",
                            "champions:ancient_sword",
                            "champions:ancient_axe",
                            "champions:power_sword",
                            "champions:power_axe",
                            "champions:standard_sword",
                            "champions:standard_axe",
                            "champions:basic_sword",
                            "champions:basic_axe",
                            "champions:bow",
                            "champions:crossbow"
                            )
                )
        );
        champions.saveConfig();

        return identifiers;
    }

    /**
     * Loads an attachment for the given {@link CombatData} object.
     *
     * @param player   The UUID of the player
     * @param data     The combat data
     * @param database The database
     * @return The attachment
     */
    @Override
    public @NotNull ChampionItemDataAttachment loadAttachment(@NotNull UUID player, @NotNull CombatData data, @NotNull Database database) {
        return new ChampionItemDataAttachment(itemHandler, allowedIdentifiers);
    }
}
