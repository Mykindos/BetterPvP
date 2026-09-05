package me.mykindos.betterpvp.core.quest.conversation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.quest.model.PrimitiveData;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * An actionable response under a dialogue line. Its identity is its
 * {@link #outcome} (what happens to the conversation), not a graph edge.
 * {@link #conditions} gate visibility; {@link #actions} are orthogonal side
 * effects that fire when chosen, regardless of the outcome.
 * <p>
 * A conversation built in code uses {@link #when} and {@link #then} instead of the data-driven
 * {@link #conditions}/{@link #actions}: plain Java, closing over whatever the feature already has to hand, with no
 * primitive to register and nothing to publish. They are held alongside rather than in place of the data-driven pair,
 * so one response can carry both and neither kind of conversation has to know the other exists.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConvResponse {
    private String id;
    private String label = "Continue";
    /** Translation key for {@link #label}. Blank to use the literal text. */
    private String labelKey = "";
    /** Values for the placeholders of {@link #labelKey}, in order. Code-built only, like {@link #when}. */
    @JsonIgnore
    private transient List<Component> labelArgs = new ArrayList<>();
    /** Saved per player when chosen; usable later via a "has flag" condition. */
    private String flag = "";
    private List<PrimitiveData> conditions = new ArrayList<>();
    private List<PrimitiveData> actions = new ArrayList<>();
    private ConvOutcome outcome = new ConvOutcome();

    /** Extra visibility gate for a code-built response, ANDed with {@link #conditions}. Null means no extra gate. */
    @JsonIgnore
    private transient @Nullable Predicate<Player> when;

    /** Extra side effect for a code-built response, run alongside {@link #actions}. Null means none. */
    @JsonIgnore
    private transient @Nullable Consumer<Player> then;
}
