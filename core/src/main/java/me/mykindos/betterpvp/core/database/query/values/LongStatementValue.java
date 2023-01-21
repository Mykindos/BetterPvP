package me.mykindos.betterpvp.core.database.query.values;

import me.mykindos.betterpvp.core.database.query.StatementValue;

import java.sql.Types;

public class LongStatementValue extends StatementValue<Long> {

    public LongStatementValue(Long value) {
        super(value);
    }

    @Override
    public int getType() {
        return Types.BIGINT;
    }

}
