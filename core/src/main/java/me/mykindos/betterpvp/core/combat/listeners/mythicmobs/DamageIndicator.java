package me.mykindos.betterpvp.core.combat.listeners.mythicmobs;

import org.bukkit.entity.TextDisplay;

public record DamageIndicator(TextDisplay display, TextDisplay shadow, double damage, long timestamp) {

    public boolean isValid() {
        return display != null && display.isValid() && shadow != null && shadow.isValid();
    }

    public void despawn() {
        if (!isValid()){
            return;
        }
        display.remove();
        shadow.remove();
    }

}
