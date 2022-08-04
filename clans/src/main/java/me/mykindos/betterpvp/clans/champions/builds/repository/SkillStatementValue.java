package me.mykindos.betterpvp.clans.champions.builds.repository;

import me.mykindos.betterpvp.clans.champions.builds.BuildSkill;
import me.mykindos.betterpvp.core.database.query.StatementValue;

import java.sql.Types;

public class SkillStatementValue extends StatementValue<String> {

    public SkillStatementValue(BuildSkill buildSkill) {
        super((buildSkill == null || buildSkill.getSkill() == null) ? "" : buildSkill.getSkill().getName() + "," + buildSkill.getLevel());
    }

    @Override
    public int getType() {
        return Types.VARCHAR;
    }
}
