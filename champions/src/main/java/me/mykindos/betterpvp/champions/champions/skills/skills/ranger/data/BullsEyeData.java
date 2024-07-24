package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data;

import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

public class BullsEyeData {

    private final Player caster;
    private final ChargeData casterCharge;
    private LivingEntity target;
    private ChargeData targetFocused;
    private Color color;

    public BullsEyeData(Player caster, ChargeData casterCharge, LivingEntity target, ChargeData targetFocused, Color color) {
        this.caster = caster;
        this.casterCharge = casterCharge;
        this.target = target;
        this.targetFocused = targetFocused;
        this.color = color;
    }

    public Player getCaster() {
        return caster;
    }

    public ChargeData getCasterCharge() {
        return casterCharge;
    }

    public boolean hasTarget() {
        return target != null;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public ChargeData getTargetFocused() {
        return targetFocused;
    }

    public void setTarget(LivingEntity newTarget) {
        target = newTarget;
    }

    public void setTargetFocused(ChargeData newTargetFocused) {
        targetFocused = newTargetFocused;
    }

    public Color getColor() {
        return color;
    }

    public void spawnFocusingParticles() {
        float targetFocusedAmount = getTargetFocused().getCharge();

        Location casterLocation = caster.getLocation().add(0, caster.getHeight() / 3, 0);
        Location targetLocation = target.getLocation().add(0, target.getHeight() / 3, 0);

        Vector direction = targetLocation.toVector().subtract(casterLocation.toVector()).normalize();
        Vector rotatedDirection = new Vector(-direction.getZ(), direction.getY(), direction.getX()).normalize();

        double circleRadius = getRadius(targetFocusedAmount);
        double offsetX = circleRadius * rotatedDirection.getX();
        double offsetY = circleRadius * rotatedDirection.getY();
        double offsetZ = circleRadius * rotatedDirection.getZ();

        Location particleLocation = targetLocation.clone().subtract(offsetX, offsetY, offsetZ).subtract(direction.multiply(2));

        for (double angle = 0; angle < Math.PI * 2; angle += Math.PI / 10) {
            Vector offset = rotatedDirection.clone().multiply(circleRadius * Math.cos(angle));
            offset.setY(Math.sin(angle) * circleRadius);
            particleLocation.add(offset);
            caster.spawnParticle(Particle.DUST, particleLocation, 1, new Particle.DustOptions(color, 1));
        }
    }

    private double getRadius(double targetFocusedAmount) {
        return 0.5 - (targetFocusedAmount / 5);
    }

    public void updateColor() {
        if (color == null) {
            color = Color.fromRGB(255, 0, 0);
        }
        if (color.getRed() == 255 && color.getGreen() < 255) {
            color = Color.fromRGB(color.getRed(), Math.min(255, color.getGreen() + 6), 0);
        } else if (color.getRed() > 0 && color.getGreen() == 255) {
            color = Color.fromRGB(Math.max(0, color.getRed() - 6), color.getGreen(), 0);
        }
    }
}
