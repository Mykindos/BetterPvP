package me.mykindos.betterpvp.core.framework.events.dungeons;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;
import me.mykindos.betterpvp.core.parties.Party;

@EqualsAndHashCode(callSuper = true)
@Data
public class InstanceStartEvent extends CustomEvent {

    private final String instanceName;
    private final Party party;

}
