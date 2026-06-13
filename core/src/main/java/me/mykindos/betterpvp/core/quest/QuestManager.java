package me.mykindos.betterpvp.core.quest;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.loot.LootContext;
import me.mykindos.betterpvp.core.loot.LootSource;
import me.mykindos.betterpvp.core.loot.LootTable;
import me.mykindos.betterpvp.core.loot.LootTableRegistry;
import me.mykindos.betterpvp.core.loot.session.LootSession;
import me.mykindos.betterpvp.core.loot.session.LootSessionController;
import me.mykindos.betterpvp.core.quest.event.QuestMilestoneEvent;
import me.mykindos.betterpvp.core.quest.model.PrimitiveData;
import me.mykindos.betterpvp.core.quest.model.QuestDefinition;
import me.mykindos.betterpvp.core.quest.model.QuestNode;
import me.mykindos.betterpvp.core.quest.primitive.QuestPrimitiveHandlers;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.world.zone.Zone;
import me.mykindos.betterpvp.core.world.zone.ZoneManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Predicate;

/**
 * The quest runtime: starts quests (checking requirements), tracks objective
 * progress from gameplay events, advances stages through the graph, and grants
 * rewards on completion. Persists instances to the DB. Core primitive handlers
 * (item/zone/loot/message) are registered here; leaf-module primitives plug in
 * via {@link QuestPrimitiveHandlers}.
 */
@Singleton
@CustomLog
public class QuestManager {

    private final Core core;
    private final QuestRegistry questRegistry;
    private final QuestProgressRepository repository;
    private final ScopeResolver scopeResolver;
    private final QuestPrimitiveHandlers handlers;
    private final ItemRegistry itemRegistry;
    private final ItemFactory itemFactory;
    private final LootTableRegistry lootTableRegistry;
    private final LootSessionController lootSessionController;
    private final ZoneManager zoneManager;

    private final List<QuestInstance> active = new CopyOnWriteArrayList<>();
    private final Set<String> completed = ConcurrentHashMap.newKeySet();

    @Inject
    public QuestManager(Core core, QuestRegistry questRegistry, QuestProgressRepository repository,
                        ScopeResolver scopeResolver, QuestPrimitiveHandlers handlers, ItemRegistry itemRegistry,
                        ItemFactory itemFactory, LootTableRegistry lootTableRegistry,
                        LootSessionController lootSessionController, ZoneManager zoneManager) {
        this.core = core;
        this.questRegistry = questRegistry;
        this.repository = repository;
        this.scopeResolver = scopeResolver;
        this.handlers = handlers;
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.lootTableRegistry = lootTableRegistry;
        this.lootSessionController = lootSessionController;
        this.zoneManager = zoneManager;
        registerCoreHandlers();
    }

    /** Warm the in-memory cache from the DB. Call once after the registry loads. */
    public void loadAll() {
        active.clear();
        active.addAll(repository.loadAllActive());
        completed.clear();
        completed.addAll(repository.loadCompletedScopeKeys());
        log.info("Loaded {} active quest instances, {} completed scope keys", active.size(), completed.size()).submit();
    }

    public List<QuestInstance> activeFor(Player player) {
        List<QuestInstance> result = new ArrayList<>();
        for (QuestInstance instance : active) {
            if (scopeResolver.participates(player, instance.getScopeType(), instance.getScopeId())) {
                result.add(instance);
            }
        }
        return result;
    }

    /** Explicit start (command / giver). Announces failures to the player. */
    public void startQuest(Player player, String questId) {
        Optional<QuestDefinition> maybe = questRegistry.get(questId);
        if (maybe.isEmpty()) {
            UtilMessage.simpleMessage(player, "Quest", "Unknown quest: " + questId);
            return;
        }

        final ScopeKey scope = scopeResolver.resolve(player, maybe.get().getScope());
        final QuestInstance instance = findActive(questId, scope).orElseThrow();
        completeQuest(player, instance, maybe.get());
//        beginQuest(player, maybe.get(), true);
    }

    /**
     * Start {@code def} for the player's scope if it isn't already active and (for
     * non-repeatable quests) hasn't been completed, and its requirements pass.
     *
     * @param announce whether to message the player on start/skip (explicit start vs silent auto-start)
     * @return true if a new instance was created
     */
    private boolean beginQuest(Player player, QuestDefinition def, boolean announce) {
        ScopeKey scope = scopeResolver.resolve(player, def.getScope());

        if (findActive(def.getId(), scope).isPresent()) {
            if (announce) UtilMessage.simpleMessage(player, "Quest", "That quest is already active.");
            return false;
        }
        if (!def.isRepeatable() && isCompleted(scope.getType(), scope.getId(), def.getId())) {
            if (announce) UtilMessage.simpleMessage(player, "Quest", "You have already completed that quest.");
            return false;
        }
        for (PrimitiveData requirement : def.getRequirements()) {
            if (!handlers.evaluate(player, requirement)) {
                if (announce) UtilMessage.simpleMessage(player, "Quest", "You don't meet the requirements for this quest.");
                return false;
            }
        }

        QuestInstance instance = new QuestInstance(UUID.randomUUID(), def.getId(), scope.getType(), scope.getId());
        for (QuestNode root : def.rootStages()) {
            addStage(instance, def, root.getId());
        }
        active.add(instance);
        save(instance);
        if (announce) UtilMessage.message(player, "Quest", "Started: <green>" + def.getName());
        return true;
    }

    /**
     * Record a gameplay event against active objectives. {@code matcher} decides
     * whether a given objective's params match the event (e.g. the right zone).
     */
    public void recordEvent(Player player, String triggerType, Predicate<PrimitiveData> matcher, int amount) {
        // Auto-start any quest whose root stage opens on this trigger, before recording, so a single
        // event both starts the quest and advances its first objective. beginQuest is a no-op when the
        // quest is already active or (non-repeatable) already completed, so this can't duplicate.
        for (QuestDefinition candidate : questRegistry.questsStartableBy(triggerType)) {
            boolean rootMatches = candidate.rootStages().stream()
                    .flatMap(root -> root.getData().getObjectives().stream())
                    .anyMatch(objective -> triggerType.equals(objective.getType()) && matcher.test(objective));
            if (rootMatches) {
                beginQuest(player, candidate, false);
            }
        }

        for (QuestInstance instance : active) {
            if (!instance.getStatus().equals(QuestInstance.STATUS_ACTIVE)) continue;
            if (!scopeResolver.participates(player, instance.getScopeType(), instance.getScopeId())) continue;
            QuestDefinition def = questRegistry.get(instance.getQuestId()).orElse(null);
            if (def == null) continue;

            boolean changed = false;
            for (String stageId : new ArrayList<>(instance.getCurrentStages())) {
                QuestNode node = def.node(stageId).orElse(null);
                if (node == null) continue;
                for (PrimitiveData objective : node.getData().getObjectives()) {
                    if (!triggerType.equals(objective.getType()) || !matcher.test(objective)) continue;
                    String key = QuestInstance.objectiveKey(stageId, objective.getId());
                    int target = instance.getTargets().getOrDefault(key, Math.max(1, objective.getInt("count", 1)));
                    int current = Math.min(target, instance.getProgress().getOrDefault(key, 0) + amount);
                    instance.getProgress().put(key, current);
                    instance.getTargets().put(key, target);
                    changed = true;
                }
            }
            if (changed) {
                for (String stageId : new ArrayList<>(instance.getCurrentStages())) {
                    if (instance.isStageComplete(stageId)) {
                        completeStage(player, instance, def, stageId);
                    }
                }
                save(instance);
            }
        }
    }

    private void addStage(QuestInstance instance, QuestDefinition def, String stageId) {
        instance.getCurrentStages().add(stageId);
        def.node(stageId).ifPresent(node -> {
            for (PrimitiveData objective : node.getData().getObjectives()) {
                String key = QuestInstance.objectiveKey(stageId, objective.getId());
                instance.getProgress().putIfAbsent(key, 0);
                instance.getTargets().putIfAbsent(key, Math.max(1, objective.getInt("count", 1)));
            }
        });
    }

    private void completeStage(Player player, QuestInstance instance, QuestDefinition def, String stageId) {
        List<PrimitiveData> gating = def.node(stageId)
                .map(node -> node.getData().getActions().stream()
                        .filter(action -> handlers.isGating(action.getType()))
                        .toList())
                .orElse(List.of());
        if (gating.isEmpty()) {
            finishStage(player, instance, def, stageId);
            return;
        }

        // Gating actions (e.g. a conversation) must resolve true before the stage
        // completes. The stage stays current until then, so re-firing the trigger
        // retries a failed or aborted gate. The gate's owner guarantees resolution,
        // so no pending state is held here.
        handlers.runGatingSequence(player, gating).thenAccept(success -> {
            if (!success || !instance.getStatus().equals(QuestInstance.STATUS_ACTIVE)) return;
            if (!instance.getCurrentStages().contains(stageId)) return;
            finishStage(player, instance, def, stageId);
            save(instance);
        });
    }

    /** Unconditionally complete a stage. Gating actions already ran as the gate, so only plain actions fire. */
    private void finishStage(Player player, QuestInstance instance, QuestDefinition def, String stageId) {
        instance.getCurrentStages().remove(stageId);
        def.node(stageId).ifPresent(node -> node.getData().getActions().stream()
                .filter(action -> !handlers.isGating(action.getType()))
                .forEach(action -> handlers.run(player, action)));

        for (QuestNode next : def.nextStages(stageId)) {
            boolean guardsPass = def.edge(stageId, next.getId())
                    .map(edge -> edge.getData().getConditions().stream().allMatch(c -> handlers.evaluate(player, c)))
                    .orElse(true);
            if (guardsPass) {
                addStage(instance, def, next.getId());
            }
        }

        if (instance.getCurrentStages().isEmpty()) {
            completeQuest(player, instance, def);
        }
    }

    private void completeQuest(Player player, QuestInstance instance, QuestDefinition def) {
        instance.setStatus(QuestInstance.STATUS_COMPLETED);
        completed.add(completedKey(instance.getScopeType(), instance.getScopeId(), instance.getQuestId()));
        def.getRewards().forEach(reward -> handlers.run(player, reward));
        UtilMessage.message(player, "Quest", "Completed: <gold>" + def.getName());
    }

    public Optional<QuestInstance> findActive(String questId, ScopeKey scope) {
        return active.stream()
                .filter(i -> i.getQuestId().equals(questId)
                        && i.getScopeType().equals(scope.getType())
                        && i.getScopeId().equals(scope.getId())
                        && i.getStatus().equals(QuestInstance.STATUS_ACTIVE))
                .findFirst();
    }

    private boolean isCompleted(String scopeType, String scopeId, String questId) {
        return completed.contains(completedKey(scopeType, scopeId, questId));
    }

    private String completedKey(String scopeType, String scopeId, String questId) {
        return scopeType + ":" + scopeId + ":" + questId;
    }

    private void save(QuestInstance instance) {
        UtilServer.runTaskAsync(core, () -> {
            try {
                repository.save(instance);
            } catch (Exception ex) {
                log.error("Failed to save quest instance {}", instance.getInstanceId(), ex).submit();
            }
        });
    }

    // ── Core primitive handlers ─────────────────────────────────────────────
    private void registerCoreHandlers() {
        handlers.registerCondition("condition.has_item", (player, data) -> countItem(player, data.getString("item")) >= data.getInt("count", 1));
        handlers.registerCondition("condition.in_zone", (player, data) -> inZone(player, data.getString("zone")));
        handlers.registerCondition("condition.quest_completed", (player, data) -> questCompletedFor(player, data.getString("quest")));
        handlers.registerCondition("requirement.quest_completed", (player, data) -> questCompletedFor(player, data.getString("quest")));

        handlers.registerAction("action.send_message", (player, data) -> UtilMessage.message(player, "Quest", data.getString("message")));
        handlers.registerAction("action.play_sound", this::playSound);
        handlers.registerAction("action.fire_event", (player, data) -> UtilServer.callEvent(new QuestMilestoneEvent(player, data.getString("key"))));
        handlers.registerAction("action.give_item", (player, data) -> giveItem(player, data.getString("item"), data.getInt("amount", 1)));
        handlers.registerAction("reward.item", (player, data) -> giveItem(player, data.getString("item"), data.getInt("amount", 1)));
        handlers.registerAction("reward.loot_table", this::rollLootTable);
    }

    private boolean questCompletedFor(Player player, String questId) {
        if (questId == null) return false;
        ScopeKey solo = new ScopeKey("solo", player.getUniqueId().toString());
        return isCompleted(solo.getType(), solo.getId(), questId);
    }

    private boolean inZone(Player player, String zoneKey) {
        if (zoneKey == null) return false;
        Zone zone = zoneManager.getZoneAt(player.getLocation());
        return zone != null && zone.getKey().asString().equals(zoneKey);
    }

    private int countItem(Player player, String itemKey) {
        if (itemKey == null) return 0;
        Material material = Material.matchMaterial(itemKey);
        if (material == null) return 0; // custom-item matching is a leaf-module extension
        int count = 0;
        for (ItemStack stack : player.getInventory().getContents()) {
            if (stack != null && stack.getType() == material) count += stack.getAmount();
        }
        return count;
    }

    private void giveItem(Player player, String itemKey, int amount) {
        if (itemKey == null) return;
        BaseItem base = itemRegistry.getItem(itemKey);
        if (base == null) {
            log.warn("Quest give_item: unknown item {}", itemKey).submit();
            return;
        }
        int toGive = Math.max(1, amount);
        while (toGive > 0) {
            ItemStack stack = itemFactory.create(base).getItemStack();
            int giveAmount = Math.min(toGive, stack.getMaxStackSize());
            stack.setAmount(giveAmount);
            player.getInventory().addItem(stack);
            toGive -= giveAmount;
        }
    }

    private void playSound(Player player, PrimitiveData data) {
        String key = data.getString("key");
        if (key == null || key.isBlank()) return;
        Object volume = data.getParams().get("volume");
        Object pitch = data.getParams().get("pitch");
        player.playSound(player.getLocation(), key,
                volume instanceof Number v ? v.floatValue() : 1f,
                pitch instanceof Number p ? p.floatValue() : 1f);
    }

    private void rollLootTable(Player player, PrimitiveData data) {
        String id = data.getString("lootTable");
        if (id == null) return;
        LootTable table = lootTableRegistry.getLoaded().get(id);
        if (table == null) {
            log.warn("Quest reward.loot_table: unknown table {}", id).submit();
            return;
        }
        LootSession session = lootSessionController.resolve(player, table, () -> LootSession.newSession(table, player));
        LootContext context = new LootContext(session, player.getLocation(), LootSource.of("Quest", "quest:reward"));
        table.generateLoot(context).award();
    }
}
