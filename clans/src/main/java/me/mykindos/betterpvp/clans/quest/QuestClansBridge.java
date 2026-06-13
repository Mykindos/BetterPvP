package me.mykindos.betterpvp.clans.quest;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.clans.ClanManager;
import me.mykindos.betterpvp.clans.world.resource.event.ResourceHarvestEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.quest.QuestManager;
import me.mykindos.betterpvp.core.quest.ScopeResolver;
import me.mykindos.betterpvp.core.quest.model.PrimitiveData;
import me.mykindos.betterpvp.core.quest.primitive.QuestPrimitiveHandlers;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

/**
 * Wires clans into the quest engine: registers the clan-level condition core
 * can't evaluate and supplies the clan scope resolver (so clan-scoped quests
 * resolve to a clan participant). Leaf-module extension pattern — core stays
 * unaware of clans.
 */
@Singleton
@BPvPListener
public class QuestClansBridge implements Listener {

    private final ClanManager clanManager;
    private final QuestManager questManager;

    @Inject
    public QuestClansBridge(ClanManager clanManager, QuestManager questManager,
                            QuestPrimitiveHandlers handlers, ScopeResolver scopeResolver) {
        this.clanManager = clanManager;
        this.questManager = questManager;

        handlers.registerCondition("condition.clan_level", this::hasClanLevel);
        handlers.registerCondition("requirement.clan_level", this::hasClanLevel);

        // Clan / alliance scoped quests resolve to the player's clan id.
        scopeResolver.setClanIdResolver(uuid ->
                clanManager.getClanByPlayer(uuid).map(clan -> String.valueOf(clan.getId())));
    }

    @EventHandler
    public void onHarvest(ResourceHarvestEvent event) {
        final String profession = event.getProfession();
        questManager.recordEvent(event.getPlayer(), "trigger.harvest", data -> {
            String configured = data.getString("profession");
            return configured == null || configured.isBlank()
                    || (profession != null && configured.equalsIgnoreCase(profession));
        }, 1);
    }

    private boolean hasClanLevel(Player player, PrimitiveData data) {
        int required = data.getInt("level", 1);
        return clanManager.getClanByPlayer(player.getUniqueId())
                .map(clan -> clan.getLevel() >= required)
                .orElse(false);
    }
}
