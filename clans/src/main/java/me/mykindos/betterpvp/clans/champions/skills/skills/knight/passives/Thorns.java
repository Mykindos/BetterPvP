package me.mykindos.betterpvp.clans.champions.skills.skills.knight.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.clans.Clans;
import me.mykindos.betterpvp.clans.champions.ChampionsManager;
import me.mykindos.betterpvp.clans.champions.roles.Role;
import me.mykindos.betterpvp.clans.champions.skills.Skill;
import me.mykindos.betterpvp.clans.champions.skills.config.SkillConfigFactory;
import me.mykindos.betterpvp.clans.champions.skills.data.SkillType;
import me.mykindos.betterpvp.clans.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilDamage;
import me.mykindos.betterpvp.core.utilities.UtilTime;
import org.bukkit.ChatColor;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;

import java.util.WeakHashMap;

@Singleton
@BPvPListener
public class Thorns extends Skill implements PassiveSkill, Listener {

    private final WeakHashMap<LivingEntity, Long> cd = new WeakHashMap<>();

    @Inject
    public Thorns(Clans clans, ChampionsManager championsManager, SkillConfigFactory configFactory) {
        super(clans, championsManager, configFactory);
    }

    @Override
    public String getName() {
        return "Thorns";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Enemies take " + ChatColor.GREEN + (level) + ChatColor.GRAY + " damage when",
                "they hit you using a melee attack."
        };
    }

    @Override
    public Role getClassType() {
        return Role.KNIGHT;
    }

    @Override
    public SkillType getType() {
        return SkillType.PASSIVE_A;
    }

    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if (event.isCancelled()) return;
        if (event.getCause() != DamageCause.ENTITY_ATTACK) return;
        if (!(event.getDamagee() instanceof Player p)) return;
        if (event.getDamager() == null) return;

        int level = getLevel(p);
        if (level > 0) {
            LivingEntity damager = event.getDamager();
            if (!cd.containsKey(damager)) {
                cd.put(damager, System.currentTimeMillis());
            }else{
                if(UtilTime.elapsed(cd.get(damager), 2000)){
                    UtilDamage.doCustomDamage(new CustomDamageEvent(damager, p, null, DamageCause.CUSTOM, level * 0.80, false, getName()));
                    cd.put(damager, System.currentTimeMillis());
                }
            }
        }



        }

    }
