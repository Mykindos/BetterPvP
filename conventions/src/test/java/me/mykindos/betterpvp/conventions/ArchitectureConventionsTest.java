package me.mykindos.betterpvp.conventions;

import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.freeze.FreezingArchRule;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.GeneralCodingRules.NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS;

/**
 * House rules checked on the compiled classes. Each rule is frozen: breaks that already exist are recorded in
 * {@code archunit_store} and only new ones fail. Run {@code ./gradlew :conventions:test -PupdateBaseline} to record
 * the store again after fixing old breaks.
 */
class ArchitectureConventionsTest {

    private static final String LISTENER = "org.bukkit.event.Listener";
    private static final String UPDATE_EVENT = "me.mykindos.betterpvp.core.framework.updater.UpdateEvent";
    private static final String BPVP_LISTENER = "me.mykindos.betterpvp.core.listener.BPvPListener";

    @Test
    void noRecords() {
        check(noClasses().should().beAssignableTo(Record.class)
                .as("no Java records")
                .because("data holders are Lombok @Value or @Data classes"));
    }

    @Test
    void updateEventMethodsArePublic() {
        check(methods().that().areAnnotatedWith(UPDATE_EVENT).should().haveModifier(JavaModifier.PUBLIC)
                .as("@UpdateEvent methods are public")
                .because("UpdateEventExecutor finds them with getMethods(), which skips non-public methods"));
    }

    @Test
    void updateEventMethodsLiveInListeners() {
        check(methods().that().areAnnotatedWith(UPDATE_EVENT).should().beDeclaredInClassesThat().implement(LISTENER)
                .as("@UpdateEvent methods live in Listener classes")
                .because("only registered listeners are ticked"));
    }

    @Test
    void bpvpListenersAreListeners() {
        check(classes().that().areAnnotatedWith(BPVP_LISTENER).should().implement(LISTENER)
                .as("@BPvPListener classes implement Listener")
                .because("the listener loader registers them with Bukkit and the update executor"));
    }

    @Test
    void eventHandlersLiveInListeners() {
        check(methods().that().areAnnotatedWith("org.bukkit.event.EventHandler")
                .should().beDeclaredInClassesThat().implement(LISTENER)
                .as("@EventHandler methods live in Listener classes")
                .because("Bukkit only calls handlers on registered listeners"));
    }

    @Test
    void noNewDelayedActionEvents() {
        check(noClasses().that().resideOutsideOfPackage("me.mykindos.betterpvp.core.framework.delayedactions..")
                .should().dependOnClassesThat()
                .resideInAPackage("me.mykindos.betterpvp.core.framework.delayedactions..")
                .as("nothing new builds on the delayed action framework")
                .because("PlayerDelayedActionEvent is being retired, features own their controller and interrupts"));
    }

    @Test
    void bossBarViewersAreTrackedLocally() {
        check(noClasses().should().callMethod("net.kyori.adventure.bossbar.BossBar", "viewers")
                .as("no BossBar.viewers() calls")
                .because("boss bar viewers are tracked in the feature's own Set<Player>"));
    }

    @Test
    void noPrintStackTrace() {
        check(noClasses().should().callMethod(Throwable.class, "printStackTrace")
                .as("no printStackTrace()")
                .because("errors go through the logger, ending in .submit()"));
    }

    @Test
    void noStandardStreams() {
        check(NO_CLASSES_SHOULD_ACCESS_STANDARD_STREAMS
                .as("no System.out or System.err")
                .because("output goes through the logger, ending in .submit()"));
    }

    private static void check(ArchRule rule) {
        FreezingArchRule.freeze(rule.allowEmptyShould(true)).check(Codebase.classes());
    }
}
