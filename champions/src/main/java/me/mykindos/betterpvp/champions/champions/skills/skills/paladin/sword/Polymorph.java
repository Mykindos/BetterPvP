package me.mykindos.betterpvp.champions.champions.skills.skills.paladin.sword;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.libraryaddict.disguise.DisguiseAPI;
import me.libraryaddict.disguise.disguisetypes.DisguiseType;
import me.libraryaddict.disguise.disguisetypes.MobDisguise;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillActions;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.PrepareSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.components.champions.events.PlayerUseSkillEvent;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilMessage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Polymorph extends PrepareSkill implements CooldownSkill {

    public final WeakHashMap<LivingEntity, Long> polymorphed = new WeakHashMap<>();

    private double polymorphDuration;

    @Inject
    public Polymorph(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Polymorph";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Right click with a sword to prepare.",
                "",
                "The next player you hit, is polymorphed",
                "into a sheep for " + ChatColor.GREEN + polymorphDuration + ChatColor.GRAY + " seconds.",
                "",
                "While a player is polymorphed, they cannot deal",
                "or take any damage.",
                "",
                "Cooldown: " + ChatColor.GREEN + getCooldown(level)
        };
    }

    @Override
    public Role getClassType() {
        return Role.PALADIN;
    }


    @EventHandler(priority = EventPriority.HIGHEST)
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player player)) return;
        if (!active.contains(player.getUniqueId())) return;

        int level = getLevel(player);
        if (level > 0) {
            LivingEntity ent = event.getDamagee();
            event.cancel("Polymorph start");
            DisguiseAPI.disguiseToAll(ent, new MobDisguise(DisguiseType.SHEEP));
            ent.getWorld().playSound(ent.getLocation(), Sound.ENTITY_SHEEP_AMBIENT, 2.0f, 1.0f);
            polymorphed.put(ent, System.currentTimeMillis());

            ent.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) (polymorphDuration * 20), 2));
            UtilMessage.simpleMessage(player, getName(), "You polymorphed <green>%s<gray>.", ent.getName());
            if (ent instanceof Player target) {
                championsManager.getEffects().addEffect(target, EffectType.SILENCE, (long) polymorphDuration * 1000);
                UtilMessage.simpleMessage(target, getName(), "<green>%s<gray> polymorphed you.", player.getName());
            }
            active.remove(player.getUniqueId());
        }

    }

    @EventHandler
    public void onUseSkill(PlayerUseSkillEvent event) {
        if(polymorphed.containsKey(event.getPlayer())){
            event.cancel("Cannot use skills while polymorphed");
        }
    }

    @UpdateEvent(delay = 250)
    public void updatePoly() {
        Iterator<Entry<LivingEntity, Long>> it = polymorphed.entrySet().iterator();
        while (it.hasNext()) {
            Entry<LivingEntity, Long> next = it.next();
            if (UtilTime.elapsed(next.getValue(), (long) polymorphDuration * 1000)) {
                UtilMessage.message(next.getKey(), getName(), "You are no longer polymorphed.");
                DisguiseAPI.undisguiseToAll(next.getKey());
                it.remove();
            }
        }

    }

    @EventHandler
    public void onPolyDamage(CustomDamageEvent event) {

        if (polymorphed.containsKey(event.getDamager())) {
            event.cancel("Polymorphed");
        } else if (polymorphed.containsKey(event.getDamagee())) {
            event.cancel("Polymorphed");
        }

    }

    @Override
    public SkillType getType() {

        return SkillType.SWORD;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 7);
    }


    @Override
    public void activate(Player player, int level) {
        active.add(player.getUniqueId());
    }

    @Override
    public Action[] getActions() {
        return SkillActions.RIGHT_CLICK;
    }

    @Override
    public void loadSkillConfig(){
        polymorphDuration = getConfig("duration", 8.0, Double.class);
    }
}
