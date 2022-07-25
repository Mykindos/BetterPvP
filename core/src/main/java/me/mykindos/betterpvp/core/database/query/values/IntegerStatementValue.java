package me.mykindos.betterpvp.core.database.query.values;

import me.mykindos.betterpvp.core.database.query.StatementValue;

import java.sql.Types;

public class IntegerStatementValue extends StatementValue<Integer> {

    public IntegerStatementValue(Integer value) {
        super(value);
    }

    public IntegerStatementValue(String value) {
        super(Integer.parseInt(value));
    }

    @Override
    public int getType() {
        return Types.INTEGER;
    }

}
