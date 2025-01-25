package me.mykindos.betterpvp.core.framework.blocktag;

import lombok.Data;

import java.time.Instant;

@Data
public class BlockTag {

    private final String tag;
    private final String value;
    private Instant lastUpdated;

    public int getAsInt() {
        return Integer.parseInt(value);
    }

    public double getAsDouble() {
        return Double.parseDouble(value);
    }

    public boolean getAsBoolean() {
        return Boolean.parseBoolean(value);
    }

}
