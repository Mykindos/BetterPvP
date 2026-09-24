package me.mykindos.betterpvp.clans.world.camp.upgrade;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/** Where each player's last death is kept, one per player, readable from any server. */
public interface CasualtyStore {

    /** Records {@code casualty} as its member's last death, replacing what was there. */
    @NotNull CompletableFuture<Void> save(@NotNull Casualty casualty);

    /** The last death of each of {@code members} that has one. */
    @NotNull CompletableFuture<Map<UUID, Casualty>> lastDeaths(@NotNull Collection<UUID> members);
}
