package me.mykindos.betterpvp.core.database.query.values;

import me.mykindos.betterpvp.core.database.query.StatementValue;
import org.jetbrains.annotations.Nullable;

import java.sql.Types;
import java.util.UUID;

public class UuidStatementValue extends StatementValue<String> {

    public UuidStatementValue(@Nullable UUID value) {
        super(value == null ? null : value.toString());
    }

    @Override
    public int getType() {
        return Types.VARCHAR;
    }

}
