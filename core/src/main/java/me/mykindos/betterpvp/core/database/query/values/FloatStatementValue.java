package me.mykindos.betterpvp.core.database.query.values;

import me.mykindos.betterpvp.core.database.query.StatementValue;

import java.sql.Types;

public class FloatStatementValue extends StatementValue<Float> {

    public FloatStatementValue(Float value) {
        super(value);
    }

    @Override
    public int getType() {
        return Types.FLOAT;
    }

}
