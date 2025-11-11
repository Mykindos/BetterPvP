package me.mykindos.betterpvp.core.combat.adapters;

import me.mykindos.betterpvp.core.combat.events.DamageEvent;

public interface CustomDamageAdapter {

    boolean isValid(DamageEvent event);
    boolean processPreCustomDamage(DamageEvent event);
    boolean processCustomDamageAdapter(DamageEvent event);

}
