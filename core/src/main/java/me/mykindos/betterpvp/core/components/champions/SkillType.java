package me.mykindos.betterpvp.core.components.champions;

public enum SkillType {
    SWORD, AXE, BOW, PASSIVE_A, PASSIVE_B, GLOBAL, TRAIT;

    public boolean isPassive() {
        return this == PASSIVE_A || this == PASSIVE_B;
    }

    /**
     * Whether a player picks this skill themselves. Traits are innate to a role, so they occupy no build
     * slot, cost no skill points, and never appear in the skill menu.
     *
     * @return true if the skill can be equipped into a build
     */
    public boolean isSelectable() {
        return this != TRAIT;
    }
}
