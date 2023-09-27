package me.mykindos.betterpvp.champions.champions.skills.skills.assassin.passives;


import com.google.inject.Inject;
import com.google.inject.Singleton;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.champions.champions.ChampionsManager;
import me.mykindos.betterpvp.champions.champions.skills.Skill;
import me.mykindos.betterpvp.champions.champions.skills.types.CooldownSkill;
import me.mykindos.betterpvp.champions.champions.skills.types.ToggleSkill;
import me.mykindos.betterpvp.core.components.champions.Role;
import me.mykindos.betterpvp.core.components.champions.SkillType;
import me.mykindos.betterpvp.core.listener.BPvPListener;
import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;

@Singleton
@BPvPListener
public class Endurance extends Skill implements ToggleSkill, CooldownSkill{

    private double effectDuration;
    @Inject
    public Endurance(Champions champions, ChampionsManager championsManager) {
        super(champions, championsManager);
    }


    @Override
    public String getName() {
        return "Endurance";
    }

    @Override
    public String[] getDescription(int level) {

        return new String[]{
                "Drop your Sword / Axe to activate",
                "",
                "Instantly gain <stat>3</stat> extra hearts and <effect>Speed III</effect> for <val>" + (5 + (level - 1)) + "</val> seconds",
                "",
                "Cooldown: <val>" + getCooldown(level)
        };
    }

    @Override
    public void toggle(Player player, int level) {

        player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, ((5 + (level - 1)) * 20), 2, true, true));
        player.getWorld().playSound(player.getLocation(), Sound.ENTITY_WARDEN_DEATH, 1.0F, 1.0F);
        player.getWorld().playEffect(player.getLocation(), Effect.STEP_SOUND, Material.REDSTONE_BLOCK);

        AttributeInstance maxHealth = player.getAttribute(Attribute.GENERIC_MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(26.0);
            if (player.getHealth() < 26.0) player.setHealth(player.getHealth()+6);
        }

        Bukkit.getScheduler().runTaskLater(champions, () -> {
            if (maxHealth != null) {
                maxHealth.setBaseValue(20.0);
            }
        }, (5 + (level - 1)) * 20L);
    }

    @Override
    public Role getClassType() {
        return Role.ASSASSIN;
    }


    @Override
    public SkillType getType() {

        return SkillType.PASSIVE_B;
    }

    @Override
    public double getCooldown(int level) {

        return cooldown - ((level - 1) * 2.5);
    }

    @Override
    public void loadSkillConfig(){
        effectDuration = getConfig("effectDuration", 5.0, Double.class);
    }
}


