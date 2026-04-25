package me.mykindos.betterpvp.core.access;

import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.component.impl.access.RestrictedAccessComponent;
import net.kyori.adventure.key.Key;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central service for evaluating item access across multiple {@link AccessScope}s.
 *
 * <p>A scope is enforced for an item if and only if the item's {@link RestrictedAccessComponent}
 * lists that scope in its {@code enforcedScopes} set. All scopes — including CRAFT — follow this
 * same rule; there are no exceptions.</p>
 *
 * <p>Providers are registered here and consulted in order when {@link #isAllowed} or
 * {@link #getRequirements} is called. A scope is considered blocked if at least one registered
 * provider returns an unsatisfied requirement that covers that scope.</p>
 */
@Singleton
public class ItemAccessService {

    private final List<ItemAccessProvider> providers = new CopyOnWriteArrayList<>();

    public void register(ItemAccessProvider provider) {
        providers.add(provider);
    }

    public void unregister(ItemAccessProvider provider) {
        providers.remove(provider);
    }

    /**
     * Returns {@code true} if the item's component does not enforce {@code scope}, or every
     * provider that gates the scope is satisfied.
     */
    public boolean isAllowed(@Nullable Player player, @NotNull BaseItem item, @NotNull Key itemKey, @NotNull AccessScope scope) {
        if (!isEnforced(item, scope)) return true;
        for (ItemAccessProvider provider : providers) {
            Optional<AccessRequirement> req = provider.evaluate(player, itemKey);
            if (req.isEmpty()) continue;
            AccessRequirement r = req.get();
            if (r.gatedScopes().contains(scope) && !r.satisfied()) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns the first unsatisfied requirement that gates the requested scope, if any.
     * Returns empty immediately if the item's component does not enforce {@code scope}.
     */
    public Optional<AccessRequirement> firstBlocker(@Nullable Player player, @NotNull BaseItem item, @NotNull Key itemKey, @NotNull AccessScope scope) {
        if (!isEnforced(item, scope)) return Optional.empty();
        for (ItemAccessProvider provider : providers) {
            Optional<AccessRequirement> req = provider.evaluate(player, itemKey);
            if (req.isEmpty()) continue;
            AccessRequirement r = req.get();
            if (r.gatedScopes().contains(scope) && !r.satisfied()) {
                return Optional.of(r);
            }
        }
        return Optional.empty();
    }

    /**
     * Returns all requirements that any provider associates with the given player + item key,
     * regardless of satisfaction state. Used to build lore lines.
     *
     * <p>When {@code player} is {@code null} (lore rendering without a viewer), providers that
     * require a player to evaluate should return a requirement with {@code satisfied=false}.</p>
     */
    public List<AccessRequirement> getRequirements(@Nullable Player player, @NotNull Key itemKey) {
        return providers.stream()
                .map(p -> p.evaluate(player, itemKey))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();
    }

    private boolean isEnforced(@NotNull BaseItem item, @NotNull AccessScope scope) {
        return item.getComponent(RestrictedAccessComponent.class)
                .map(c -> c.getEnforcedScopes().contains(scope))
                .orElse(false);
    }
}
