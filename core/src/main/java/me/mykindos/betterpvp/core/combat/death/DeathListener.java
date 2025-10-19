package me.mykindos.betterpvp.core.combat.death;

import com.google.inject.Inject;
import io.papermc.paper.datacomponent.DataComponentTypes;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLog;
import me.mykindos.betterpvp.core.combat.damagelog.DamageLogManager;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathEvent;
import me.mykindos.betterpvp.core.combat.death.events.CustomDeathMessageEvent;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilFormat;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedDeque;

@BPvPListener
public class DeathListener implements Listener {

    private final ItemFactory itemFactory;
    private final DamageLogManager damageLogManager;
    private final ClientManager clientManager;

    @Inject
    public DeathListener(ItemFactory itemFactory, DamageLogManager damageLogManager, ClientManager clientManager) {
        this.itemFactory = itemFactory;
        this.damageLogManager = damageLogManager;
        this.clientManager = clientManager;
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        event.deathMessage(null);
        DamageLog lastDamage = damageLogManager.getLastDamager(event.getPlayer());

        final CustomDeathEvent deathEvent = new CustomDeathEvent(event.getPlayer());
        if (lastDamage != null) {
            deathEvent.setKiller(lastDamage.getDamager());
            deathEvent.setReason(lastDamage.getReason());
        }
        deathEvent.callEvent();

        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> {
            CustomDeathMessageEvent customDeathMessageEvent = new CustomDeathMessageEvent(onlinePlayer, deathEvent);
            UtilServer.callEvent(customDeathMessageEvent);
        });
    }



    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        final Client client = clientManager.search().online(event.getPlayer());
        final Gamer gamer = client.getGamer();
        gamer.setLastDamaged(0);
        gamer.setLastDeath(System.currentTimeMillis());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onCustomDeath(CustomDeathMessageEvent event) {
        final String[] reasonRaw = Objects.requireNonNullElse(event.getReason(), new String[]{});
        Component[] reasons = Arrays.stream(reasonRaw).map(text -> Component.text(text, NamedTextColor.GREEN)).toArray(Component[]::new);
        Component reason = Component.join(JoinConfiguration.separator(Component.text(", ", NamedTextColor.GRAY)), reasons).applyFallbackStyle(NamedTextColor.GRAY);
        Component message;
        final Component killedName = event.getKilledName().applyFallbackStyle(NamedTextColor.YELLOW);
        if (event.getKiller() == null) {
            if (reasons.length == 0) {
                final Optional<ConcurrentLinkedDeque<DamageLog>> damageLog = damageLogManager.getObject(event.getDeathEvent().getKilled().getUniqueId());
                if (damageLog.isPresent() && !damageLog.get().isEmpty()) {
                    final DamageLog lastDamage = damageLog.get().getLast();
                    final TextComponent name = Component.text(lastDamage.getDamageCause().getDisplayName());
                    message = killedName.append(Component.text(" was killed by ", NamedTextColor.GRAY)).append(name);
                } else {
                    message = killedName.append(Component.text(" was killed", NamedTextColor.GRAY));
                }
            } else {
                message = killedName.append(Component.text(" was killed by ", NamedTextColor.GRAY)).append(reason);
            }
        } else {
            final Component killerName = event.getKillerName().applyFallbackStyle(NamedTextColor.YELLOW);
            LivingEntity killer = event.getKiller();
            boolean with = false;
            if (killer instanceof Player) {
                ItemStack itemStack = killer.getEquipment().getItemInMainHand();
                if (reasons.length == 0 && itemStack.getType() != Material.AIR) {

                    final Optional<ItemInstance> instanceOpt = itemFactory.fromItemStack(itemStack);
                    if (instanceOpt.isPresent()) {
                        itemStack = instanceOpt.get().getView().get();
                    }

                    final Component name;
                    if (itemStack.getItemMeta().hasDisplayName()) {
                        name = Objects.requireNonNull(itemStack.getItemMeta().displayName());
                    } else {
                        name = Objects.requireNonNullElse(itemStack.getData(DataComponentTypes.ITEM_NAME),
                                Component.translatable(itemStack.getType().translationKey()));
                    }

                    String namePlainText = PlainTextComponentSerializer.plainText().serialize(name).replaceAll("[\\[\\]]", "");
                    final String article = UtilFormat.getIndefiniteArticle(namePlainText);
                    reason = Component.text(article + " ", NamedTextColor.GRAY).append(name.hoverEvent(itemStack));
                    with = true;
                }
            }

            if (reasons.length == 0 && !with) {
                if (!(event.getKiller() instanceof Player)) {
                    final Component customName = event.getKiller().customName();
                    if (customName == null) {
                        // Better english if the killer doesn't have a custom name
                        message = killedName
                                .append(Component.text(" was killed by a ", NamedTextColor.GRAY)
                                        .append(killerName.applyFallbackStyle(NamedTextColor.YELLOW)));
                    } else {
                        message = killedName
                                .append(Component.text(" was killed by ", NamedTextColor.GRAY))
                                .append(customName.color(NamedTextColor.YELLOW));
                    }
                } else {
                    message = killedName
                            .append(Component.text(" was killed by ", NamedTextColor.GRAY))
                            .append(killerName);
                }
            } else {
                message = killedName.append(Component.text(" was killed by ", NamedTextColor.GRAY))
                        .append(killerName.applyFallbackStyle(NamedTextColor.YELLOW));

                if (killer instanceof Player) {
                    message = message.append(Component.text(" with ", NamedTextColor.GRAY))
                            .append(reason);
                }
            }
        }

        message = Component.text("").applyFallbackStyle(NamedTextColor.GRAY).append(message).append(Component.text("."));
        Component hoverComponent = Component.text("Damage Breakdown", NamedTextColor.GOLD).appendNewline();
        for (var breakdown : damageLogManager.getDamageBreakdown(event.getKilled())) {
            final String round = breakdown.getValue() == Double.POSITIVE_INFINITY
                    ? "∞"
                    : String.format("%.1f", breakdown.getValue());
            hoverComponent = hoverComponent.append(Component.text(breakdown.getKey() + ": ", NamedTextColor.YELLOW)
                    .append(Component.text(round, NamedTextColor.GREEN))).appendNewline();
        }

        UtilMessage.simpleMessage(event.getReceiver(), "Death", message, hoverComponent);
    }
}
