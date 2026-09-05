package me.mykindos.betterpvp.core.quest.conversation;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * A single line of dialogue plus the responses the player may pick from it.
 * <p>
 * {@link #body} is the line as written. {@link #bodyKey} names a translation key to show instead, so a conversation can
 * speak the reader's language; the literal body stays as the fallback for a key that is missing or not set.
 * <p>
 * {@link #bodyArgs} fills the placeholders of that translation. It is code-built only - a published row has no way to
 * name a component - so it is held transiently, the way a code-built response holds its gate.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConvNodeData {
    private String speaker = "";
    private String body = "";
    /** Translation key for {@link #body}. Blank to use the literal text. */
    private String bodyKey = "";
    /** Values for the placeholders of {@link #bodyKey}, in order. Empty when the line takes none. */
    @JsonIgnore
    private transient List<Component> bodyArgs = new ArrayList<>();
    private String font = "default";
    private int typewriterCps = 30;
    private String voiceLineKey = "";
    private int delayTicks = 0;
    private List<ConvResponse> responses = new ArrayList<>();
}
