package me.mykindos.betterpvp.champions.champions.npc;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.champions.builds.BuildManager;
import me.mykindos.betterpvp.champions.champions.builds.GamerBuilds;
import me.mykindos.betterpvp.champions.champions.builds.menus.BuildMenu;
import me.mykindos.betterpvp.champions.champions.skills.ChampionsSkillManager;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.combat.events.EntityCanHurtEntityEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.events.EntityProperty;
import me.mykindos.betterpvp.core.utilities.events.FetchNearbyEntityEvent;
import me.mykindos.betterpvp.core.utilities.events.GetEntityRelationshipEvent;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.GameMode;
import org.bukkit.Sound;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;

import java.util.Objects;
import java.util.WeakHashMap;

@BPvPListener
@Singleton
public class KitSelectorListener implements Listener {

    protected final WeakHashMap<Entity, KitSelector> selectors = new WeakHashMap<>();
    private final ItemFactory itemFactory;
    private final BuildManager buildManager;
    private final ChampionsSkillManager skillManager;

    @Inject
    private KitSelectorListener(ItemFactory itemFactory, BuildManager buildManager, ChampionsSkillManager skillManager) {
        this.itemFactory = itemFactory;
        this.buildManager = buildManager;
        this.skillManager = skillManager;
    }

    @EventHandler
    public void onDamage(DamageEvent event) {
        if (selectors.containsKey(event.getDamagee())) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!selectors.containsKey(event.getRightClicked()) || event.getPlayer().getGameMode() == GameMode.SPECTATOR) {
            return;
        }

        event.setCancelled(true);

        // Equip
        final KitSelector selector = selectors.get(event.getRightClicked());
        final Role role = selector.getRole();
        final Player player = event.getPlayer();

        final KitSelectorUseEvent useEvent = new KitSelectorUseEvent(event.getPlayer(), selector);
        useEvent.callEvent();
        if (useEvent.isCancelled()) {
            return;
        }

        if (selector.isEquip()) {
            // Equip them
            role.equip(itemFactory, player, true);
            new SoundEffect(Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f).play(player);
        }

        if (selector.isEditor()) {
            // Open editor
            final GamerBuilds builds = buildManager.getObject(player.getUniqueId()).orElseThrow();

            BuildMenu gui = selector.getBuildMenuFunction() == null ? null : selector.getBuildMenuFunction().apply(player);
            Objects.requireNonNullElseGet(gui, () -> new BuildMenu(builds, role, this.buildManager, this.skillManager, null, null)).show(player);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onRelation(GetEntityRelationshipEvent event) {
        if (!selectors.containsKey(event.getTarget())) {
            return;
        }

        event.setEntityProperty(EntityProperty.ALL);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onTarget(EntityCanHurtEntityEvent event) {
        if (!selectors.containsKey(event.getDamagee())) {
            return;
        }

        event.setResult(Event.Result.DENY);
    }

    // Remove dead players from nearby entity fetches, like AoE skills
    @EventHandler(priority = EventPriority.LOWEST)
    public void onFetchNearby(FetchNearbyEntityEvent<?> event) {
        event.getEntities().removeIf(pair -> selectors.containsKey(pair.get()));
    }
}