package me.mykindos.betterpvp.core.database.query.values;

import me.mykindos.betterpvp.core.database.query.StatementValue;

import java.sql.Types;

public class DoubleStatementValue extends StatementValue<Double> {

    public DoubleStatementValue(Double value) {
        super(value);
    }

    @Override
    public int getType() {
        return Types.DOUBLE;
    }

}
