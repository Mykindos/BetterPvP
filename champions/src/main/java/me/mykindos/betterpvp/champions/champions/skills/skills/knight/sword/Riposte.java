package me.mykindos.betterpvp.champions.champions.skills.skills.knight.sword;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.EnergySkill;
import me.mykindos.betterpvp.champions.champions.skills.types.InteractSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
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
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Riposte extends PrepareSkill implements CooldownSkill, Listener, InteractSkill, EnergySkill {


    public HashMap<String, Long> prepare = new HashMap<>();
    private final HashMap<String, Long> riposting = new HashMap<>();
    private final WeakHashMap<Player, Long> delay = new WeakHashMap<>();

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

        }
    }

    @EventHandler
    public void onRiposteDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        riposting.remove(player.getName());
    }

    @EventHandler
    public void onRiposte(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;
        if (event.getDamager() == null) return;
        LivingEntity ent = event.getDamager();


        if (hasSkill(player)) {

            if (!delay.containsKey(player)) {
                delay.put(player, 0L);
            }

            event.setKnockback(false);
            event.cancel("Skill Riposte");
            if (UtilTime.elapsed(delay.get(player), 500)) {
                for (int i = 0; i < 3; i++) {
                    player.getWorld().playEffect(player.getLocation(), Effect.SMOKE, 5);
                }
                //add functionality


                UtilMessage.simpleMessage(player, getClassType().getName(), "You used <green>%s<gray>.", getName());
                if (ent instanceof Player temp) {
                    UtilMessage.simpleMessage(temp, getClassType().getName(), "<yellow>%s<gray> used evade!", player.getName());
                }

                delay.put(player, System.currentTimeMillis());
            }

        }
    }

    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_IRON_GOLEM_ATTACK, 2.0f, 1.3f);

        Particle.SMOKE_LARGE.builder().location(player.getLocation().add(0, 0.25, 0)).receivers(20).extra(0).spawn();
    }

    @UpdateEvent
    public void onUpdate() {
        Iterator<UUID> it = active.iterator();
        while (it.hasNext()) {
            Player player = Bukkit.getPlayer(it.next());
            if (player != null) {
                int level = getLevel(player);
                if (level > 0) {
                    if (player.isHandRaised()) {
                        if (!championsManager.getEnergy().use(player, getName(), getEnergy(getLevel(player)) / 2, true)) {
                            it.remove();
                        } else if (!UtilPlayer.isHoldingItem(player, SkillWeapons.SWORDS)) {
                            it.remove();
                        } else if (UtilBlock.isInLiquid(player)) {
                            it.remove();
                        } else if (championsManager.getEffects().hasEffect(player, EffectType.SILENCE)) {
                            it.remove();
                        } else if (championsManager.getEffects().hasEffect(player, EffectType.STUN)) {
                            it.remove();
                        }
                    } else {
                        if (gap.containsKey(player)) {
                            if (UtilTime.elapsed(gap.get(player), 250)) {
                                gap.remove(player);
                                it.remove();

                            }
                        }

                    }
                } else {
                    it.remove();
                }
            }

        }

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
