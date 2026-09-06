package me.mykindos.betterpvp.core.quest.conversation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import lombok.Setter;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.offhand.OffhandExecutor;
import me.mykindos.betterpvp.core.quest.conversation.event.ConversationEndEvent;
import me.mykindos.betterpvp.core.quest.conversation.event.ConversationNodeEnterEvent;
import me.mykindos.betterpvp.core.quest.conversation.event.ConversationNodeExitEvent;
import me.mykindos.betterpvp.core.quest.conversation.event.ConversationResponseEvent;
import me.mykindos.betterpvp.core.quest.model.PrimitiveData;
import me.mykindos.betterpvp.core.quest.primitive.QuestPrimitiveHandlers;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives interactive conversations: tracks each player's {@link ConversationSession}, exposes
 * scroll-to-select / confirm transitions, runs response actions, and branches to the next node.
 * The visual rendering of a node lives in {@link ConversationRenderer}; this class only feeds it
 * the current typewriter/selection state and pushes the result onto the player's
 * {@link me.mykindos.betterpvp.core.utilities.model.display.actionbar.ActionBar}. Started in-game
 * via the {@code action.start_conversation} primitive (registered here) or directly by an NPC.
 */
@Singleton
@CustomLog
public class ConversationManager {

    private static final NamespacedKey FREEZE_KEY = new NamespacedKey("betterpvp", "conversation_freeze");

    /** Ceiling on how many responses a skip will take, so a conversation that loops cannot hang one. */
    private static final int MAX_SKIP_STEPS = 128;

    private final ClientManager clientManager;
    private final ConversationRegistry registry;
    private final QuestPrimitiveHandlers handlers;
    private final ConversationRenderer renderer = new ConversationRenderer();
    private final Map<UUID, ConversationSession> sessions = new ConcurrentHashMap<>();

    /** Answers a node's {@code await} key. Unset means nothing ever holds, which is the plain-dialogue case. */
    @Setter
    private @Nullable ConversationGate gate;

    /**
     * Confirms the selected response on an offhand press. Registered on the gamer
     * for the duration of a session, above the item-action executor so "No action
     * to trigger" feedback never fires mid-conversation.
     */
    private final OffhandExecutor confirmExecutor = (client, itemInstance) -> {
        final Player player = client.getGamer().getPlayer();
        if (player == null || !inConversation(player)) return false;
        confirm(player);
        return true;
    };

    @Inject
    public ConversationManager(ClientManager clientManager, ConversationRegistry registry, QuestPrimitiveHandlers handlers) {
        this.clientManager = clientManager;
        this.registry = registry;
        this.handlers = handlers;
        handlers.registerGatingAction("action.start_conversation", (player, data) -> startGated(player, data.getString("conversation")));
    }

    public boolean inConversation(Player player) {
        return sessions.containsKey(player.getUniqueId());
    }

    /**
     * Start a conversation and resolve once it ends: true only if the dialogue
     * ran to a terminal outcome. Resolves false immediately if it couldn't start.
     */
    public CompletableFuture<Boolean> startGated(Player player, String conversationId) {
        if (!start(player, conversationId)) return CompletableFuture.completedFuture(false);
        ConversationSession session = sessions.get(player.getUniqueId());
        return session == null ? CompletableFuture.completedFuture(false) : session.getCompletion();
    }

    /** @return true if the conversation actually started */
    public boolean start(Player player, String conversationId) {
        if (conversationId == null) return false;
        if (inConversation(player)) {
            UtilMessage.simpleMessage(player, "Quest", "You are already in a conversation.");
            SoundEffect.LOW_PITCH_PLING.play(player);
            return false;
        }
        ConversationDefinition def = registry.get(conversationId).orElse(null);
        if (def == null) {
            log.warn("Tried to start unknown conversation {}", conversationId).submit();
            return false;
        }
        return start(player, def);
    }

    /**
     * Starts a conversation from a definition held in hand rather than looked up by id — the entry point for dialogue
     * built in code (see {@link Conversations}), which never goes near the content table or the editor.
     * <p>
     * The session holds the definition itself, so everything downstream works identically whichever way it arrived.
     *
     * @return true if the conversation actually started
     */
    public boolean start(Player player, ConversationDefinition def) {
        return start(player, def, ConversationOptions.standalone());
    }

    /**
     * Starts a conversation whose presentation is partly somebody else's - a cutscene, which has already gated the
     * player and owns the screen. Flow, gating and completion are identical either way; only the freeze, the action
     * bar and the backdrop are skipped.
     *
     * @return true if the conversation actually started
     */
    public boolean start(Player player, ConversationDefinition def, ConversationOptions options) {
        if (inConversation(player)) {
            UtilMessage.simpleMessage(player, "Quest", "You are already in a conversation.");
            SoundEffect.LOW_PITCH_PLING.play(player);
            return false;
        }
        ConvNode start = def.startNode().orElse(null);
        if (start == null) return false;

        final Gamer gamer = clientManager.search().online(player).getGamer();
        // A managed conversation is already inside something that took the player out of the world, so the combat
        // check has been made by whoever started it - and refusing here would abort a cutscene mid-flight.
        if (options.isFreeze() && gamer.isInCombat()) {
            UtilMessage.simpleMessage(player, "Quest", "You cannot start a conversation while in combat.");
            SoundEffect.LOW_PITCH_PLING.play(player);
            return false;
        }

        final UUID id = player.getUniqueId();
        final ConversationActionBar bar = options.isOwnActionBar() ? new ConversationActionBar(g -> {
            ConversationSession s = sessions.get(id);
            return s == null ? null : render(s);
        }) : null;

        ConversationSession session = new ConversationSession(id, gamer, def, bar, options);
        sessions.put(id, session);
        if (bar != null) {
            gamer.getActionBarOverrides().push(100, bar);
        }
        gamer.setOffhandExecutor(100, confirmExecutor);
        // Pin the cursor mid-hotbar so scrolling can go either way and keys 1-3 stay free for direct selection.
        player.getInventory().setHeldItemSlot(5);
        if (options.isFreeze()) {
            freeze(player);
        }
        new SoundEffect(Sound.ENTITY_VILLAGER_AMBIENT, 1.2f, 0.5f).play(player);
        goToNode(session, player, start.getId());
        return true;
    }

    /**
     * Hands an already-running conversation's presentation over to its caller.
     * <p>
     * This is what a response growing a camera track needs: the dialogue was standalone a moment ago - frozen player,
     * its own action bar, its own backdrop - and now sits inside a cutscene that owns all three. Releasing them here
     * rather than restarting the conversation is what lets it carry on from the node it was already on.
     */
    public void manage(Player player) {
        final ConversationSession session = sessions.get(player.getUniqueId());
        if (session == null || !session.getOptions().isOwnActionBar()) {
            return;
        }
        if (session.getActionBar() != null) {
            session.getGamer().getActionBarOverrides().remove(session.getActionBar());
            session.setActionBar(null);
        }
        if (session.getOptions().isFreeze()) {
            unfreeze(player);
        }
        session.setOptions(ConversationOptions.managedByCaller());
    }

    /**
     * This player's conversation as it should look right now, for a caller that owns the screen and is compositing the
     * dialogue into its own layout.
     *
     * @return the drawn dialogue, or null if they are not in a conversation
     */
    public @Nullable Component renderFor(Player player) {
        final ConversationSession session = sessions.get(player.getUniqueId());
        return session == null ? null : render(session);
    }

    private void freeze(Player player) {
        applyFreezeModifier(player.getAttribute(Attribute.MOVEMENT_SPEED));
        applyFreezeModifier(player.getAttribute(Attribute.JUMP_STRENGTH));
    }

    private void unfreeze(Player player) {
        AttributeInstance speed = player.getAttribute(Attribute.MOVEMENT_SPEED);
        AttributeInstance jump = player.getAttribute(Attribute.JUMP_STRENGTH);
        if (speed != null) speed.removeModifier(FREEZE_KEY);
        if (jump != null) jump.removeModifier(FREEZE_KEY);
    }

    private void applyFreezeModifier(AttributeInstance attribute) {
        if (attribute == null || attribute.getModifier(FREEZE_KEY) != null) return;
        attribute.addTransientModifier(new AttributeModifier(FREEZE_KEY, -Integer.MAX_VALUE, AttributeModifier.Operation.ADD_NUMBER));
    }

    public void scroll(Player player, int direction) {
        ConversationSession session = sessions.get(player.getUniqueId());
        if (session == null || !isBodyRevealed(session)) return;
        List<ConvResponse> options = availableOptions(player, session);
        if (options.isEmpty()) return;
        int n = options.size();
        int next = (((session.getSelectedIndex() + direction) % n) + n) % n;
        if (next != session.getSelectedIndex()) {
            session.setSelectedIndex(next);
            new SoundEffect(Sound.UI_BUTTON_CLICK, 1.5f, 0.4f).play(player);
        }
    }

    /** Jump the cursor straight to a response index (hotbar keys 1-3). Out-of-range indices are ignored. */
    public void select(Player player, int index) {
        ConversationSession session = sessions.get(player.getUniqueId());
        if (session == null || !isBodyRevealed(session)) return;
        if (index >= 0 && index < availableOptions(player, session).size() && index != session.getSelectedIndex()) {
            session.setSelectedIndex(index);
            new SoundEffect(Sound.UI_BUTTON_CLICK, 1.5f, 0.4f).play(player);
        }
    }

    public void confirm(Player player) {
        ConversationSession session = sessions.get(player.getUniqueId());
        if (session == null || !isBodyRevealed(session)) return;
        List<ConvResponse> options = availableOptions(player, session);
        if (options.isEmpty()) {
            end(player, true); // terminal node
            return;
        }
        int index = Math.min(Math.max(0, session.getSelectedIndex()), options.size() - 1);
        choose(session, player, options.get(index));
    }

    /** Takes a response: its side effects, then its announcement, then wherever it leads. */
    private void choose(ConversationSession session, Player player, ConvResponse chosen) {
        // Side effects fire first, regardless of where the outcome leads.
        for (PrimitiveData action : chosen.getActions()) {
            handlers.run(player, action);
        }
        if (chosen.getThen() != null) {
            chosen.getThen().accept(player);
        }
        UtilServer.callEvent(new ConversationResponseEvent(player, session.getDefinition().getId(),
                session.getCurrentNodeId() == null ? "" : session.getCurrentNodeId(),
                chosen.getId() == null ? "" : chosen.getId()));
        resolveOutcome(session, player, chosen.getOutcome());
    }

    /** Whether this player is sitting on a revealed line with responses they have not answered yet. */
    public boolean isAwaitingChoice(Player player) {
        final ConversationSession session = sessions.get(player.getUniqueId());
        return session != null && isBodyRevealed(session) && !availableOptions(player, session).isEmpty();
    }

    /**
     * Runs the rest of a conversation without the player, for a skip.
     * <p>
     * Every response's side effects still fire - skipping is opting out of <em>reading</em>, not out of receiving what
     * the dialogue hands over. At each unanswered choice it takes the response marked
     * {@link ConvResponse#isOnSkip()}, or the first available one if none is.
     * <p>
     * Bounded rather than run to completion: dialogue is allowed to loop back on itself, and a conversation whose
     * skip-default happens to form a cycle would otherwise hang the server rather than end a cutscene.
     */
    public void fastForward(Player player) {
        for (int step = 0; step < MAX_SKIP_STEPS && inConversation(player); step++) {
            final ConversationSession session = sessions.get(player.getUniqueId());
            if (session == null) return;

            final List<ConvResponse> options = availableOptions(player, session);
            if (options.isEmpty()) {
                end(player, true);
                return;
            }
            choose(session, player, options.stream().filter(ConvResponse::isOnSkip).findFirst().orElse(options.getFirst()));
        }
        if (inConversation(player)) {
            log.warn("Fast-forwarding conversation for {} hit the step limit; it likely loops", player.getName()).submit();
            end(player, true);
        }
    }

    /** Apply a response's outcome to conversation flow. */
    private void resolveOutcome(ConversationSession session, Player player, ConvOutcome outcome) {
        String kind = outcome == null || outcome.getKind() == null ? "end" : outcome.getKind();
        switch (kind) {
            case "goto" -> {
                if (session.getDefinition().node(outcome.getTarget()).isPresent()) {
                    goToNode(session, player, outcome.getTarget());
                } else {
                    end(player, true); // dangling target — fail safe to ending
                }
            }
            case "start_conversation" -> {
                end(player, true); // this conversation concluded; the chained one is its own session
                handlers.run(player, action("action.start_conversation", "conversation", outcome.getConversationId()));
            }
            case "start_cinematic" -> {
                end(player, true);
                handlers.run(player, action("action.start_cinematic", "cinematic", outcome.getCinematicId()));
            }
            default -> end(player, true); // "end" and anything unknown
        }
    }

    /** Build a one-param action instance to dispatch through the handler registry. */
    private static PrimitiveData action(String type, String key, String value) {
        PrimitiveData data = new PrimitiveData();
        data.setType(type);
        data.getParams().put(key, value);
        return data;
    }

    /** Abort the session (quit, replacement by a new conversation, admin cleanup). */
    public void end(Player player) {
        end(player, false);
    }

    private void end(Player player, boolean completed) {
        ConversationSession session = sessions.remove(player.getUniqueId());
        if (session == null) return;
        if (session.getCurrentNodeId() != null) {
            UtilServer.callEvent(new ConversationNodeExitEvent(player, session.getDefinition().getId(),
                    session.getCurrentNodeId()));
        }
        if (session.getActionBar() != null) {
            session.getGamer().getActionBarOverrides().remove(session.getActionBar());
        }
        session.getGamer().removeOffhandExecutor(100);
        if (session.getOptions().isFreeze()) {
            unfreeze(player);
        }
        new SoundEffect(Sound.ENTITY_VILLAGER_NO, 0.8f, 0.5f).play(player);
        session.getCompletion().complete(completed);
        UtilServer.callEvent(new ConversationEndEvent(player, session.getDefinition().getId(), completed));
    }

    private void goToNode(ConversationSession session, Player player, String nodeId) {
        final String leaving = session.getCurrentNodeId();
        if (leaving != null) {
            UtilServer.callEvent(new ConversationNodeExitEvent(player, session.getDefinition().getId(), leaving));
        }
        session.setCurrentNodeId(nodeId);
        session.setSelectedIndex(0);
        session.setNodeStartMillis(System.currentTimeMillis());
        session.setShownCharCount(0);
        session.getDefinition().node(nodeId).ifPresent(node -> {
            String voice = node.getData().getVoiceLineKey();
            if (voice != null && !voice.isBlank()) {
                player.playSound(player.getLocation(), voice, 1f, 1f);
            }
        });
        UtilServer.callEvent(new ConversationNodeEnterEvent(player, session.getDefinition().getId(), nodeId));
    }

    /** Whether the current node's typewriter reveal has finished. Input and the response box wait on this. */
    private boolean isBodyRevealed(ConversationSession session) {
        ConvNode node = session.getDefinition().node(session.getCurrentNodeId()).orElse(null);
        if (node == null) return true;
        if (isHeld(session, node)) return false;
        ConvNodeData data = node.getData();
        final String body = bodyFor(session, data);
        return revealedChars(session, data, body) >= body.length();
    }

    /**
     * The current node's line as this reader sees it. A translated line has to be flattened per player before anything
     * measures it, because the typewriter counts characters and the renderer counts pixels.
     */
    private String bodyFor(ConversationSession session, ConvNodeData data) {
        final Player player = Bukkit.getPlayer(session.getPlayerId());
        return ConversationText.resolve(data.getBodyKey(), data.getBody(),
                player == null ? null : player.locale(), data.getBodyArgs());
    }

    private List<ConvResponse> availableOptions(Player player, ConversationSession session) {
        List<ConvResponse> available = new ArrayList<>();
        for (ConvResponse response : session.getDefinition().responses(session.getCurrentNodeId())) {
            boolean allowed = response.getConditions().stream().allMatch(c -> handlers.evaluate(player, c));
            // A code-built gate stacks with the data-driven ones rather than replacing them, so a response carrying
            // both has to satisfy both.
            if (allowed && response.getWhen() != null) {
                allowed = response.getWhen().test(player);
            }
            if (allowed) available.add(response);
        }
        return available;
    }

    private Component render(ConversationSession session) {
        ConvNode node = session.getDefinition().node(session.getCurrentNodeId()).orElse(null);
        if (node == null) return null;
        ConvNodeData data = node.getData();
        final Player player = Bukkit.getPlayer(session.getPlayerId());
        if (player == null) return null;

        final String body = bodyFor(session, data);
        final int shownChars = isHeld(session, node) ? 0 : revealedChars(session, data, body);
        playTypingSound(session, shownChars);

        // Only the options this player can actually pick are drawn, so the list they read matches the list they
        // scroll through.
        final List<String> labels = new ArrayList<>();
        for (ConvResponse response : availableOptions(player, session)) {
            labels.add(ConversationText.resolve(response.getLabelKey(), response.getLabel(), player.locale(),
                    response.getLabelArgs()));
        }
        return renderer.render(body, data.getSpeaker(), labels, shownChars, session.getSelectedIndex(),
                session.getOptions().isDrawBackdrop());
    }

    /**
     * Whether this node is still waiting on its {@code await} key.
     * <p>
     * While it is, the node's clock is pushed forward every tick, so the typewriter starts from the instant the hold
     * lifts rather than from when the node was reached - the difference between a line that begins as the camera
     * lands and one that is already half-typed by then.
     */
    private boolean isHeld(ConversationSession session, ConvNode node) {
        final String await = node.getData().getAwait();
        if (gate == null || await == null || await.isBlank()) {
            return false;
        }
        final Player player = Bukkit.getPlayer(session.getPlayerId());
        if (player == null || !gate.isHeld(player, await)) {
            return false;
        }
        session.setNodeStartMillis(System.currentTimeMillis());
        session.setShownCharCount(0);
        return true;
    }

    /** Characters of the body revealed so far by the typewriter (full length when there is no typewriter). */
    private int revealedChars(ConversationSession session, ConvNodeData data, String body) {
        if (data.getTypewriterCps() <= 0) return body.length();
        final long elapsed = System.currentTimeMillis() - session.getNodeStartMillis();
        return (int) Math.min(body.length(), (elapsed / 1000.0) * data.getTypewriterCps());
    }

    /** Tick the typing sound once each time the typewriter reveals more of the body. */
    private void playTypingSound(ConversationSession session, int shownChars) {
        if (shownChars <= session.getShownCharCount()) return;
        session.setShownCharCount(shownChars);
        final Player player = session.getGamer().getPlayer();
        if (player != null) {
            new SoundEffect("betterpvp", "conversation.typing", 1.0f, 0.4f).play(player);
        }
    }
}
