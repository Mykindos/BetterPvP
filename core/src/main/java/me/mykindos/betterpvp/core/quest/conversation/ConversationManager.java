package me.mykindos.betterpvp.core.quest.conversation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.CustomLog;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.offhand.OffhandExecutor;
import me.mykindos.betterpvp.core.quest.model.PrimitiveData;
import me.mykindos.betterpvp.core.quest.primitive.QuestPrimitiveHandlers;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Drives interactive conversations: renders the current dialogue (with a
 * typewriter reveal) into the player's {@link me.mykindos.betterpvp.core.utilities.model.display.actionbar.ActionBar},
 * exposes scroll-to-select / confirm transitions, runs response actions, and
 * branches to the next node. Started in-game via the {@code action.start_conversation}
 * primitive (registered here) or directly by an NPC.
 */
@Singleton
@CustomLog
public class ConversationManager {

    private static final NamespacedKey FREEZE_KEY = new NamespacedKey("betterpvp", "conversation_freeze");

    private final ClientManager clientManager;
    private final ConversationRegistry registry;
    private final QuestPrimitiveHandlers handlers;
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
        if (data.getTypewriterCps() <= 0) return true;
        long elapsed = System.currentTimeMillis() - session.getNodeStartMillis();
        return (elapsed / 1000.0) * data.getTypewriterCps() >= data.getBody().length();
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

        // Typewriter reveal based on elapsed time and configured chars-per-second.
        String body = data.getBody();
        long elapsed = System.currentTimeMillis() - session.getNodeStartMillis();
        int shownChars = data.getTypewriterCps() <= 0
                ? body.length()
                : (int) Math.min(body.length(), (elapsed / 1000.0) * data.getTypewriterCps());
        String shown = body.substring(0, Math.max(0, shownChars));

        if (shownChars > session.getShownCharCount()) {
            session.setShownCharCount(shownChars);
            Player player = session.getGamer().getPlayer();
            if (player != null) {
                new SoundEffect("betterpvp", "conversation.typing", 1.0f, 0.4f).play(player);
            }
        }

        final TextComponent.Builder builder = Component.text();

        // Black bars
        final TextComponent.Builder blackBar = Component.text();
        int offset = 0;
        for (int i = 0; i < 12; i++) {
            blackBar.append(Component.text("\uE000", NamedTextColor.BLACK).font(Key.key("betterpvp", "conversation"))
                    .append(Component.translatable("space.-1").font(Resources.Font.SPACE)));
            offset += 256;
        }
        builder.append(Component.translatable("offset.-" + offset / 2, blackBar.build()).font(Resources.Font.SPACE));

        // Background
        builder.append(Component.empty()
                .append(Component.translatable("space.-" + offset).font(Resources.Font.SPACE))
                .append(Component.text("\uE001", NamedTextColor.WHITE).font(Key.key("betterpvp", "conversation"))));

        // The cursor now sits at the box's right edge \u2014 step back inside it. This rewind
        // is paid back after the text so the total advance (which the client centers on)
        // is untouched and the box never shifts.
        //  (dialogue.png) is 209px wide; its advance is width + 1px glyph spacing.
        final int boxWidth = 209;
        final int boxInnerPadding = 10;
        builder.append(Component.translatable("space.-" + (boxWidth + 1 - boxInnerPadding)).font(Resources.Font.SPACE));

        // Text — word-wrap the FULL body up front so words never split across lines;
        // only the typed portion of each line is drawn.
        final int maxTextWidth = boxWidth - boxInnerPadding * 2;

        List<String> wrappedLines = new ArrayList<>();
        List<Integer> lineStarts = new ArrayList<>();
        int wrapStart = 0;
        while (wrapStart < body.length()) {
            int end = wrapStart;
            int lastSpace = -1;
            int width = 0;
            while (end < body.length()) {
                char c = body.charAt(end);
                if (width + charWidth(c) > maxTextWidth) break;
                width += charWidth(c);
                if (c == ' ') lastSpace = end;
                end++;
            }
            if (end < body.length() && lastSpace > wrapStart) {
                end = lastSpace; // next word would be cut off — wrap it whole onto the next line
            }
            if (end == wrapStart) end++; // single word wider than the box — hard-split it
            wrappedLines.add(body.substring(wrapStart, end));
            lineStarts.add(wrapStart);
            wrapStart = end;
            while (wrapStart < body.length() && body.charAt(wrapStart) == ' ') wrapStart++;
        }

        int heightOffset = 18;
        for (int line = 0; line < wrappedLines.size(); line++) {
            String fullLine = wrappedLines.get(line);
            int lineStart = lineStarts.get(line);

            String text = fullLine.substring(0, Math.clamp(shown.length() - lineStart, 0, fullLine.length()));
            if (text.isEmpty()) {
                heightOffset -= 8 + 2;
                continue; // typewriter hasn't reached this line yet
            }
            int lineWidth = textWidth(text);
            Component component = Component.text(text, NamedTextColor.WHITE);
            String font;
            if (heightOffset == 0) {
                font = "offset/center";
            } else if (heightOffset < 0) {
                font = "offset/down_" + Math.abs(heightOffset);
            } else {
                font = "offset/up_" + heightOffset;
            }

            // Draw the line, then rewind by exactly its own width so every line
            // starts at the same X and contributes zero net advance.
            builder.append(component.font(Key.key("betterpvp", font)));
            if (lineWidth > 0) {
                builder.append(Component.translatable("space.-" + lineWidth).font(Resources.Font.SPACE));
            }

            heightOffset -= 8 + 2;
        }

        // Pay back the rewind into the box: return the cursor to the box's right edge so
        // the text contributes zero net advance and the box stays put while typing.
        builder.append(Component.translatable("space." + (boxWidth + 1 - boxInnerPadding)).font(Resources.Font.SPACE));

        // Nameplate
        final String speakerName = data.getSpeaker();
        final int speakerWidth = textWidth(speakerName);
        builder.append(Component.translatable("space.-" + (boxWidth - 10)).font(Resources.Font.SPACE));
        builder.append(Component.text("\uE004", NamedTextColor.WHITE).font(Key.key("betterpvp", "conversation")));
        builder.append(Component.translatable("space.-1").font(Resources.Font.SPACE));
        final int midIterations = speakerWidth / 2; // the mid-nameplate is 2 pixels wide per char
        for (int i = 0; i < midIterations; i++) {
            builder.append(Component.text("\uE005", NamedTextColor.WHITE).font(Key.key("betterpvp", "conversation")));
            builder.append(Component.translatable("space.-1").font(Resources.Font.SPACE));
        }
        builder.append(Component.text("\uE006", NamedTextColor.WHITE).font(Key.key("betterpvp", "conversation")));
        builder.append(Component.translatable("space.-" + (speakerWidth + 3)).font(Resources.Font.SPACE));
        builder.append(Component.text(speakerName, TextColor.color(0x3aba44)).font(Key.key("betterpvp", "offset/up_32")));

        // Pay back the nameplate's net cursor movement so it contributes zero advance and the
        // centered action bar keeps its width: -(boxWidth - 10) jump, +3 left cap, +2 per mid,
        // +4 right cap, and the name rewind/draw pair nets -3.
        final int nameplateNet = -(boxWidth - 10) + 3 + midIterations * 2 + 4 - 3;
        builder.append(Component.translatable("space." + (-nameplateNet)).font(Resources.Font.SPACE));

        // Responses — hidden until the typewriter has revealed the full body.
        final List<ConvResponse> responses = Collections.unmodifiableList(data.getResponses());
        if (!responses.isEmpty() && shownChars >= body.length()) {
            final int selected = session.getSelectedIndex();
            final int responseWidth = 134;
            builder.append(Component.translatable("space.-" + (responseWidth / 2)).font(Resources.Font.SPACE));
            builder.append(Component.text("\uE002", NamedTextColor.WHITE).font(Key.key("betterpvp", "conversation")));
            builder.append(Component.translatable("space.-" + (responseWidth - 12)).font(Resources.Font.SPACE));
            // Only three row offsets exist (69/59/49, matching the hand glyphs) — a single
            // response sits on the middle row so it's vertically centered in the box.
            int yOffset = responses.size() == 1 ? 59 : 69;
            int lastLineWidth = 0;
            for (int i = 0; i < responses.size(); i++) {
                final ConvResponse response = responses.get(i);
                final boolean isSelected = i == selected;
                final TextColor color = isSelected
                        ? NamedTextColor.YELLOW
                        : NamedTextColor.WHITE;

                final String responseText = response.getLabel();
                if (lastLineWidth != 0) {
                    builder.append(Component.translatable("space.-" + lastLineWidth).font(Resources.Font.SPACE));
                }

                if (isSelected) {
                    // hands are in this order \uE00A, \uE009, \uE008
                    char hand = switch (yOffset) {
                        case 69 -> '\uE00A';
                        case 59 -> '\uE009';
                        case 49 -> '\uE008';
                        default -> 't';
                    };
                    builder.append(Component.translatable("space.-10").font(Resources.Font.SPACE));
                    builder.append(Component.text(hand, NamedTextColor.WHITE).font(Key.key("betterpvp", "conversation")));
                    builder.append(Component.translatable("space.3").font(Resources.Font.SPACE));
                } else {
                    char num = switch (yOffset) {
                        case 69 -> '\ue033';
                        case 59 -> '\ue034';
                        case 49 -> '\ue035';
                        default -> 't';
                    };
                    String font = switch (yOffset) {
                        case 69 -> "input/up_79";
                        case 59 -> "input/up_69";
                        case 49 -> "input/up_59";
                        default -> "t";
                    };
                    // num glyphs are 10px wide, so their advance is 11 (+1px glyph spacing);
                    // -13/+11/+2 nets zero like the hand trio and leaves the same 3px gap to the text
                    builder.append(Component.translatable("space.-10").font(Resources.Font.SPACE));
                    builder.append(Component.text(num, NamedTextColor.WHITE).font(Key.key("betterpvp", font)));
                    builder.append(Component.translatable("space.2").font(Resources.Font.SPACE));
                }
                builder.append(Component.text(responseText, color).font(Key.key("betterpvp", "offset/up_" + yOffset)));

                yOffset -= 8 + 2;
                // charWidth already includes the 1px spacing, so the string's full advance IS
                // textWidth — rewinding any more drifts every following row left.
                lastLineWidth = textWidth(responseText) + 2;
            }

            // Pay back the block's net cursor movement so it contributes zero advance and the
            // centered action bar keeps its width: -(width/2) jump, +(width + 1) box, -(width - 12)
            // step back inside, telescoped line rewinds leave the last line's width, and both
            // icon trios (hand: -12/+9/+3, num: -13/+11/+2) net zero.
            final int responseNet = -(responseWidth / 2) + (responseWidth + 1) - (responseWidth - 12) + lastLineWidth;
            builder.append(Component.translatable("space." + (-responseNet)).font(Resources.Font.SPACE));

            // Input instruction
            final String inputLabel = "to select";
            builder.append(Component.translatable("space.6").font(Resources.Font.SPACE));
            builder.append(Component.text('\uE7EB').font(Key.key("betterpvp", "input/up_40")));
            builder.append(Component.translatable("space.3").font(Resources.Font.SPACE));
            builder.append(Component.text(inputLabel).font(Key.key("betterpvp", "offset/up_32")));

            // Pay back the input instruction's net cursor movement so it contributes zero
            // advance: +6 gap, +11 key glyph (10px wide + 1px glyph spacing), +3 gap, +label.
            final int inputNet = 6 + 8 + 3 + textWidth(inputLabel);
            builder.append(Component.translatable("space.-" + inputNet).font(Resources.Font.SPACE));
        }

        return builder.build();
    }

    /** Pixel advance of a string in the default font (glyph width + 1px spacing each). */
    private static int textWidth(String text) {
        int width = 0;
        for (char c : text.toCharArray()) {
            width += charWidth(c);
        }
        return width;
    }

    private static int charWidth(char c) {
        return switch (c) {
            case 'i', '!', '.', ',', ':', ';', '\'', '|' -> 2;
            case 'l', '`' -> 3;
            case 'I', 't', ' ', '[', ']' -> 4;
            case 'f', 'k', '"', '(', ')', '*', '<', '>', '{', '}' -> 5;
            case '@', '~' -> 7;
            default -> 6;
        };
    }

    private Key fontKey(String font) {
        return switch (font == null ? "default" : font) {
            case "small_caps" -> Resources.Font.SMALL_CAPS;
            case "font_3d" -> Resources.Font.FONT_3D;
            case "menu" -> Resources.Font.MENU;
            default -> Resources.Font.DEFAULT;
        };
    }
}
