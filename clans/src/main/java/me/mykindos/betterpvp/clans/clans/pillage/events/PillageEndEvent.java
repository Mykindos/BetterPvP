package me.mykindos.betterpvp.clans.clans.pillage.events;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.clans.clans.pillage.Pillage;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class PillageEndEvent extends CustomEvent {

    private final Pillage pillage;
    
}
