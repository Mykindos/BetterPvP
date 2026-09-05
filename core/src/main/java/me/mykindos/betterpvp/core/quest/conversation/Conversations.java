package me.mykindos.betterpvp.core.quest.conversation;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Builds a conversation in code, for dialogue that belongs to a feature rather than to authored content.
 * <p>
 * Not every conversation is a quest. A shopkeeper who hands out a pickaxe has dialogue that exists because the feature
 * exists: it ships with the code, changes with the code, and has no life in an editor. Routing that through the content
 * table would mean publishing a row before the feature works on a fresh server, and expressing "have they got one
 * already?" as a registered primitive rather than as the one line of Java it actually is.
 * <p>
 * So a response here takes a {@link Predicate} to decide whether it is offered and a {@link Consumer} to run when it is
 * chosen, closing over whatever the feature already holds. Everything downstream is unchanged — the session carries the
 * definition either way — so a code-built conversation types out, scrolls and branches exactly like a published one.
 *
 * <pre>{@code
 * ConversationDefinition convo = Conversations.builder("quartermaster")
 *         .typewriterCps(60)
 *         .node("greet", node -> node
 *                 .speaker("Quartermaster")
 *                 .bodyKey("clans.mine.npc.greet")
 *                 .response(r -> r.labelKey("clans.mine.npc.ask_pickaxe")
 *                         .when(p -> !onCooldown(p))
 *                         .then(this::givePickaxe)
 *                         .goTo("given"))
 *                 .response(r -> r.labelKey("clans.mine.npc.leave").end()))
 *         .node("given", node -> node
 *                 .speaker("Quartermaster")
 *                 .bodyKey("clans.mine.npc.given"))
 *         .build();
 * }</pre>
 *
 * Pass the result to {@link ConversationManager#start(Player, ConversationDefinition)}. Build it per interaction rather
 * than once at startup when its gates depend on the player — that is the cheap way to have already answered a question
 * (a cooldown lookup, say) before the dialogue opens, instead of asking it from inside a predicate that cannot wait.
 *
 * @see ConversationManager#start(Player, ConversationDefinition)
 */
public final class Conversations {

    private Conversations() {
    }

    public static @NotNull Builder builder(@NotNull String id) {
        return new Builder(id);
    }

    private static @NotNull List<Component> components(@NotNull ComponentLike[] args) {
        return Arrays.stream(args).map(ComponentLike::asComponent).collect(Collectors.toCollection(ArrayList::new));
    }

    /** Assembles the nodes of one conversation. The first node added is where it starts. */
    public static final class Builder {

        private final String id;
        private final List<NodeBuilder> nodes = new ArrayList<>();
        private Integer typewriterCps;

        private Builder(@NotNull String id) {
            this.id = id;
        }

        /**
         * Typewriter speed for every node that does not name its own, in characters per second; {@code 0} shows the
         * whole line at once. Unset leaves each node at the default speed.
         * <p>
         * A speaker types at one pace, so this is the usual place to set it — {@link NodeBuilder#typewriterCps} is for
         * the one line that wants to be slower or faster than the rest. Order does not matter: it is applied when the
         * conversation is built, so it reaches nodes added before the call as well as after.
         */
        public @NotNull Builder typewriterCps(int cps) {
            this.typewriterCps = cps;
            return this;
        }

        /**
         * Adds a dialogue node.
         *
         * @param nodeId what responses branch to with {@link ResponseBuilder#goTo}
         */
        public @NotNull Builder node(@NotNull String nodeId, @NotNull Consumer<NodeBuilder> node) {
            final NodeBuilder builder = new NodeBuilder(nodeId);
            node.accept(builder);
            nodes.add(builder);
            return this;
        }

        public @NotNull ConversationDefinition build() {
            final ConversationDefinition definition = new ConversationDefinition();
            definition.setId(id);
            definition.setName(id);
            definition.setNodes(nodes.stream().map(node -> node.build(typewriterCps))
                    .collect(Collectors.toCollection(ArrayList::new)));
            // Named explicitly rather than left to be inferred: the fallback picks a node nothing branches into, which
            // is the wrong answer for dialogue that loops back to its own opening line.
            definition.setStartNodeId(nodes.isEmpty() ? null : nodes.get(0).id);
            return definition;
        }
    }

    /** One line of dialogue and the responses under it. */
    public static final class NodeBuilder {

        private final String id;
        private final ConvNodeData data = new ConvNodeData();
        private boolean ownTypewriterCps;

        private NodeBuilder(@NotNull String id) {
            this.id = id;
        }

        /** The name on the nameplate. */
        public @NotNull NodeBuilder speaker(@NotNull String speaker) {
            data.setSpeaker(speaker);
            return this;
        }

        /** The line, written literally. Prefer {@link #bodyKey} so it can be translated. */
        public @NotNull NodeBuilder body(@NotNull String body) {
            data.setBody(body);
            return this;
        }

        /**
         * The line, as a translation key. The literal {@link #body} stays as the fallback.
         *
         * @param args values for the key's {@code {0}}, {@code {1}} placeholders, in order
         */
        public @NotNull NodeBuilder bodyKey(@NotNull String bodyKey, @NotNull ComponentLike @NotNull ... args) {
            data.setBodyKey(bodyKey);
            data.setBodyArgs(components(args));
            return this;
        }

        /**
         * Typewriter speed for this line in characters per second; {@code 0} shows the whole line at once. Overrides
         * the conversation's {@link Builder#typewriterCps}.
         */
        public @NotNull NodeBuilder typewriterCps(int cps) {
            data.setTypewriterCps(cps);
            ownTypewriterCps = true;
            return this;
        }

        public @NotNull NodeBuilder response(@NotNull Consumer<ResponseBuilder> response) {
            final ResponseBuilder builder = new ResponseBuilder();
            response.accept(builder);
            data.getResponses().add(builder.response);
            return this;
        }

        private @NotNull ConvNode build(@Nullable Integer defaultCps) {
            if (defaultCps != null && !ownTypewriterCps) {
                data.setTypewriterCps(defaultCps);
            }
            final ConvNode node = new ConvNode();
            node.setId(id);
            node.setData(data);
            return node;
        }
    }

    /** One pickable response: whether it is offered, what it does, and where it leads. */
    public static final class ResponseBuilder {

        private final ConvResponse response = new ConvResponse();

        /** The response text, written literally. Prefer {@link #labelKey} so it can be translated. */
        public @NotNull ResponseBuilder label(@NotNull String label) {
            response.setLabel(label);
            return this;
        }

        /**
         * The response text, as a translation key. The literal {@link #label} stays as the fallback.
         *
         * @param args values for the key's {@code {0}}, {@code {1}} placeholders, in order
         */
        public @NotNull ResponseBuilder labelKey(@NotNull String labelKey, @NotNull ComponentLike @NotNull ... args) {
            response.setLabelKey(labelKey);
            response.setLabelArgs(components(args));
            return this;
        }

        /** Offer this response only when the player satisfies {@code when}. Unset means always offered. */
        public @NotNull ResponseBuilder when(@NotNull Predicate<Player> when) {
            response.setWhen(when);
            return this;
        }

        /** Runs when the response is chosen, before the conversation moves on. */
        public @NotNull ResponseBuilder then(@NotNull Consumer<Player> then) {
            response.setThen(then);
            return this;
        }

        /** Branch to another node. */
        public @NotNull ResponseBuilder goTo(@NotNull String nodeId) {
            final ConvOutcome outcome = new ConvOutcome();
            outcome.setKind("goto");
            outcome.setTarget(nodeId);
            response.setOutcome(outcome);
            return this;
        }

        /** Close the conversation. This is the default, so it only needs saying for readability. */
        public @NotNull ResponseBuilder end() {
            final ConvOutcome outcome = new ConvOutcome();
            outcome.setKind("end");
            response.setOutcome(outcome);
            return this;
        }
    }
}
