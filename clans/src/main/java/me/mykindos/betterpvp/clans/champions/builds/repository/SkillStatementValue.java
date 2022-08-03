package me.mykindos.betterpvp.clans.champions.builds.repository;

import me.mykindos.betterpvp.clans.champions.builds.BuildSkill;
import me.mykindos.betterpvp.clans.champions.builds.RoleBuild;
import me.mykindos.betterpvp.core.database.query.StatementValue;

import java.sql.Types;

public class SkillStatementValue extends StatementValue<String> {

    public SkillStatementValue(BuildSkill skill) {
        super(skill == null ? "" : skill.getSkill().getName() + "," + skill.getLevel());
    }

    @Override
    public int getType() {
        return Types.VARCHAR;
    }
}
