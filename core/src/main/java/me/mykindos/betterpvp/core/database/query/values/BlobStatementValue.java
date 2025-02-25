package me.mykindos.betterpvp.core.database.query.values;

import me.mykindos.betterpvp.core.database.query.StatementValue;

import java.sql.Types;

public class BlobStatementValue extends StatementValue<byte[]> {

    public BlobStatementValue(byte[] value) {
        super(value);
    }

    @Override
    public int getType() {
        return Types.BLOB;
    }

}
