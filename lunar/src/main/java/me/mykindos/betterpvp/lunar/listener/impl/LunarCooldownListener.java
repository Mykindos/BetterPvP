package me.mykindos.betterpvp.lunar.listener.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.lunarclient.apollo.Apollo;
import com.lunarclient.apollo.common.icon.ItemStackIcon;
import com.lunarclient.apollo.module.cooldown.Cooldown;
import com.lunarclient.apollo.module.cooldown.CooldownModule;
import com.lunarclient.apollo.player.ApolloPlayer;
import me.mykindos.betterpvp.core.cooldowns.events.CooldownEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import java.time.Duration;
import java.util.Optional;

@Singleton
@BPvPListener
public class LunarCooldownListener implements Listener {

    private final CooldownModule cooldownModule;

    @Inject
    private LunarCooldownListener() {
        this.cooldownModule = Apollo.getModuleManager().getModule(CooldownModule.class);
        this.cooldownModule.enable();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onCooldown(CooldownEvent event) {
        if (event.isCancelled()) {
            return;
        }

        final me.mykindos.betterpvp.core.cooldowns.Cooldown cooldown = event.getCooldown();
        if (!cooldown.isInform() || !cooldown.isRemoveOnDeath()) {
            return;
        }

        final Optional<ApolloPlayer> apolloPlayerOptional = Apollo.getPlayerManager().getPlayer(event.getPlayer().getUniqueId());
        if (apolloPlayerOptional.isEmpty()) {
            return;
        }

        final Cooldown.CooldownBuilder builder = Cooldown.builder()
                .name(cooldown.getName())
                .duration(Duration.ofMillis((long) cooldown.getSeconds()));

        final ItemStack item = event.getPlayer().getInventory().getItemInMainHand();
        if (!item.getType().equals(Material.AIR)) {
            builder.icon(ItemStackIcon.builder()
                    .itemName(item.getType().name())
                    .customModelData(item.hasItemMeta() && item.getItemMeta().hasCustomModelData() ? item.getItemMeta().getCustomModelData() : 0)
                    .build());
        } else {
            builder.icon(ItemStackIcon.builder()
                    .itemName("BEDROCK")
                    .build());
        }

        this.cooldownModule.displayCooldown(apolloPlayerOptional.get(), builder.build());
    }

}
