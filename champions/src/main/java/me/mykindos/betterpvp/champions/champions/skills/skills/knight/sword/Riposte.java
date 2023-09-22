package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

@Singleton
@BPvPListener
public class Riposte extends PrepareSkill implements CooldownSkill, Listener {


    public HashMap<String, Long> prepare = new HashMap<>();
    private final HashMap<String, Long> riposting = new HashMap<>();

    private double damageReduction;
    @Inject
    public Riposte(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Riposte";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a sword to activate.",
                "",
                "Reduce all melee damage by <val>" + (damageReduction * 100) + "%</val> for <val>" + (1 + (level * 0.5)) + "</val> seconds.",
                "Impervious to knockback while active.",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.SWORD;
    }

    /**
     * Cancel riposte if the player swaps to any weapon other than another sword
     */
    @EventHandler
    public void onSwapItems(PlayerItemHeldEvent event) {
        Player player = event.getPlayer();
        if (!riposting.containsKey(player.getName())) return;

        ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
        if (newItem == null) return;
        if (!newItem.getType().name().contains("SWORD")) {

            riposting.remove(player.getName());
            UtilMessage.message(player, getClassType().getName(), "You are no longer riposting.");

        }
    }

    @EventHandler
    public void onRiposteHit(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player target)) return;
        if (event.getDamager() == null) return;

        int level = getLevel(target);
        if (level > 0) {
            LivingEntity damager = event.getDamager();
            if (prepare.containsKey(target.getName())) {

                prepare.remove(target.getName());
                if (damager instanceof Player) {
                    UtilMessage.simpleMessage(target, getClassType().getName(), "Countered an attack from <yellow>%s<gray>.", damager.getName());
                    UtilMessage.simpleMessage(target, getClassType().getName(), "You <light_purple>Riposted <gray>against <yellow>%s<gray>.", damager.getName());
                }

                target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0F, 1.0F);
                riposting.put(target.getName(), (long) (System.currentTimeMillis() + ((level * 0.5)) * 1000));
                event.cancel("Riposte");

            } else {

                if (riposting.containsKey(target.getName())) {

                    long time = riposting.get(target.getName()) - System.currentTimeMillis();
                    double remaining = UtilTime.convert(time,
                            UtilTime.TimeUnit.SECONDS, 1);

                    if (time <= 0) {
                        riposting.remove(target.getName());
                        return;
                    }

                    if (damager instanceof Player player) {
                        UtilMessage.simpleMessage(player, getClassType().getName(),
                                "<yellow>%s<gray> is resistant to melee attacks for <green>%.1f<gray> seconds.", target.getName(), remaining);
                    }

                    target.getWorld().playSound(target.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 1.0F, 1.0F);
                    event.setDamage(event.getDamage() * (1 - damageReduction));
                    event.setKnockback(false);

                }
            }
        }


    }

    @EventHandler
    public void onRiposteDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        riposting.remove(player.getName());
    }

    @UpdateEvent(delay = 100)
    public void removeFailures() {
        prepare.entrySet().removeIf(entry -> {
            if (UtilTime.elapsed(entry.getValue(), 1000)) {

                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    UtilMessage.simpleMessage(player, getClassType().getName(), "You failed <green>%s<gray>.", getName());
                    player.getWorld().playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2.0f, 1.0f);
                }

                return true;
            }
            return false;
        });
    }

    @UpdateEvent
    public void removeCompleted() {
        riposting.entrySet().removeIf(entry -> {
            if (entry.getValue() - System.currentTimeMillis() <= 0) {
                Player player = Bukkit.getPlayer(entry.getKey());
                if (player != null) {
                    UtilMessage.message(player, getClassType().getName(), "You are no longer riposting");
                }
                return true;
            }
            return false;
        });
    }


    @Override
    public void activate(Player player, int level) {
        prepare.put(player.getName(), System.currentTimeMillis());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 2.0f, 1.3f);

        Particle.SMOKE_LARGE.builder().location(player.getLocation().add(0, 0.25, 0)).receivers(20).extra(0).spawn();

    }

    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * 1.5);
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }


    @Override
    public void loadSkillConfig() {
        damageReduction = getConfig("damageReduction", 0.75, Double.class);
    }
}
