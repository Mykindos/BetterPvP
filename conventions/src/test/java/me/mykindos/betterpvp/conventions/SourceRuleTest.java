package me.mykindos.betterpvp.conventions;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SourceRuleTest {

    private static List<Integer> lines(SourceRule rule, String source) {
        return rule.violations(new JavaText(source));
    }

    @Test
    void fullyQualifiedNameIsFlaggedInCode() {
        assertEquals(List.of(2), lines(SourceRule.FULLY_QUALIFIED_NAME, """
                class A {
                    java.util.List<String> names;
                }
                """));
    }

    @Test
    void fullyQualifiedNameIsAllowedInImportsStringsAndComments() {
        assertEquals(List.of(), lines(SourceRule.FULLY_QUALIFIED_NAME, """
                package me.mykindos.betterpvp.core;
                import java.util.List;
                class A {
                    // see java.util.List
                    String type = "java.util.List";
                    List<String> names;
                }
                """));
    }

    @Test
    void fullyQualifiedNameCanBeAllowedOnItsLine() {
        assertEquals(List.of(), lines(SourceRule.FULLY_QUALIFIED_NAME, """
                class A {
                    java.awt.List list; // conventions:allow FULLY_QUALIFIED_NAME
                }
                """));
    }

    @Test
    void allowCommentOnlyCoversItsOwnRule() {
        assertEquals(List.of(2), lines(SourceRule.FULLY_QUALIFIED_NAME, """
                class A {
                    java.awt.List list; // conventions:allow BANNER_COMMENT
                }
                """));
    }

    @Test
    void bannerCommentIsFlagged() {
        assertEquals(List.of(2, 3), lines(SourceRule.BANNER_COMMENT, """
                class A {
                    // ─── Fields ───
                    // =====
                }
                """));
    }

    @Test
    void ordinaryCommentIsNotABanner() {
        assertEquals(List.of(), lines(SourceRule.BANNER_COMMENT, """
                class A {
                    // a - b is the gap
                    int gap = a -- b;
                }
                """));
    }

    @Test
    void inlineArrayLoopIsFlagged() {
        assertEquals(List.of(2), lines(SourceRule.INLINE_ARRAY_LOOP, """
                void spawn() {
                    for (double offset : new double[]{-1, 0, 1}) {
                    }
                }
                """));
    }

    @Test
    void loopOverAFieldIsFine() {
        assertEquals(List.of(), lines(SourceRule.INLINE_ARRAY_LOOP, """
                void spawn() {
                    for (double offset : OFFSETS) {
                    }
                }
                """));
    }

    @Test
    void hardcodedMessageIsFlagged() {
        assertEquals(List.of(2), lines(SourceRule.HARDCODED_PLAYER_TEXT, """
                void tell(Player player) {
                    UtilMessage.simpleMessage(player, "Clans", "You joined the clan.");
                }
                """));
    }

    @Test
    void ac1_messageStartingWithMiniMessageTagIsHardcoded() {
        assertEquals(List.of(2), lines(SourceRule.HARDCODED_PLAYER_TEXT, """
                void tell(Player player) {
                    UtilMessage.message(player, "Clans", "<gray>Loaded");
                }
                """));
    }

    @Test
    void indentedMessageStartingWithMiniMessageTagIsHardcoded() {
        assertEquals(List.of(2), lines(SourceRule.HARDCODED_PLAYER_TEXT, """
                void tell(Player player) {
                    UtilMessage.message(player, "Clans", "  <gray>Loaded");
                }
                """));
    }

    @Test
    void translatedMessageAndCommentedMessageAreFine() {
        assertEquals(List.of(), lines(SourceRule.HARDCODED_PLAYER_TEXT, """
                void tell(Player player) {
                    UtilMessage.message(player, "Clans", Translations.component("clans.joined"));
                    // UtilMessage.simpleMessage(player, "Clans", "Old text");
                }
                """));
    }

    @Test
    void refactorNarrativeIsFlagged() {
        assertEquals(List.of(2, 3), lines(SourceRule.REFACTOR_NARRATIVE, """
                class A {
                    // Previously this ran every tick.
                    /** Replaces the old loader. */
                }
                """));
    }

    @Test
    void usedToInAnOrdinarySentenceIsFine() {
        assertEquals(List.of(), lines(SourceRule.REFACTOR_NARRATIVE, """
                class A {
                    /** The key used to look up the bundle. */
                    String note = "previously";
                }
                """));
    }

    @Test
    void ac3_noLongerInACommentIsFine() {
        assertEquals(List.of(), lines(SourceRule.REFACTOR_NARRATIVE, """
                class A {
                    // The handle is no longer valid once the chunk unloads.
                }
                """));
    }

    @Test
    void unsubmittedLogIsFlagged() {
        assertEquals(List.of(4), lines(SourceRule.UNSUBMITTED_LOG, """
                @CustomLog
                class A {
                    void run() {
                        log.warn("Missing {}", name);
                    }
                }
                """));
    }

    @Test
    void submittedLogAcrossLinesIsFine() {
        assertEquals(List.of(), lines(SourceRule.UNSUBMITTED_LOG, """
                @CustomLog
                class A {
                    void run() {
                        log.error("Failed to load {}", name, e)
                                .submit();
                        log.info("Loaded {}", call(a, b)).submit();
                    }
                }
                """));
    }

    @Test
    void otherLoggersAreIgnored() {
        assertEquals(List.of(), lines(SourceRule.UNSUBMITTED_LOG, """
                @Slf4j
                class A {
                    void run() {
                        log.warn("Missing {}", name);
                    }
                }
                """));
    }

    @Test
    void textBlocksAreNotCode() {
        assertEquals(List.of(), lines(SourceRule.FULLY_QUALIFIED_NAME, "class A {\n    String s = \"\"\"\n"
                + "        java.util.List\n        \"\"\";\n}\n"));
    }
}
