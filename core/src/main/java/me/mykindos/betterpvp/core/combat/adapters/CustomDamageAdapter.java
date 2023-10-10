package me.mykindos.betterpvp.core.combat.adapters;

import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;

public interface CustomDamageAdapter {

    boolean isValid(CustomDamageEvent event);
    boolean processPreCustomDamage(CustomDamageEvent event);
    boolean processCustomDamageAdapter(CustomDamageEvent event);

}
