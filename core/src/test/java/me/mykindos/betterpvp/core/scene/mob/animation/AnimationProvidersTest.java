package me.mykindos.betterpvp.core.scene.mob.animation;

import me.mykindos.betterpvp.core.scene.mob.SceneMob;
import org.bukkit.entity.LivingEntity;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnimationProvidersTest {

    private final SceneMob mob = mock(SceneMob.class);

    private static AnimationProvider recording(List<Boolean> entries, String clip) {
        return new AnimationProvider() {
            @Override
            public String resolve(SceneMob mob) {
                return resolve(mob, true);
            }

            @Override
            public String resolve(SceneMob mob, boolean reentry) {
                entries.add(reentry);
                return clip;
            }
        };
    }

    @Test
    void ac24_fixedAlwaysReturnsItsClip() {
        final AnimationProvider provider = AnimationProviders.fixed("idle");

        assertEquals("idle", provider.resolve(mob));
        assertEquals("idle", provider.resolve(mob, true));
        assertEquals("idle", provider.resolve(mob, false));
    }

    @Test
    void ac24_randomPicksAgainOnlyOnAFreshEntry() {
        final AnimationProvider provider = AnimationProviders.random("a", "b", "c", "d", "e", "f", "g", "h");
        final Set<String> picks = new HashSet<>();

        for (int i = 0; i < 50; i++) {
            final String pick = provider.resolve(mob, true);
            picks.add(pick);
            for (int refresh = 0; refresh < 5; refresh++) {
                assertEquals(pick, provider.resolve(mob, false));
            }
        }

        assertTrue(picks.size() > 1, "fresh entries pick again");
    }

    @Test
    void ac24_sequentialStartsAtTheFirstClipAndStepsOnlyOnAFreshEntry() {
        final AnimationProvider provider = AnimationProviders.sequential("one", "two", "three");

        assertEquals("one", provider.resolve(mob, true));
        assertEquals("one", provider.resolve(mob, false));
        assertEquals("two", provider.resolve(mob, true));
        assertEquals("two", provider.resolve(mob, false));
        assertEquals("three", provider.resolve(mob, true));
        assertEquals("one", provider.resolve(mob, true));
    }

    @Test
    void ac24_aRefreshBeforeAnyPickStillPicks() {
        assertEquals("one", AnimationProviders.sequential("one", "two").resolve(mob, false));
    }

    @Test
    void ac24_whenChecksItsConditionOnEveryCallAndPassesTheFreshEntryFlag() {
        final AtomicBoolean condition = new AtomicBoolean(true);
        final AtomicInteger checks = new AtomicInteger();
        final List<Boolean> yes = new ArrayList<>();
        final List<Boolean> no = new ArrayList<>();
        final AnimationProvider provider = AnimationProviders.when(unused -> {
            checks.incrementAndGet();
            return condition.get();
        }, recording(yes, "yes"), recording(no, "no"));

        assertEquals("yes", provider.resolve(mob, true));
        assertEquals("yes", provider.resolve(mob, false));
        condition.set(false);
        assertEquals("no", provider.resolve(mob, false));
        assertEquals("no", provider.resolve(mob, true));

        assertEquals(4, checks.get());
        assertEquals(List.of(true, false), yes);
        assertEquals(List.of(false, true), no);
    }

    @Test
    void ac24_whenTakesSingleClips() {
        final AtomicBoolean condition = new AtomicBoolean(true);
        final AnimationProvider provider = AnimationProviders.when(unused -> condition.get(), "yes", "no");

        assertEquals("yes", provider.resolve(mob));
        condition.set(false);
        assertEquals("no", provider.resolve(mob));
    }

    @Test
    void ac24_whenTargetingUsesItsFirstBranchWhileTheMobHasATarget() {
        final AnimationProvider provider = AnimationProviders.whenTargeting("idle_combat", "idle");

        assertEquals("idle", provider.resolve(mob, false));
        when(mob.getCurrentTarget()).thenReturn(mock(LivingEntity.class));
        assertEquals("idle_combat", provider.resolve(mob, false));
    }

    @Test
    void ac24_anEmptyClipListOrANullIdIsRefused() {
        assertThrows(IllegalArgumentException.class, AnimationProviders::random);
        assertThrows(IllegalArgumentException.class, AnimationProviders::sequential);
        assertThrows(NullPointerException.class, () -> AnimationProviders.fixed(null));
    }
}
