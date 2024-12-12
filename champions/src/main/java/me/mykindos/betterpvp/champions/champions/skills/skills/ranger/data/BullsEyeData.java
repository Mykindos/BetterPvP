package me.mykindos.betterpvp.champions.champions.skills.skills.ranger.data;

import lombok.Getter;
import lombok.Setter;
import me.mykindos.betterpvp.champions.champions.skills.data.ChargeData;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

@Getter
public class BullsEyeData {

    private final Player caster;
    private final ChargeData casterCharge;

    @Setter
    private LivingEntity target;

    @Setter
    private ChargeData targetFocused;
    private Color color;
    @Setter
    private long lastChargeTime;

    public BullsEyeData(Player caster, ChargeData casterCharge, LivingEntity target, ChargeData targetFocused, Color color) {
        this.caster = caster;
        this.casterCharge = casterCharge;
        this.target = target;
        this.targetFocused = targetFocused;
        this.color = color;
        this.lastChargeTime = System.currentTimeMillis();
    }

    public boolean hasTarget() {
        return target != null;
    }

    public void spawnFocusingParticles() {
        float charge = casterCharge.getCharge();
        updateColor();

        Location casterLocation = caster.getLocation().add(0, caster.getHeight() / 3, 0);
        Location targetLocation = target.getLocation().add(0, target.getHeight() / 3, 0);

        Vector direction = targetLocation.toVector().subtract(casterLocation.toVector()).normalize();
        Vector rotatedDirection = new Vector(-direction.getZ(), direction.getY(), direction.getX()).normalize();

        double circleRadius = getRadius(charge);
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
        float charge = casterCharge.getCharge();
        int red = (int) Math.min(255, 255 * (1 - charge));
        int green = (int) Math.min(255, 255 * charge);
        this.color = Color.fromRGB(red, green, 0);
    }

    public void decayCharge(double decayRate) {
        casterCharge.setCharge(Math.max(0, casterCharge.getCharge() - (float) decayRate));
        targetFocused.setCharge(Math.max(0, targetFocused.getCharge() - (float) decayRate));

        if (casterCharge.getCharge() == 0) {
            target = null;
        }
    }
}