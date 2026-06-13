package me.mykindos.betterpvp.core.quest.conversation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/** A single line of dialogue plus the responses the player may pick from it. */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ConvNodeData {
    private String speaker = "";
    private String body = "";
    private String font = "default";
    private int typewriterCps = 30;
    private String voiceLineKey = "";
    private int delayTicks = 0;
    private List<ConvResponse> responses = new ArrayList<>();
}
