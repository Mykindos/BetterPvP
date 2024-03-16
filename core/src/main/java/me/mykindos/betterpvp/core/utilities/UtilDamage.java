package me.mykindos.betterpvp.core.utilities;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilDamage {

    public static CustomDamageEvent doCustomDamage(CustomDamageEvent event) {

        PreCustomDamageEvent preCustomDamageEvent = UtilServer.callEvent(new PreCustomDamageEvent(event));
        if (!preCustomDamageEvent.isCancelled()) {
            return UtilServer.callEvent(event);
        }

        return null;
    }
}
