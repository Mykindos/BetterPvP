package me.mykindos.betterpvp.core.quest.conversation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * What a {@link ConvResponse} does to conversation flow when chosen. A tagged
 * union keyed by {@code kind}; only the field for that kind is populated:
 * <ul>
 *   <li>{@code goto} → branch to {@link #target} (a node id)</li>
 *   <li>{@code end} → terminate the conversation</li>
 *   <li>{@code start_conversation} → hand off to {@link #conversationId}</li>
 *   <li>{@code start_cinematic} → hand off to {@link #cinematicId}</li>
 * </ul>
 * Modelled flat (rather than Jackson polymorphism) to match the console's JSON
 * and the loose {@code @JsonIgnoreProperties} style used across this package.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConvOutcome {
    private String kind = "end";
    private String target;
    private String conversationId;
    private String cinematicId;
}
