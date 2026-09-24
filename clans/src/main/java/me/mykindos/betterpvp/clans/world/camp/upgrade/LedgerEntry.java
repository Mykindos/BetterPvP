package me.mykindos.betterpvp.clans.world.camp.upgrade;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One line of a camp's ledger: who did what and when. What happened is kept as a kind and its arguments, and only
 * turned into words when the ledger is read.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LedgerEntry {

    /** When it happened, in epoch milliseconds. */
    private long at;

    private UUID member;

    /** The member's name when it happened. */
    private String memberName;

    private Kind kind;

    /**
     * What the kind needs to be described. {@link Kind#DEPOSIT}: resource id and amount pairs. {@link Kind#CLAIM}: the
     * job kind, the structure type and, for an advance or an upgrade, the stage or upgrade id. {@link Kind#HIRE}: the
     * settler's name and the price. {@link Kind#WAGES}: the amount.
     */
    private List<String> args = new ArrayList<>();

    public enum Kind {
        DEPOSIT,
        CLAIM,
        HIRE,
        WAGES
    }
}
