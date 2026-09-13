package me.mykindos.betterpvp.core.world.site;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Decides whether a party may enter an instance that already exists. Choosing between instances that would all take
 * them is {@link Selection}.
 */
@FunctionalInterface
public interface Admission {

    boolean admits(@NotNull SiteKey key, @NotNull Set<UUID> occupants, @NotNull Party party);

    /**
     * Anybody may join anybody. Capacity is enforced by the caller.
     */
    static @NotNull Admission open() {
        return (key, occupants, party) -> true;
    }

    /**
     * Only somebody already travelling with an occupant may follow them in.
     */
    static @NotNull Admission party() {
        return (key, occupants, party) -> occupants.isEmpty() || party.overlaps(occupants);
    }

    /**
     * Nobody joins an occupied instance. Every party gets its own.
     */
    static @NotNull Admission privateToParty() {
        return (key, occupants, party) -> occupants.isEmpty();
    }

    /**
     * Resolves a rule by its config name. A module contributes rules that depend on its own domain, such as clan
     * membership, through {@link #register(String, Admission)}.
     *
     * @return the named rule, or {@code null} if nothing has registered it
     */
    static @Nullable Admission byName(@NotNull String name) {
        return Registry.RULES.get(name.toLowerCase());
    }

    static void register(@NotNull String name, @NotNull Admission admission) {
        Registry.RULES.put(name.toLowerCase(), admission);
    }

    final class Registry {

        private static final Map<String, Admission> RULES = new ConcurrentHashMap<>(Map.of(
                "open", open(),
                "party", party(),
                "private", privateToParty()));

        private Registry() {
        }
    }
}
