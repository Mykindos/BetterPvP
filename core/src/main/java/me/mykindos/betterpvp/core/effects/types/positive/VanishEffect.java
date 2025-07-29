package me.mykindos.betterpvp.core.effects.types.positive;

import me.mykindos.betterpvp.core.Core;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import me.mykindos.betterpvp.core.utilities.UtilEffect;
import org.bukkit.Bukkit;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

public class VanishEffect extends VanillaEffectType {

    private final Core core;

    public VanishEffect() {
        core = JavaPlugin.getPlugin(Core.class);
    }

    @Override
    public String getName() {
        return "Vanish";
    }

    @Override
    public boolean isNegative() {
        return false;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.INVISIBILITY;
    }

    @Override
    public void onReceive(LivingEntity livingEntity, Effect effect) {
        UtilEffect.applyCraftEffect(livingEntity, new PotionEffect(PotionEffectType.INVISIBILITY, effect.getVanillaDuration(), 0, false, false, true));

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.isOp() && onlinePlayer.getGameMode().isInvulnerable()) continue;
            onlinePlayer.hideEntity(core, livingEntity);
            if (livingEntity instanceof Player player) {
                onlinePlayer.unlistPlayer(player);
            }
        }

    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect, boolean notify) {
        super.onExpire(livingEntity, effect, notify);

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            onlinePlayer.showEntity(core, livingEntity);
            if (livingEntity instanceof Player player) {
                onlinePlayer.listPlayer(player);
            }
        }
    }

    @Override
    public void checkActive(LivingEntity livingEntity, Effect effect) {
        UtilEffect.applyCraftEffect(livingEntity, new PotionEffect(PotionEffectType.INVISIBILITY, effect.getVanillaDuration(), 0, false, false, true));

        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
            if (onlinePlayer.isOp() && onlinePlayer.getGameMode().isInvulnerable()) continue;
            if (onlinePlayer.canSee(livingEntity)) {
                onlinePlayer.hideEntity(core, livingEntity);
                if (livingEntity instanceof Player player) {
                    onlinePlayer.unlistPlayer(player);
                }
            }
        }
    }

    @Override
    public String getDescription(int level) {
        return "<white>" + getName() + "</white> completely hides you from other players";
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
