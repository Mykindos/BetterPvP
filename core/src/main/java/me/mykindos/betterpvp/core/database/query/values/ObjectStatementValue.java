package me.mykindos.betterpvp.core.database.query.values;

import me.mykindos.betterpvp.core.database.query.StatementValue;

import java.sql.Types;

public class ObjectStatementValue extends StatementValue<Object> {

    public ObjectStatementValue(Object value) {
        super(value);
    }

    @Override
    public int getType() {
        return Types.JAVA_OBJECT;
    }

}
