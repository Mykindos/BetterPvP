package me.mykindos.betterpvp.core.quest.conversation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseItemEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.interaction.event.InteractionPreExecuteEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Conversation input: the hotbar scroll wheel moves the response cursor (consumed
 * so the held item never actually changes); the off-hand key (F) confirms via the
 * high-priority {@link me.mykindos.betterpvp.core.combat.offhand.OffhandExecutor}
 * that {@link ConversationManager} registers for the session.
 * <p>
 * Also locks the player down for the duration: world/entity interaction, skills,
 * item abilities, and incoming damage are all cancelled.
 */
@Singleton
@BPvPListener
public class ConversationListener implements Listener {

    private final ConversationManager manager;

    @Inject
    public ConversationListener(ConversationManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onScroll(PlayerItemHeldEvent event) {
        final Player player = event.getPlayer();
        if (!manager.inConversation(player)) return;
        event.setCancelled(true);

        final int newSlot = event.getNewSlot();
        if (newSlot <= 2) {
            // Hotbar keys 1-3 jump straight to that response.
            manager.select(player, newSlot);
            return;
        }
        // The cursor is pinned to slot 6 (index 5), so any other slot change is a scroll relative to it.
        int direction = Integer.signum(newSlot - 5);
        if (direction != 0) manager.scroll(player, direction);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (!manager.inConversation(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onInteractEntity(PlayerInteractEntityEvent event) {
        if (!manager.inConversation(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onUseSkill(PlayerUseSkillEvent event) {
        if (!manager.inConversation(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onUseItem(PlayerUseItemEvent event) {
        if (!manager.inConversation(event.getPlayer())) return;
        event.setCancelled(true);
    }

    @EventHandler
    public void onItemAbility(InteractionPreExecuteEvent event) {
        if (!(event.getActor().getEntity() instanceof Player player)) return;
        if (!manager.inConversation(player)) return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onDamage(DamageEvent event) {
        if (event.getDamagee() instanceof Player damagee && manager.inConversation(damagee)) {
            event.setCancelled(true);
            return;
        }
        if (event.getDamager() instanceof Player damager && manager.inConversation(damager)) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        manager.end(event.getPlayer());
    }
}
