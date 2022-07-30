package me.mykindos.betterpvp.clans.skills;

import lombok.Setter;

public abstract class Skill implements ISkill {

    @Setter
    private boolean enabled;

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
