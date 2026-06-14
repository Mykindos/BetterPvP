package me.mykindos.betterpvp.core.quest.conversation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.offhand.OffhandExecutor;
import me.mykindos.betterpvp.core.quest.model.PrimitiveData;
import me.mykindos.betterpvp.core.quest.primitive.QuestPrimitiveHandlers;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.text.Component;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

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

    private final ClientManager clientManager;
    private final ConversationRegistry registry;
    private final QuestPrimitiveHandlers handlers;
    private final ConversationRenderer renderer = new ConversationRenderer();
    private final Map<UUID, ConversationSession> sessions = new ConcurrentHashMap<>();

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
        ConvNode start = def.startNode().orElse(null);
        if (start == null) return false;

        final Gamer gamer = clientManager.search().online(player).getGamer();
        if (gamer.isInCombat()) {
            UtilMessage.simpleMessage(player, "Quest", "You cannot start a conversation while in combat.");
            SoundEffect.LOW_PITCH_PLING.play(player);
            return false;
        }

        final UUID id = player.getUniqueId();
        final ConversationActionBar bar = new ConversationActionBar(g -> {
            ConversationSession s = sessions.get(id);
            return s == null ? null : render(s);
        });

        ConversationSession session = new ConversationSession(id, gamer, def, bar);
        sessions.put(id, session);
        gamer.getActionBarOverrides().push(100, bar);
        gamer.setOffhandExecutor(100, confirmExecutor);
        // Pin the cursor mid-hotbar so scrolling can go either way and keys 1-3 stay free for direct selection.
        player.getInventory().setHeldItemSlot(5);
        freeze(player);
        new SoundEffect(Sound.ENTITY_VILLAGER_AMBIENT, 1.2f, 0.5f).play(player);
        goToNode(session, player, start.getId());
        return true;
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
        ConvResponse chosen = options.get(index);
        // Side effects fire first, regardless of where the outcome leads.
        for (PrimitiveData action : chosen.getActions()) {
            handlers.run(player, action);
        }
        resolveOutcome(session, player, chosen.getOutcome());
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
        session.getGamer().getActionBarOverrides().remove(session.getActionBar());
        session.getGamer().removeOffhandExecutor(100);
        unfreeze(player);
        new SoundEffect(Sound.ENTITY_VILLAGER_NO, 0.8f, 0.5f).play(player);
        session.getCompletion().complete(completed);
    }

    private void goToNode(ConversationSession session, Player player, String nodeId) {
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
    }

    /** Whether the current node's typewriter reveal has finished. Input and the response box wait on this. */
    private boolean isBodyRevealed(ConversationSession session) {
        ConvNode node = session.getDefinition().node(session.getCurrentNodeId()).orElse(null);
        if (node == null) return true;
        ConvNodeData data = node.getData();
        return revealedChars(session, data) >= data.getBody().length();
    }

    private List<ConvResponse> availableOptions(Player player, ConversationSession session) {
        List<ConvResponse> available = new ArrayList<>();
        for (ConvResponse response : session.getDefinition().responses(session.getCurrentNodeId())) {
            boolean allowed = response.getConditions().stream().allMatch(c -> handlers.evaluate(player, c));
            if (allowed) available.add(response);
        }
        return available;
    }

    private Component render(ConversationSession session) {
        ConvNode node = session.getDefinition().node(session.getCurrentNodeId()).orElse(null);
        if (node == null) return null;
        ConvNodeData data = node.getData();

        final int shownChars = revealedChars(session, data);
        playTypingSound(session, shownChars);

        return renderer.render(data, shownChars, session.getSelectedIndex());
    }

    /** Characters of the body revealed so far by the typewriter (full length when there is no typewriter). */
    private int revealedChars(ConversationSession session, ConvNodeData data) {
        final String body = data.getBody();
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
