package me.mykindos.betterpvp.clans.world.mine;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.core.config.Config;
import me.mykindos.betterpvp.core.cooldowns.persistent.PersistentCooldownService;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.quest.conversation.ConversationDefinition;
import me.mykindos.betterpvp.core.quest.conversation.ConversationManager;
import me.mykindos.betterpvp.core.quest.conversation.ConversationText;
import me.mykindos.betterpvp.core.quest.conversation.Conversations;
import me.mykindos.betterpvp.core.scene.interaction.SceneInteractionRegistry;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;

/**
 * The quartermaster beside the training mine: hands out a pickaxe, and trades bloomstone for a starter set.
 * <p>
 * Placed as map data, not code — an {@code npc_resident} marker tagged {@code interact:training_mine} — so moving them,
 * or having one in every world, is a map edit. This class claims that name and owns what happens when they are clicked.
 *
 * <h3>Why the dialogue is built here</h3>
 * It is not a quest. It ships with the feature, changes with the feature, and would gain nothing from living in the
 * content editor except the requirement to publish a row before a fresh server works. Built with
 * {@link Conversations}, its gates are ordinary predicates over things this class already knows.
 *
 * <h3>Why the cooldown is read before the conversation opens</h3>
 * A response gate is a synchronous predicate and the cooldown lives in the database, so the question is asked first and
 * the dialogue is assembled around the answer. That also makes the refusal branch a real branch — the player is offered
 * a different line, rather than an option that silently does nothing.
 *
 * <h3>Why this is a listener</h3>
 * It handles no events. It is a {@link BPvPListener} because that is what guarantees it is <em>built</em>: this class
 * exists to claim an interaction name at startup, and a Guice singleton nothing injects is a singleton nothing ever
 * constructs — so the claim would never be made and the marker would report that nothing had registered its name.
 */
@Singleton
@BPvPListener
@CustomLog
public class TrainingMineNpc implements Listener {

    /** The name a marker carries to become this NPC. */
    public static final String INTERACTION = "training_mine";

    /** Keyed rather than per-NPC: one pickaxe per player, however many quartermasters they visit. */
    private static final String PICKAXE_COOLDOWN = "training_mine_pickaxe";

    private static final String STARTER_PICKAXE = "core:rustic_pickaxe";

    @Inject
    @Config(path = "trainingmine.pickaxe.cooldown-seconds", defaultValue = "300")
    private int pickaxeCooldownSeconds;

    @Inject
    @Config(path = "trainingmine.trade.cost", defaultValue = "24")
    private int setCost;

    private final Clans clans;
    private final ConversationManager conversations;
    private final PersistentCooldownService cooldowns;
    private final ItemRegistry itemRegistry;
    private final ItemFactory itemFactory;
    private final MineCurrency currency;
    private final KitTrade kitTrade;

    @Inject
    public TrainingMineNpc(@NotNull Clans clans, @NotNull ConversationManager conversations,
                           @NotNull PersistentCooldownService cooldowns, @NotNull ItemRegistry itemRegistry,
                           @NotNull ItemFactory itemFactory, @NotNull MineCurrency currency,
                           @NotNull KitTrade kitTrade, @NotNull SceneInteractionRegistry sceneInteractions) {
        this.clans = clans;
        this.conversations = conversations;
        this.cooldowns = cooldowns;
        this.itemRegistry = itemRegistry;
        this.itemFactory = itemFactory;
        this.currency = currency;
        this.kitTrade = kitTrade;

        sceneInteractions.register(INTERACTION, (player, placement) -> greet(player));
    }

    /** Resolves the cooldown, then opens the dialogue already knowing which pickaxe line to offer. */
    private void greet(@NotNull Player player) {
        cooldowns.remaining(player, PICKAXE_COOLDOWN).thenAccept(remaining ->
                UtilServer.runTask(clans, () -> {
                    if (player.isOnline()) {
                        conversations.start(player, dialogue(player, remaining.isZero()));
                    }
                }));
    }

    private @NotNull ConversationDefinition dialogue(@NotNull Player player, boolean pickaxeReady) {
        final boolean canTrade = currency.count(player) >= setCost;
        return Conversations.builder("training_mine_quartermaster")
                .typewriterCps(60)
                .node("greet", node -> node
                        .speaker(speaker(player))
                        .bodyKey("clans.mine.npc.greet")
                        .response(response -> response
                                .labelKey("clans.mine.npc.ask-pickaxe")
                                .when(who -> pickaxeReady)
                                .then(this::givePickaxe)
                                .goTo("pickaxe-given"))
                        .response(response -> response
                                .labelKey("clans.mine.npc.ask-pickaxe")
                                .when(who -> !pickaxeReady)
                                .goTo("pickaxe-denied"))
                        .response(response -> response
                                .labelKey("clans.mine.npc.ask-trade")
                                .when(who -> canTrade)
                                .then(who -> kitTrade.open(who, setCost))
                                .end())
                        .response(response -> response
                                .labelKey("clans.mine.npc.ask-trade")
                                .when(who -> !canTrade)
                                .goTo("trade-poor"))
                        .response(response -> response
                                .labelKey("clans.mine.npc.leave")
                                .end()))
                .node("pickaxe-given", node -> node
                        .speaker(speaker(player))
                        .bodyKey("clans.mine.npc.pickaxe-given"))
                .node("pickaxe-denied", node -> node
                        .speaker(speaker(player))
                        .bodyKey("clans.mine.npc.pickaxe-denied"))
                .node("trade-poor", node -> node
                        .speaker(speaker(player))
                        .bodyKey("clans.mine.npc.trade-poor", Component.text(setCost)))
                .build();
    }

    private void givePickaxe(@NotNull Player player) {
        final BaseItem pickaxe = itemRegistry.getItem(STARTER_PICKAXE);
        if (pickaxe == null) {
            log.warn("Training mine quartermaster cannot hand out '{}' - no such item is registered", STARTER_PICKAXE).submit();
            return;
        }

        // Started before the item is handed over, so a failure to fit it in the bag still costs the wait: otherwise a
        // player with a full inventory could re-roll the cooldown for free until they had room.
        cooldowns.start(player, PICKAXE_COOLDOWN, Duration.ofSeconds(pickaxeCooldownSeconds));
        final var leftover = player.getInventory().addItem(itemFactory.create(pickaxe).getItemStack());
        leftover.values().forEach(stack -> player.getWorld().dropItemNaturally(player.getLocation(), stack));
        new SoundEffect(Sound.ENTITY_ITEM_PICKUP, 1.3f, 2f).play(player);
    }

    /**
     * The nameplate. Resolved per player so the speaker's name is translated alongside their dialogue rather than
     * sitting in English above it.
     */
    private @NotNull String speaker(@NotNull Player player) {
        return ConversationText.resolve("clans.mine.npc.speaker", "Quartermaster", player.locale());
    }
}
