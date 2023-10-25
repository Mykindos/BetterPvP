package me.mykindos.betterpvp.champions.champions.skills.skills.brute.passives;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.data.SkillWeapons;
import me.mykindos.betterpvp.champions.champions.skills.types.PassiveSkill;
import me.mykindos.betterpvp.core.combat.events.CustomDamageEvent;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import me.mykindos.betterpvp.core.utilities.UtilPlayer;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.Set;

@Singleton
@BPvPListener
public class CripplingBlow extends Skill implements PassiveSkill {

    private double slowDuration;

    @Inject
    public CripplingBlow(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }

    @Override
    public String getName() {
        return "Crippling Blow";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Enemies you hit with an axe don't take knockback",
                "and receive <effect>Slowness I</effect> for <val>" + (slowDuration + (level * 0.5)) + "</val> seconds"
        };
    }

    @Override
    public Set<Role> getClassTypes() {
        return Role.BRUTE;
    }


    @EventHandler
    public void onDamage(CustomDamageEvent event) {
        if(!(event.getDamager() instanceof Player player)) return;
        if(!UtilPlayer.isHoldingItem(player, SkillWeapons.AXES)) return;

        int level = getLevel(player);
        if(level > 0) {
            LivingEntity target = event.getDamagee();
            target.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, (int) ((slowDuration + (level * 0.5)) * 20), 0));
            event.setReason(getName());
            event.setKnockback(false);
        }

    }

    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_A;
    }

    @Override
    public void loadSkillConfig(){
        slowDuration = getConfig("slowDuration", 2.0, Double.class);
    }

}
