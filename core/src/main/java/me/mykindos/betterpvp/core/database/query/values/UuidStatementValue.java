package me.mykindos.betterpvp.core.database.query.values;

import me.mykindos.betterpvp.core.database.query.StatementValue;

import java.sql.Types;
import java.util.UUID;

public class UuidStatementValue extends StatementValue<String> {

    public UuidStatementValue(UUID value) {
        super(value.toString());
    }

    @Override
    public int getType() {
        return Types.VARCHAR;
    }

}
