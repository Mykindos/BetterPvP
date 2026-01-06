package me.mykindos.betterpvp.champions.item.ability;

import lombok.Setter;
import lombok.Value;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.roles.RoleManager;
import me.mykindos.betterpvp.champions.item.component.storage.ArmorStorageComponent;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.gamer.Gamer;
import me.mykindos.betterpvp.core.combat.events.CustomEntityVelocityEvent;
import me.mykindos.betterpvp.core.combat.events.DamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseInteractSkillEvent;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseToggleSkillEvent;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerTypes;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilServer;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTeleportEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;

import java.util.Objects;
import java.util.Optional;
import java.util.WeakHashMap;

public class PortableClassAbility extends ItemAbility implements Listener {

    private static final NamespacedKey KEY = new NamespacedKey("betterpvp", "portable_class");

    @Setter
    private double castTime;
    private final Role role;
    private final RoleManager roleManager;
    private final Champions champions;
    private final WeakHashMap<Player, Data> tasks = new WeakHashMap<>();

    public PortableClassAbility(Role role) {
        super(KEY,
                "Equip",
                "Instantly swaps to the " + role.getName() + " class. Does not work in combat. Armor stored on this item will be equipped.",
                TriggerTypes.RIGHT_CLICK);
        this.role = role;
        this.champions = JavaPlugin.getPlugin(Champions.class);
        this.roleManager = champions.getInjector().getInstance(RoleManager.class);
        Bukkit.getPluginManager().registerEvents(this, champions);
        UtilServer.runTaskTimer(champions, this::onTick, 0L, 1L);
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        final Gamer gamer = client.getGamer();
        final Player player = Objects.requireNonNull(gamer.getPlayer());
        if (gamer.isInCombat() || roleManager.getRole(player) == role) {
            new SoundEffect(Sound.ENTITY_BEE_STING, 0f, 1f).play(player);
            return false; // They're already the role or they're in combat
        }

        schedule(player, itemInstance);
        return true;
    }

    private void cancel(Player player) {
        final Data data = tasks.remove(player);
        if (data == null) {
            return;
        }

        if (data.task != null) {
            data.task.cancel();
        }

        if (!player.isValid()) {
            data.location.getWorld().dropItemNaturally(data.location, data.itemStack);
        } else {
            UtilItem.insert(player, data.itemStack);
        }
        new SoundEffect(Sound.ENTITY_BEE_STING, 0f, 1f).play(player);
    }

    private void schedule(Player player, ItemInstance itemInstance) {
        player.setVelocity(new Vector());

        final Location start = player.getLocation();
        final BukkitTask task = new BukkitRunnable() {
            int ticks = 0;
            @Override
            public void run() {
                ticks++;

                if (!player.isValid()) {
                    tasks.remove(player);
                    cancel();
                    return;
                }

                // Sounds
                int totalTicks = (int) (castTime * 20);
                new SoundEffect(Sound.BLOCK_SCULK_CATALYST_BLOOM, ((float) ticks / totalTicks) * 2f, 1f).play(player.getLocation());

                // VFX
                start.clone();
                final Location topCenter = player.getLocation().subtract(0, 1.0, 0);
                final Location abovePlayer = topCenter.clone().add(0, 2.0, 0);
                final Color color = Color.fromRGB((int) (49 + Math.random() * 30), (int) (168 - Math.random() * 100), 157);
                Particle.TRAIL.builder()
                        .data(new Particle.Trail(abovePlayer, color, 15))
                        .location(topCenter)
                        .offset(0.3, 0, 0.3)
                        .receivers(60)
                        .extra(0.5)
                        .count(5)
                        .spawn();

                if (ticks % 2 == 0) {
                    Particle.SCULK_CHARGE.builder()
                            .data(0f)
                            .location(topCenter)
                            .offset(0.25, 0.1, 0.25)
                            .receivers(60)
                            .count(1)
                            .extra(0)
                            .spawn();
                }

                player.setVelocity(new Vector(0, 0.025, 0));

                // Equip
                final double secondsPassed = ticks / 20.0;
                if (secondsPassed >= castTime) {
                    player.setFallDistance(-10);
                    new SoundEffect("emaginationfallenheroes", "custom.spell.soulfirecast", 2f, 1).play(player.getLocation());
                    equip(player, itemInstance);
                    tasks.remove(player);
                    cancel();
                }
            }
        }.runTaskTimer(champions, 0L, 1L);
        tasks.put(player, new Data(task, start, itemInstance.getItemStack().clone()));
    }

    private void equip(Player player, ItemInstance itemInstance) {
        if (!roleManager.equipRole(player, role)) {
            new SoundEffect(Sound.ENTITY_BEE_STING, 0f, 1f).play(player);
            return; // They weren't allowed to equip from the event
        }

        // Equip them with gear, if ANY
        final Optional<ArmorStorageComponent> storageComponent = itemInstance.getComponent(ArmorStorageComponent.class);
        if (storageComponent.isPresent()) {
            final ArmorStorageComponent armorComponent = storageComponent.get();

            // Refund current gear
            for (ItemStack armorContent : player.getInventory().getArmorContents()) {
                UtilItem.insert(player, armorContent);
            }

            // Populate with new
            player.getInventory().setItem(EquipmentSlot.HEAD, armorComponent.getItem(EquipmentSlot.HEAD));
            player.getInventory().setItem(EquipmentSlot.CHEST, armorComponent.getItem(EquipmentSlot.CHEST));
            player.getInventory().setItem(EquipmentSlot.LEGS, armorComponent.getItem(EquipmentSlot.LEGS));
            player.getInventory().setItem(EquipmentSlot.FEET, armorComponent.getItem(EquipmentSlot.FEET));
        }
    }

    private void onTick() {
        tasks.keySet().removeIf(player -> {
            if (player == null || !player.isValid()) {
                tasks.get(player).task.cancel();
                return true;
            }
            return false;
        });
    }

    // Cancel if they deal or take damage
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onDamage(DamageEvent event) {
        if (event.getDamagee() instanceof Player player && tasks.containsKey(player)) {
            cancel(player);
        }
        if (event.getDamager() instanceof Player player && tasks.containsKey(player)) {
            cancel(player);
        }
    }

    // Cancel if they take velocity
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onVelocity(CustomEntityVelocityEvent event) {
        if (event.getEntity() instanceof Player player && tasks.containsKey(player)) {
            cancel(player);
        }
    }

    // Cancel if they use an interact ability
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onAbility(PlayerUseInteractSkillEvent event) {
        if (event.getPlayer() instanceof Player player && tasks.containsKey(player)) {
            cancel(player);
        }
    }

    // Cancel if they use a toggle skill
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onToggle(PlayerUseToggleSkillEvent event) {
        if (event.getPlayer() instanceof Player player && tasks.containsKey(player)) {
            cancel(player);
        }
    }

    // Cancel if they teleport
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    void onTeleport(EntityTeleportEvent event) {
        if (event instanceof Player player && tasks.containsKey(player)) {
            cancel(player);
        }
    }

    // Cancel if they leave
    @EventHandler(priority = EventPriority.MONITOR)
    void onQuit(PlayerQuitEvent event) {
        cancel(event.getPlayer());
    }

    @Value
    private static class Data {
        BukkitTask task;
        Location location;
        ItemStack itemStack;
    }
}
