package me.mykindos.betterpvp.champions.item.thornfang;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.EqualsAndHashCode;
import me.mykindos.betterpvp.champions.Champions;
import me.mykindos.betterpvp.core.client.repository.ClientManager;
import me.mykindos.betterpvp.core.cooldowns.CooldownManager;
import me.mykindos.betterpvp.core.effects.EffectManager;
import me.mykindos.betterpvp.core.item.Item;
import me.mykindos.betterpvp.core.item.ItemFactory;
import me.mykindos.betterpvp.core.item.ItemKey;
import me.mykindos.betterpvp.core.item.ItemRarity;
import me.mykindos.betterpvp.core.item.component.impl.ability.AbilityContainerComponent;
import me.mykindos.betterpvp.core.item.config.Config;
import me.mykindos.betterpvp.core.item.model.WeaponItem;
import me.mykindos.betterpvp.core.utilities.model.Reloadable;

import java.util.List;

@Singleton
@EqualsAndHashCode(callSuper = true)
@ItemKey("champions:thornfang")
public class Thornfang extends WeaponItem implements Reloadable {

    private final Vipersprint vipersprint;
    private final Needlegrasp needlegrasp;
    private final HuntersBrand huntersBrand;

    @Inject
    private Thornfang(Champions champions, CooldownManager cooldownManager, ClientManager clientManager, EffectManager effectManager, ItemFactory itemFactory) {
        super(champions, "Thornfang", Item.model("thornfang"), ItemRarity.MYTHICAL, List.of(Group.MELEE));

        // Create Hunter's Brand first
        this.huntersBrand = new HuntersBrand(champions, itemFactory, effectManager, this);

        this.vipersprint = new Vipersprint(champions, cooldownManager, clientManager, effectManager);

        // Pass huntersBrand to Needlegrasp
        this.needlegrasp = new Needlegrasp(champions, cooldownManager, huntersBrand);

        addBaseComponent(AbilityContainerComponent.builder()
                .ability(vipersprint)
                .ability(needlegrasp)
                .ability(huntersBrand)
                .build());
    }

    @Override
    public void reload() {
        super.reload();
        final Config config = Config.item(Champions.class, this);

        this.vipersprint.setCooldown(config.getConfig("vipersprint.cooldown", 2.5, Double.class));
        this.vipersprint.setDuration(config.getConfig("vipersprint.duration", 0.2, Double.class));
        this.vipersprint.setSpeed(config.getConfig("vipersprint.speed", 3.1, Double.class));
        this.vipersprint.setDamage(config.getConfig("vipersprint.damage", 1.0, Double.class));
        this.vipersprint.setPoisonAmplifier(config.getConfig("vipersprint.poison-level", 2, Integer.class));
        this.vipersprint.setPoisonSeconds(config.getConfig("vipersprint.poison-seconds", 2.0, Double.class));

        this.needlegrasp.setCooldown(config.getConfig("needlegrasp.cooldown", 8.0, Double.class));
        this.needlegrasp.setDamage(config.getConfig("needlegrasp.extra-damage", 0.0, Double.class));
        this.needlegrasp.setHitboxSize(config.getConfig("needlegrasp.hitbox-size", 0.6, Double.class));
        this.needlegrasp.setSpeed(config.getConfig("needlegrasp.speed", 30.0, Double.class));
        this.needlegrasp.setAirDuration(config.getConfig("needlegrasp.air-duration", 0.8, Double.class));
        this.needlegrasp.setPullDuration(config.getConfig("needlegrasp.pull-duration", 1.5, Double.class));
        this.needlegrasp.setPullSpeed(config.getConfig("needlegrasp.pull-speed", 30.0, Double.class));
        this.needlegrasp.setRecoilStrength(config.getConfig("needlegrasp.recoil-strength", 1.2, Double.class));
        this.needlegrasp.setGracePeriodSeconds(config.getConfig("needlegrasp.grace-period-seconds", 1.0, Double.class));

        this.huntersBrand.setPoisonBonusDamage(config.getConfig("huntersbrand.poison-bonus-damage", 3.0, Double.class));
        this.huntersBrand.setResetCounterTimeoutSeconds(config.getConfig("huntersbrand.reset-counter-timeout", 5.0, Double.class));
        this.huntersBrand.setFrenzyDurationSeconds(config.getConfig("huntersbrand.frenzy-duration", 2.0, Double.class));
        this.huntersBrand.setFrenzyLevel(config.getConfig("huntersbrand.frenzy-level", 3, Integer.class));
    }
} 