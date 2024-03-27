package me.mykindos.betterpvp.core.logging.type;

import lombok.Getter;
import me.mykindos.betterpvp.core.database.query.Statement;
import me.mykindos.betterpvp.core.logging.FormattedLog;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class FormattedItemLog extends FormattedLog {
    private List<Statement> locationStatements = new ArrayList<>();
    private UUIDLogType type;
    private UUID item;


    public FormattedItemLog(long time) {
        super(time);
    }


}
