package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.skills.assassin.data.SilencingStrikesData;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.effects.EffectType;
import me.mykindos.betterpvp.core.framework.updater.UpdateEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

@Singleton
@BPvPListener
public class SilencingStrikes extends Skill implements PassiveSkill, Listener {

    public List<SilencingStrikesData> data = new ArrayList<>();

    @Inject
    public SilencingStrikes(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    private int hitsNeeded;
    private double timeSpan;

    @Override
    public String getName() {
        return "Silencing Strikes";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                //Student 9/21/2023 reworded description to match how the skill actually works
                "Hit a player <val>" + hitsNeeded + "</val> consecutive times without letting",
                "<val>" + timeSpan + "</van> seconds pass.",
                "to silence them for <val>" + (level) + "</val> seconds."
        };
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }


    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamager() instanceof Player damager)) return;
        if (!(event.getDamagee() instanceof Player damagee)) return;

        int level = getLevel(damager);
        if (level > 0) {
            SilencingStrikesData silenceData = getSilencingStrikesData(damager, damagee);
            if (silenceData == null) {
                silenceData = new SilencingStrikesData(damager.getUniqueId(), damagee.getUniqueId());
                data.add(silenceData);
            }

            silenceData.addCount();
            silenceData.setLastHit(System.currentTimeMillis());
            event.setReason(getName());
            if (silenceData.getCount() == hitsNeeded) {
                championsManager.getEffects().addEffect(damagee, EffectType.SILENCE, (long) ((level * 1000L) * 0.75));
                //if (championsManager.getEffects().hasEffect(damagee, EffectType.IMMUNETOEFFECTS)) {
                //    UtilMessage.message(damager, getClassType().getName(), "%s is immune to your silence!",
                //            ChatColor.GREEN + damagee.getName() + ChatColor.GRAY);
                //}
                data.remove(silenceData);
            }
        }

    }


    @UpdateEvent
    public void onUpdate() {
        //Student 9/21/2023 This seems to be the area where we reset if we have not hit
        //This does not match the original skill description with original value of 800
        data.removeIf(silenceData -> UtilTime.elapsed(silenceData.getLastHit(), (long) timeSpan * 1000));
    }

    public SilencingStrikesData getSilencingStrikesData(Player damager, Player damagee) {
        for (SilencingStrikesData silenceData : data) {
            if (silenceData.getPlayer().equals(damager.getUniqueId())) {
                if (silenceData.getTarget().equals(damagee.getUniqueId())) {
                    return silenceData;
                }
            }
        }
        return null;
    }
    public void loadSkillConfig(){
        hitsNeeded = getConfig("hitsNeeded", 3, Integer.class);
        timeSpan = getConfig("timeSpan", 0.8, Double.class);
    }
}
