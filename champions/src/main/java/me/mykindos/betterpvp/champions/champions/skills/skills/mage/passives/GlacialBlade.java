package me.mykindos.betterpvp.champions.champions.skills.skills.mage.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.DamageSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableItem;
import me.mykindos.betterpvp.core.combat.throwables.ThrowableListener;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import org.bukkit.Effect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Singleton
@BPvPListener
public class GlacialBlade extends Skill implements PassiveSkill, CooldownSkill, EnergySkill, ThrowableListener, DamageSkill {

    private double damage;
    private double damageIncreasePerLevel;
    private List<Item> iceShards = new ArrayList<>();
    private Map<Item, Player> shardMap = new HashMap<>();


    @Inject
    public GlacialBlade(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Glacial Blade";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Swinging your sword launches a glacial",
                "shard that deals " + getValueString(this::getDamage, level) + " damage to enemies",
                "",
                "Will not work within melee range",
                "",
                "Cooldown: " + getValueString(this::getCooldown, level),
                "Energy: " + getValueString(this::getEnergy, level),
        };
    }

    public double getDamage(int level){
        return damage + ((level - 1) * damageIncreasePerLevel);
    }
    @Override
    public float getEnergy(int level) {
        return (float) (energy - (level - 1) * energyDecreasePerLevel);
    }

    @EventHandler
    public void onSwing(PlayerInteractEvent event) {
        if (!SkillWeapons.isHolding(event.getPlayer(), SkillType.SWORD)) return;
        if (!event.getAction().isLeftClick()) return;
        if(event.useItemInHand() == Event.Result.DENY) return;

        Player player = event.getPlayer();
        int level = getLevel(player);
        if (level < 1) return;

        if(championsManager.getCooldowns().hasCooldown(player, getName())) return;

        if (!championsManager.getEnergy().use(player, getName(), getEnergy(level), true)) return;

        if (!isObstructionNearby(player)) {
            ItemStack ghastTear = new ItemStack(Material.GHAST_TEAR);
            Item ice = player.getWorld().dropItem(player.getEyeLocation(), ghastTear);
            ice.getWorld().playSound(ice.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 2f);
            ice.setVelocity(player.getLocation().getDirection().multiply(2.5));
            championsManager.getThrowables().addThrowable(this, ice, player, getName(), 5000L);
            iceShards.add(ice);
            shardMap.put(ice, player);

            championsManager.getCooldowns().use(player, getName(), getCooldown(level), false, true, isCancellable());
        }
    }

    private boolean isObstructionNearby(Player player) {
        Location eyeLocation = player.getEyeLocation();
        Vector direction = eyeLocation.getDirection();

        for (double distance = 0; distance <= 3; distance += 0.2) {
            Location stepLocation = eyeLocation.clone().add(direction.multiply(distance));

            List<Entity> nearbyEntities = (List<Entity>) stepLocation.getWorld().getNearbyEntities(stepLocation, 0.2, 0.2, 0.2);
            if (!nearbyEntities.isEmpty()) {
                for (Entity entity : nearbyEntities) {
                    if (!entity.equals(player)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    @UpdateEvent
    public void onUpdate() {
        for (Item ice : new ArrayList<>(iceShards)) {
            if (!ice.isOnGround() && !ice.isDead()) {
                Location iceLocation = ice.getLocation().add(0, 0.25, 0);
                ice.getWorld().spawnParticle(Particle.ITEM_SNOWBALL, iceLocation, 1);
            } else {
                ice.remove();
                iceShards.remove(ice);
            }
        }
    }

    @Override
    public void onThrowableHit(ThrowableItem throwableItem, LivingEntity thrower, LivingEntity hit) {
        if (hit instanceof ArmorStand) {
            return;
        }

        Item iceItem = throwableItem.getItem();
        if (iceItem != null) {
            iceShards.remove(iceItem);
            iceItem.remove();
        }

        if (thrower instanceof Player damager) {
            int level = getLevel(damager);

            CustomDamageEvent cde = new CustomDamageEvent(hit, damager, null, DamageCause.CUSTOM, getDamage(level), false, "Glacial Blade");
            cde.setDamageDelay(0);
            UtilDamage.doCustomDamage(cde);
            hit.getWorld().playEffect(hit.getLocation(), Effect.STEP_SOUND, Material.GLASS);

        }
    }
    @Override
    public Role getClassType() {
        return Role.MAGE;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_B;
    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * cooldownDecreasePerLevel);
    }

    public void loadSkillConfig() {
        damage = getConfig("damage", 1.5, Double.class);
        damageIncreasePerLevel = getConfig("damageIncreasePerLevel", 1.0, Double.class);
    }
}