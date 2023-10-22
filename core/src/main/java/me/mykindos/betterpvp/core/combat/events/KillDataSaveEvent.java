package me.mykindos.betterpvp.core.combat.events;

import me.mykindos.betterpvp.core.combat.stats.Kill;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.framework.events.CustomEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class KillDataSaveEvent extends CustomEvent {

    private final List<Statement> statements = new ArrayList<>();
    private final Kill kill;

    public KillDataSaveEvent(Kill kill) {
        this.kill = kill;
    }

    public Kill getKillData() {
        return kill;
    }

    public void addStatement(Statement statement) {
        statements.add(statement);
    }

    public void addStatement(List<Statement> statement) {
        statements.addAll(statement);
    }

    public List<Statement> getStatements() {
        return Collections.unmodifiableList(statements);
    }
}
