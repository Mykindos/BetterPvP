package me.mykindos.betterpvp.core.parties.events;

import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.core.framework.events.CustomCancellableEvent;
import me.mykindos.betterpvp.core.parties.Party;
import me.mykindos.betterpvp.core.parties.PartyMemberFilter;

@EqualsAndHashCode(callSuper = true)
@Data
public class PartyCreateEvent extends CustomCancellableEvent {

    private final Party party;
    private final PartyMemberFilter filter;

}
