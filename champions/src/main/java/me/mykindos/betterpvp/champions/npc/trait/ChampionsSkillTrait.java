package me.mykindos.betterpvp.champions.npc.trait;

import me.mykindos.betterpvp.core.components.champions.SkillType;
import net.citizensnpcs.api.persistence.Persist;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.trait.TraitName;

import java.util.Random;

@TraitName("champions_skills")
public class ChampionsSkillTrait extends Trait {

    private static final Random random = new Random();

    @Persist
    private final SkillType skillType;
    @Persist
    long minMillisInterval = 3 * 20;
    @Persist
    long maxMillisInterval = 7 * 20;
    private long nextUse = -1;

    public ChampionsSkillTrait(SkillType skillType, long minMillisInterval, long maxMillisInterval) {
        super("champions_skills");
        this.skillType = skillType;
        this.minMillisInterval = minMillisInterval;
        this.maxMillisInterval = maxMillisInterval;
    }

    @Override
    public void run() {
        final long time = System.currentTimeMillis();
        if (!npc.isSpawned() || npc.getEntity() == null || npc.getEntity().isDead() || time < nextUse) {
            return;
        }

        this.nextUse = time + random.nextInt((int) (maxMillisInterval - minMillisInterval)) + minMillisInterval;
    }
}
