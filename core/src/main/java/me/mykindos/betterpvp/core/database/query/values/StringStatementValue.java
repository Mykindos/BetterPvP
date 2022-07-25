package me.mykindos.betterpvp.core.database.query.values;

import me.mykindos.betterpvp.core.database.query.StatementValue;

import java.sql.Types;

public class StringStatementValue extends StatementValue<String> {

    public StringStatementValue(String value) {
        super(value);
    }

    @Override
    public int getType() {
        return Types.VARCHAR;
    }

}
