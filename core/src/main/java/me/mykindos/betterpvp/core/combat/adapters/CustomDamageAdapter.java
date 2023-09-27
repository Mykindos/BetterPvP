package me.mykindos.betterpvp.core.combat.adapters;

import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;

public interface CustomDamageAdapter {
    boolean processCustomDamageAdapter(CustomDamageEvent event, double damage);

    void processKnockbackAdapter(CustomDamageEvent event, boolean knockback);

}
