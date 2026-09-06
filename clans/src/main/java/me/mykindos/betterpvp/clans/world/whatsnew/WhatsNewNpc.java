package me.mykindos.betterpvp.clans.world.whatsnew;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.cutscene.Cutscene;
import me.mykindos.betterpvp.core.cutscene.CutsceneManager;
import me.mykindos.betterpvp.core.cutscene.CutsceneRegistry;
import me.mykindos.betterpvp.core.cutscene.camera.Easing;
import me.mykindos.betterpvp.core.cutscene.effect.ShotEffects;
import me.mykindos.betterpvp.core.cutscene.signal.Signal;
import me.mykindos.betterpvp.core.cutscene.skip.SkipPolicy;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.quest.conversation.ConversationDefinition;
import me.mykindos.betterpvp.core.quest.conversation.ConversationText;
import me.mykindos.betterpvp.core.quest.conversation.Conversations;
import me.mykindos.betterpvp.core.scene.interaction.SceneInteractionRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

/**
 * The Chronicler: an NPC who shows a player around whatever is new, as a guided camera tour with dialogue.
 * <p>
 * This is a working demo of the cutscene system as much as it is content. It is meant to be edited - the four camera
 * ids below are placeholders, and the whole tour is four shots long precisely so that adding a fifth is visibly one
 * marker plus one line.
 *
 * <h3>Placing the NPC</h3>
 * Draw an {@code npc_resident} marker (perspective - the facing is which way they stand) and tag it:
 * <pre>
 * npc_resident   interact:whats_new  name:Chronicler  skin:skin_farmer
 * </pre>
 * That is the whole of it. Moving them, or having one in every world, is a map edit and never a code change.
 *
 * <h3>Placing the camera</h3>
 * Each shot names a {@code camera} marker - also perspective, because where it looks <em>is</em> the shot - carrying an
 * {@code id:} tag:
 * <pre>
 * camera   id:whats_new/opening
 * camera   id:whats_new/first
 * camera   id:whats_new/second
 * camera   id:whats_new/closing
 * </pre>
 * Rename the constants below to suit and drag the markers wherever the tour should look. {@code /cutscene markers}
 * lists what a world actually declares, and {@code /cutscene validate whats_new} says whether this cutscene agrees
 * with it.
 *
 * <h3>Iterating on it</h3>
 * {@code /cutscene play whats_new} needs no NPC at all, so the tour can be watched before anyone is standing there to
 * offer it. {@code /cutscene from whats_new second} starts at a named shot, which is what to use when the third shot
 * is the one that reads wrong. {@code /cutscene forget whats_new} makes it a first viewing again, so the
 * {@link SkipPolicy#seenBefore} rule below can be tested from both sides.
 *
 * @see me.mykindos.betterpvp.clans.world.mine.TrainingMineNpc for why dialogue like this is built in code
 */
@Singleton
@BPvPListener
public class WhatsNewNpc implements Listener {

    /** The name a marker carries to become this NPC, and the id the tour is registered and played under. */
    public static final String INTERACTION = "whats_new";
    public static final String CUTSCENE = "whats_new";

    /**
     * The {@code camera} marker ids this tour looks through, in order. These are the placeholders to replace: each
     * one is a marker you draw, and nothing else in the file needs to change when you rename one.
     */
    private static final String CAMERA_OPENING = "whats_new/opening";
    private static final String CAMERA_FIRST = "whats_new/first";
    private static final String CAMERA_SECOND = "whats_new/second";
    private static final String CAMERA_CLOSING = "whats_new/closing";

    @Inject
    public WhatsNewNpc(@NotNull CutsceneManager cutscenes, @NotNull CutsceneRegistry registry,
                       @NotNull SceneInteractionRegistry sceneInteractions) {
        registry.register(CUTSCENE, "What's New tour", this::tour);
        sceneInteractions.register(INTERACTION, (player, placement) -> cutscenes.start(player, CUTSCENE));
    }

    /**
     * The tour, assembled for one viewer.
     * <p>
     * Registered as a factory rather than an instance so it is rebuilt each time somebody watches it - which is what
     * would let a line or a shot depend on who is standing there, once it needs to.
     */
    @VisibleForTesting
    @NotNull Cutscene tour(Player viewer) {
        return Cutscene.builder(CUTSCENE)
                // The first viewing is watched in full; anyone who has seen it before may skip.
                .skip(SkipPolicy.seenBefore(SkipPolicy.all()))
                .dialogue(dialogue(viewer))
                .camera(camera -> camera
                        // Opens where the player already is looking, and waits for them rather than a clock.
                        .shot(CAMERA_OPENING, shot -> shot
                                .cut()
                                .until(Signal.response("greet", "show_me")))

                        // Eases across, then holds until the line it is paired with has been answered.
                        .shot(CAMERA_FIRST, shot -> shot
                                .travel(2.5, Easing.EASE_IN_OUT)
                                .until(Signal.nodeEnd("first")))

                        .shot(CAMERA_SECOND, shot -> shot
                                .travel(3.0, Easing.EASE_IN_OUT)
                                .until(Signal.nodeEnd("second")))

                        // A long drift out, with a caption that lives exactly as long as this shot does.
                        .shot(CAMERA_CLOSING, shot -> shot
                                .travel(4.0, Easing.EASE_IN)
                                .until(Signal.nodeEnd("closing"))
                                .during(ShotEffects.subtitle(
                                        Component.text("See you out there.", NamedTextColor.GRAY)))))
                .build();
    }

    /**
     * The dialogue half of the tour.
     * <p>
     * Each line after the first waits on the shot it belongs to, so it starts typing as the camera lands rather than
     * while it is still moving. Every line carries both a translation key and the literal text: an unregistered key
     * falls back to the literal, so this reads correctly today and becomes translatable the moment the keys are added.
     */
    private @NotNull ConversationDefinition dialogue(Player viewer) {
        final String speaker = speaker(viewer);
        return Conversations.builder(CUTSCENE)
                .typewriterCps(55)
                .node("greet", node -> node
                        .speaker(speaker)
                        .bodyKey("clans.whatsnew.greet")
                        .body("Plenty has changed since you were last here. Want the tour?")
                        .response(response -> response
                                .id("show_me")
                                .labelKey("clans.whatsnew.show-me")
                                .label("Show me.")
                                // Taken on a skip, so skipping never leaves the tour hanging on an unasked question.
                                .onSkip()
                                .goTo("first"))
                        .response(response -> response
                                .id("not_now")
                                .labelKey("clans.whatsnew.not-now")
                                .label("Not right now.")
                                .end()))

                .node("first", node -> node
                        .speaker(speaker)
                        .await(Signal.beatKey(CAMERA_FIRST))
                        .bodyKey("clans.whatsnew.first")
                        .body("This is the first thing worth knowing about.")
                        .response(response -> response
                                .id("go_on")
                                .labelKey("clans.whatsnew.go-on")
                                .label("Go on.")
                                .onSkip()
                                .goTo("second")))

                .node("second", node -> node
                        .speaker(speaker)
                        .await(Signal.beatKey(CAMERA_SECOND))
                        .bodyKey("clans.whatsnew.second")
                        .body("And this one you will want to have seen before you need it.")
                        .response(response -> response
                                .id("understood")
                                .labelKey("clans.whatsnew.understood")
                                .label("Understood.")
                                .onSkip()
                                .goTo("closing")))

                // No responses, so confirming here ends the conversation - and the closing shot is waiting on exactly
                // that, which is what ends the tour rather than a timer that has to be kept in sync with the dialogue.
                .node("closing", node -> node
                        .speaker(speaker)
                        .await(Signal.beatKey(CAMERA_CLOSING))
                        .bodyKey("clans.whatsnew.closing")
                        .body("That is everything. Go and have a look for yourself."))
                .build();
    }

    /** The nameplate, resolved per player so the speaker's name is translated alongside their dialogue. */
    private @NotNull String speaker(Player viewer) {
        return ConversationText.resolve("clans.whatsnew.speaker", "Chronicler",
                viewer == null ? null : viewer.locale());
    }
}
