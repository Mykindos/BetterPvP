package me.mykindos.betterpvp.core.effects.types.negative;

import lombok.Getter;
import me.mykindos.betterpvp.core.effects.Effect;
import me.mykindos.betterpvp.core.effects.VanillaEffectType;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffectType;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PinEffect extends VanillaEffectType {

    @Getter
    private final Map<UUID, Integer> interactionsLeft = new HashMap<>();

    @Override
    public String getName() {
        return "Pin";
    }

    @Override
    public boolean isNegative() {
        return true;
    }

    @Override
    public PotionEffectType getVanillaPotionType() {
        return PotionEffectType.BAD_OMEN;
    }

    @Override
    public void onTick(LivingEntity livingEntity, Effect effect) {
        super.onTick(livingEntity, effect);

        final int remaining = interactionsLeft.getOrDefault(livingEntity.getUniqueId(), 0);
        if (!livingEntity.isValid() || !(livingEntity instanceof Player player) || remaining <= 0) {
            this.interactionsLeft.remove(livingEntity.getUniqueId());
            return;
        }

        Particle.ENCHANTED_HIT.builder()
                .count(7)
                .offset(0.5, 0.5, 0.5)
                .location(livingEntity.getLocation().add(0, livingEntity.getHeight() / 2, 0))
                .receivers(60)
                .spawn();

        final boolean alternate = Bukkit.getCurrentTick() / 7 != (Bukkit.getCurrentTick() - 1) / 7;
        final TextColor titleColor = alternate ? TextColor.color(255, 0, 0) : TextColor.color(186, 186, 186);
        final Component titleComponent = Component.text("YOU ARE PINNED").color(titleColor);
        final TextColor subTitleColor = alternate ? TextColor.color(186, 186, 186) : TextColor.color(255, 171, 171);
        final Component subTitleComponent = Component.text("Jump").color(subTitleColor)
                .appendSpace()
                .append(Component.text(remaining, NamedTextColor.WHITE))
                .appendSpace()
                .append(Component.text("times to escape!").color(subTitleColor));
        final Title title = Title.title(titleComponent, subTitleComponent, Title.Times.times(
                Duration.ofMillis(0),
                Duration.ofMillis(100),
                Duration.ofMillis(0)
        ));
        player.showTitle(title);
    }

    @Override
    public void onExpire(LivingEntity livingEntity, Effect effect, boolean notify) {
        super.onExpire(livingEntity, effect, notify);

        AttributeInstance noJumpAttr = livingEntity.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
        if (noJumpAttr != null) {
            noJumpAttr.setBaseValue(noJumpAttr.getDefaultValue());
        }

        AttributeInstance speedAttr = livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(0.1);
        }
    }

    @Override
    public void onReceive(LivingEntity livingEntity, Effect effect) {
        super.onReceive(livingEntity, effect);
        AttributeInstance attribute = livingEntity.getAttribute(Attribute.GENERIC_JUMP_STRENGTH);
        if (attribute != null) {
            attribute.setBaseValue(0.1);
        }

        AttributeInstance speedAttr = livingEntity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
        if (speedAttr != null) {
            speedAttr.setBaseValue(0);
        }

        interactionsLeft.put(livingEntity.getUniqueId(), effect.getAmplifier());
    }

    @Override
    public String getDescription(int level) {
        return "<white>" + getName() + "</white> immobilizes the target until they jump " + level + " times.";
    }

    @Override
    public String getGenericDescription() {
        return "<white>" + getName() + "</white> immobilizes the target until they jump a certain amount of times.";
    }
}
