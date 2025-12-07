package me.mykindos.betterpvp.champions.item.ability;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.Client;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.effects.EffectTypes;
import me.mykindos.betterpvp.core.item.BaseItem;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemInstance;
import me.mykindos.betterpvp.core.item.component.impl.ability.ItemAbility;
import me.mykindos.betterpvp.core.item.component.impl.ability.TriggerType;
import me.mykindos.betterpvp.core.utilities.UtilItem;
import me.mykindos.betterpvp.core.utilities.UtilVelocity;
import me.mykindos.betterpvp.core.utilities.math.VelocityData;
import me.mykindos.betterpvp.core.utilities.model.SoundEffect;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;

import java.util.Objects;

@Getter
@Setter
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
public class SkyforgedAscent extends ItemAbility implements Listener {

    @EqualsAndHashCode.Include
    private double velocity;
    @EqualsAndHashCode.Include
    private double cooldown; // Seconds
    @EqualsAndHashCode.Include
    private int speedAmplifier;
    @EqualsAndHashCode.Include
    private double speedDuration; // Seconds
    private final EffectManager effectManager;
    private final CooldownManager cooldownManager;
    private final BaseItem heldItem;
    private final ItemFactory itemFactory;
    private final ClientManager clientManager;

    public SkyforgedAscent(EffectManager effectManager, CooldownManager cooldownManager,
                            BaseItem heldItem, ItemFactory itemFactory, ClientManager clientManager) {
        super(new NamespacedKey(JavaPlugin.getPlugin(Champions.class),
                        "skyforged_ascent"),
                "Skyforged Ascent",
                "Throw the weapon to ascend skyward, riding its divine force, granting you a burst of speed.",
                TriggerType.dummy("Throw"));
        this.effectManager = effectManager;
        this.cooldownManager = cooldownManager;
        this.heldItem = heldItem;
        this.itemFactory = itemFactory;
        this.clientManager = clientManager;
        Bukkit.getPluginManager().registerEvents(this, JavaPlugin.getPlugin(Champions.class));
    }

    @Override
    public boolean invoke(Client client, ItemInstance itemInstance, ItemStack itemStack) {
        final Player player = Objects.requireNonNull(client.getGamer().getPlayer());
        if (!cooldownManager.use(player, getName(), cooldown, true, true, true, (BaseItem) null)) {
            return false;
        }

        // Set them to riptide mode
        UtilVelocity.velocity(player, player, new VelocityData(
                player.getLocation().getDirection(),
                velocity,
                0,
                10.0,
                true
        ));
        effectManager.addEffect(player, EffectTypes.NO_FALL, 5_000L);

        // Give speed effect
        effectManager.addEffect(player, EffectTypes.SPEED, speedAmplifier, (long) (speedDuration * 1000L));

        // Cues
        new SoundEffect(Sound.ITEM_TRIDENT_THUNDER, 2f, 0.5f).play(player.getLocation());
        new SoundEffect(Sound.ITEM_TRIDENT_RIPTIDE_2, 0f, 1f).broadcast(player);
        Particle.CLOUD.builder()
                .extra(0.2)
                .count(20)
                .location(player.getLocation())
                .receivers(60)
                .spawn();

        // Spawn circle with star of DUST particles under the player
        // Create circle particles
        Location center = player.getEyeLocation().add(player.getLocation().getDirection().multiply(2.5));
        Particle particle = Particle.END_ROD;
        final double radius = 2.5; // Radius of the circle
        final double pitch = Math.toRadians(player.getLocation().getPitch() + 90);
        final double yaw = Math.toRadians(-player.getLocation().getYaw());

        for (int i = 0; i < 360; i += 10) {
            double angle = Math.toRadians(i);
            double x = Math.cos(angle) * radius; // 2 block radius
            double z = Math.sin(angle) * radius;

            Vector particleLoc = new Vector(x, 0, z);
            particleLoc.rotateAroundX(pitch);
            particleLoc.rotateAroundY(yaw);
            particleLoc.add(center.toVector());
            particle.builder()
                    .location(particleLoc.toLocation(center.getWorld()))
                    .extra(0)
                    .receivers(60)
                    .spawn();
        }

        // Create star particles (5-pointed star)
        for (int i = 0; i < 5; i++) {
            double angle = Math.toRadians(i * 72); // 72 degrees between points
            double x = Math.cos(angle) * radius; // Slightly larger radius for star
            double z = Math.sin(angle) * radius;

            Vector starPoint = new Vector(x, 0, z);
            starPoint.rotateAroundX(pitch);
            starPoint.rotateAroundY(yaw);
            starPoint.add(center.toVector());
            particle.builder()
                    .location(starPoint.toLocation(center.getWorld()))
                    .extra(0)
                    .receivers(60)
                    .spawn();
        }

        // Create inner star points (connecting lines)
        for (int i = 0; i < 5; i++) {
            double angle1 = Math.toRadians(i * 72);
            double angle2 = Math.toRadians((i + 2) * 72); // Connect to point 2 steps away

            double x1 = Math.cos(angle1) * radius;
            double z1 = Math.sin(angle1) * radius;
            double x2 = Math.cos(angle2) * radius;
            double z2 = Math.sin(angle2) * radius;

            // Spawn particles along the line between star points
            for (int j = 0; j <= 10; j++) {
                double t = j / 10.0;
                double x = x1 + (x2 - x1) * t;
                double z = z1 + (z2 - z1) * t;

                Vector linePoint = new Vector(x, 0, z);
                linePoint.rotateAroundX(pitch);
                linePoint.rotateAroundY(yaw);
                linePoint.add(center.toVector());
                particle.builder()
                        .location(linePoint.toLocation(center.getWorld()))
                        .extra(0)
                        .receivers(60)
                        .spawn();
            }
        }

        // Consume durability
        UtilItem.damageItem(player, itemStack, 1);
        return true;
    }

    @EventHandler
    public void onTridentThrow(ProjectileLaunchEvent event) {
        if (!(event.getEntity() instanceof Trident trident)) return; // Not a throwable trident
        if (!(trident.getShooter() instanceof Player player)) return; // Shooter must be a player

        ItemStack itemStack = trident.getItemStack();
        ItemInstance itemInstance = itemFactory.fromItemStack(itemStack).orElse(null);
        if (itemInstance == null || !itemInstance.getBaseItem().equals(heldItem)) {
            return; // Ensure the item is the one with the ability
        }

        event.setCancelled(true); // Cancel the default throw action
        Client client = clientManager.search().online(player);
        invoke(client, itemInstance, player.getEquipment().getItem(player.getActiveItemHand()));
    }
}