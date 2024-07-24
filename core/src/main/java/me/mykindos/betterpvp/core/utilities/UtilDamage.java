package me.mykindos.betterpvp.core.utilities;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreCustomDamageEvent;
import me.mykindos.betterpvp.core.combat.events.PreDamageEvent;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class UtilDamage {

    public static CustomDamageEvent doCustomDamage(CustomDamageEvent event) {

        PreCustomDamageEvent preCustomDamageEvent = UtilServer.callEvent(new PreCustomDamageEvent(event));
        if (!preCustomDamageEvent.isCancelled()) {
            return UtilServer.callEvent(event);
        }

        return null;
    }

    public static DamageEvent doDamage(DamageEvent event) {

        PreDamageEvent preDamageEvent = UtilServer.callEvent(new PreDamageEvent(event));
        if (!preDamageEvent.isCancelled()) {
            return UtilServer.callEvent(event);
        }

        return null;
    }
}
