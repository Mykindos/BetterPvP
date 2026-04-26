package me.mykindos.betterpvp.core.item.component.impl.access;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.access.AccessRequirement;
import me.mykindos.betterpvp.core.access.AccessScope;
import me.mykindos.betterpvp.core.access.ItemAccessService;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.ItemRegistry;
import me.mykindos.betterpvp.core.item.component.AbstractItemComponent;
import me.mykindos.betterpvp.core.item.component.ItemComponent;
import me.mykindos.betterpvp.core.item.component.LoreComponent;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.NamespacedKey;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Declares which {@link AccessScope}s are enforced for this item and renders requirement lore lines.
 *
 * <p>Lore rendering note: {@link LoreComponent#getLines(ItemInstance)} does not receive a Player
 * reference. Tracing the call chain confirms this signature is fixed (it is called from
 * {@code ItemLoreRenderer} which has no viewer). Therefore this component renders requirement
 * text without ✓/✗ satisfaction indicators; denial feedback is handled by the access listeners
 * at interaction time instead.</p>
 */
public class RestrictedAccessComponent extends AbstractItemComponent implements LoreComponent {

    private final Set<AccessScope> enforcedScopes;

    public RestrictedAccessComponent(Set<AccessScope> enforcedScopes) {
        super("restricted_access");
        this.enforcedScopes = Set.copyOf(enforcedScopes);
    }

    public Set<AccessScope> getEnforcedScopes() {
        return enforcedScopes;
    }

    @Override
    public ItemComponent copy() {
        return new RestrictedAccessComponent(enforcedScopes);
    }

    @Override
    public List<Component> getLines(ItemInstance item) {
        NamespacedKey nsk = resolveKey(item);
        if (nsk == null) {
            return List.of();
        }

        Key key = Key.key(nsk.namespace(), nsk.getKey());

        ItemAccessService service = resolveService();
        if (service == null) {
            return List.of();
        }

        // No Player available at lore-render time — render requirement text without ✓/✗.
        // Satisfaction indicators are provided at interaction time via listener denial messages.
        List<AccessRequirement> requirements = service.getRequirements(null, key);
        if (requirements.isEmpty()) {
            return List.of();
        }

        List<Component> lines = new ArrayList<>();
        for (AccessRequirement req : requirements) {
            boolean intersects = req.gatedScopes().stream().anyMatch(enforcedScopes::contains);
            if (!intersects) continue;
            lines.add(req.lore().colorIfAbsent(NamedTextColor.RED));
        }
        return lines;
    }

    /**
     * Render priority sits just below rarity tag (which uses -1 in InteractionContainerComponent).
     * Using -2 places this above rarity but below standard descriptions.
     */
    @Override
    public int getRenderPriority() {
        return Integer.MAX_VALUE;
    }

    private NamespacedKey resolveKey(ItemInstance item) {
        try {
            ItemRegistry registry = JavaPlugin.getPlugin(Core.class).getInjector().getInstance(ItemRegistry.class);
            return registry.getKey(item.getBaseItem());
        } catch (Exception e) {
            return null;
        }
    }

    private ItemAccessService resolveService() {
        try {
            return JavaPlugin.getPlugin(Core.class).getInjector().getInstance(ItemAccessService.class);
        } catch (Exception e) {
            return null;
        }
    }
}
