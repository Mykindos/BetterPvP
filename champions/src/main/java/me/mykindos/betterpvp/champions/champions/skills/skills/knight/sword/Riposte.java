package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.*;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilBlock;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.*;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Riposte extends ChannelSkill implements CooldownSkill, InteractSkill {


    private final HashMap<String, Long> riposting = new HashMap<>();
    private final HashMap<Player, Long> handRaisedTime = new HashMap<>();
    private final HashMap<Player, Long> boostedAttackTime = new HashMap<>();
    private final HashMap<Player, Double> boostedDamage = new HashMap<>();

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
                "Hold right click with a Sword to activate",
                "",
                "if an enemy hits you within 0.75 seconds,",
                "",
                "your next attack will deal <val>" + (6 + (level)) + "</val> extra damage",
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
            player.getWorld().playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2.0f, 1.0f);
        }
    }

    @EventHandler
    public void onRiposte(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;
        if (event.getDamager() == null) return;
        LivingEntity ent = event.getDamager();

        if (hasSkill(player)) {
            event.setKnockback(false);
            event.setDamage(0);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_ATTACK_IRON_DOOR, 2.0f, 1.3f);
            int level = getLevel(player);
            boostedDamage.put(player, 6.0 + level);
            boostedAttackTime.put(player, System.currentTimeMillis());
            handRaisedTime.remove(player);
            UtilMessage.simpleMessage(player, getClassType().getName(), "You used <green>%s<gray>.", getName());
            if (ent instanceof Player temp) {
                UtilMessage.simpleMessage(temp, getClassType().getName(), "<yellow>%s<gray> used riposte!", player.getName());
            }
        }
    }

    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 2.0f, 1.3f);

        Particle.SMOKE_LARGE.builder().location(player.getLocation().add(0, 0.25, 0)).receivers(20).extra(0).spawn();
    }

    @UpdateEvent(delay = 100)
    public void onUpdateEffect() {
        Iterator<UUID> it = active.iterator();
        while (it.hasNext()) {
            Player player = Bukkit.getPlayer(it.next());
            if (player != null) {
                if (player.isHandRaised()) {
                    player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.IRON_BLOCK);
                }
            } else {
                it.remove();
            }
        }
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<UUID> it = active.iterator();
        long currentTime = System.currentTimeMillis();

        while (it.hasNext()) {
            Player player = Bukkit.getPlayer(it.next());
            if (player != null) {
                int level = getLevel(player);
                if (level > 0) {

                    if (player.isHandRaised() && !handRaisedTime.containsKey(player)) {
                        handRaisedTime.put(player, System.currentTimeMillis());
                    }

                    if (!player.isHandRaised() && handRaisedTime.containsKey(player) && !boostedDamage.containsKey(player)) {
                        if (UtilTime.elapsed(handRaisedTime.get(player), 750)) {
                            handRaisedTime.remove(player);
                            it.remove();
                            UtilMessage.message(player, getClassType().getName(), "Your Riposte failed.");
                            player.getWorld().playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2.0f, 1.0f);
                        } else {
                            handRaisedTime.remove(player);
                            it.remove();
                            UtilMessage.message(player, getClassType().getName(), "Your Riposte failed.");
                            player.getWorld().playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2.0f, 1.0f);
                        }
                    }

                    if (player.isHandRaised() && handRaisedTime.containsKey(player) && UtilTime.elapsed(handRaisedTime.get(player), 750) && !boostedDamage.containsKey(player)) {
                        handRaisedTime.remove(player);
                        UtilMessage.message(player, getClassType().getName(), "Your Riposte failed.");
                        player.getWorld().playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2.0f, 1.0f);
                        it.remove();
                    }

                    if (boostedAttackTime.containsKey(player) && UtilTime.elapsed(boostedAttackTime.get(player), 2000)) {
                        boostedAttackTime.remove(player);
                        boostedDamage.remove(player);
                        UtilMessage.message(player, getClassType().getName(), "You lost your boosted attack.");
                        player.getWorld().playSound(player.getLocation(), Sound.UI_BUTTON_CLICK, 2.0f, 1.0f);
                    }
                } else {
                    it.remove();
                }
            }
        }
    }

    @EventHandler
    public void onAttack(CustomDamageEvent event) {
        if (!(event.getDamager() instanceof Player player)) return;
        if (boostedDamage.containsKey(player)) {
            event.setDamage(event.getDamage() + boostedDamage.get(player));
            boostedDamage.remove(player);
            boostedAttackTime.remove(player);
            player.getWorld().playSound(player.getLocation(), Sound.ENTITY_ZOMBIE_BREAK_WOODEN_DOOR, 2.0f, 1.0f);

        }
    }

    @EventHandler
    public void onRiposteDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        riposting.remove(player.getName());
        active.remove(player.getUniqueId());
    }

    @EventHandler
    public void onPlayerLogout(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        riposting.remove(player.getName());
        active.remove(player.getUniqueId());
    }


    @Override
    public double getCooldown(int level) {
        return cooldown - ((level - 1) * 1.5);
    }


    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }
}
