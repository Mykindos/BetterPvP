package me.mykindos.betterpvp.core.world.settler.recruit;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.mykindos.betterpvp.core.world.settler.Settler;

/** Someone who could join a site, what they ask to, and until when they wait. */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlerCandidate {

    private Settler settler;
    /** Coins asked before any discount, 0 for free. */
    private long price;
    /** When they give up waiting, or 0 for never. */
    private long expiresAt;

    public boolean isExpired(long now) {
        return expiresAt > 0 && now >= expiresAt;
    }
}
