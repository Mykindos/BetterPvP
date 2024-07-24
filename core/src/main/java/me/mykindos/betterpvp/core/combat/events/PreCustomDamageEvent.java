package me.mykindos.betterpvp.core.combat.events;

public class PreCustomDamageEvent extends PreDamageEvent {

    public PreCustomDamageEvent(CustomDamageEvent damageEvent) {
        super(damageEvent);
    }

    public CustomDamageEvent getCustomDamageEvent() {
        return (CustomDamageEvent) super.getDamageEvent();
    }
}
