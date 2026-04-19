package me.mykindos.betterpvp.progression.profession.skill;

import com.google.common.base.Preconditions;

public class ProfessionSkillNode extends ProfessionNode {

    public ProfessionSkillNode(String id, ProfessionSkill skill) {
        super(id);
        this.skill = Preconditions.checkNotNull(skill);
    }

    @Override
    public boolean isGlowing() {
        return false;
    }
}
