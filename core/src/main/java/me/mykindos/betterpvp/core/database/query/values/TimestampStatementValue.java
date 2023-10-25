package me.mykindos.betterpvp.core.database.query.values;

import me.mykindos.betterpvp.core.database.query.StatementValue;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;

public class TimestampStatementValue extends StatementValue<Timestamp> {

    public TimestampStatementValue(Timestamp value) {
        super(value);
    }

    public TimestampStatementValue(Instant value) {
        this(Timestamp.from(value));
    }

    @Override
    public int getType() {
        return Types.TIMESTAMP;
    }

}
