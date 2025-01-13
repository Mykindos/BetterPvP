package me.mykindos.betterpvp.champions.champions.builds.repository;

import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.core.database.query.StatementValue;

import java.sql.Types;

public class SkillStatementValue extends StatementValue<String> {

    public SkillStatementValue(Skill skill) {
        super(skill == null ? "" : skill.getName());
    }

    @Override
    public int getType() {
        return Types.VARCHAR;
    }
}
