package me.mykindos.betterpvp.core.menu.dialog;

import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilFont;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.ShadowColor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DisplayName("DialogCompiler")
class DialogCompilerTest {

    private static Component ui(String text) {
        return Component.text(text).font(Resources.Font.UI);
    }

    private static boolean isNewline(Component child) {
        return child instanceof TextComponent text && text.content().equals("\n");
    }

    private static int indexOf(List<Component> children, String content) {
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) instanceof TextComponent text && text.content().equals(content)) {
                return i;
            }
        }
        throw new AssertionError("No child with content " + content);
    }

    /** Line index and the advance from the line start up to the child at {@code index}. */
    private static int[] position(List<Component> children, int index) {
        int line = 0;
        int advance = 0;
        for (int i = 0; i < index; i++) {
            if (isNewline(children.get(i))) {
                line++;
                advance = 0;
            } else {
                advance += UtilFont.componentWidth(children.get(i));
            }
        }
        return new int[]{line, advance};
    }

    @Test
    @DisplayName("AC1: an element at (x, y) starts x pixels into the line holding y, in the offset font for the rest")
    void ac1_elementLandsAtItsPosition() {
        final DialogCanvas canvas = new DialogCanvas(200);
        canvas.text(37, 20, ui("Tab"));

        final List<Component> children = DialogCompiler.body(canvas, index -> null).children();
        final int index = indexOf(children, "Tab");
        final int[] position = position(children, index);

        assertEquals(2, position[0]);
        assertEquals(37, position[1]);
        assertEquals(Key.key("betterpvp", "rpg/down_2"), children.get(index).font());
    }

    @Test
    @DisplayName("AC2: elements on one line compile left to right and each lands at its own x")
    void ac2_lineCompilesLeftToRight() {
        final DialogCanvas canvas = new DialogCanvas(200);
        canvas.text(50, 0, ui("B"));
        canvas.text(10, 0, ui("A"));

        final List<Component> children = DialogCompiler.body(canvas, index -> null).children();
        final int a = indexOf(children, "A");
        final int b = indexOf(children, "B");

        assertTrue(a < b);
        assertEquals(10, position(children, a)[1]);
        assertEquals(50, position(children, b)[1]);
    }

    @Test
    @DisplayName("AC3: the backdrop title has zero net advance and no shadow")
    void ac3_backdropIsZeroAdvanceWithoutShadow() {
        final DialogCanvas backdrop = new DialogCanvas(200);
        backdrop.text(30, 0, ui("Skills"));

        final Component title = DialogCompiler.backdrop(backdrop, 200);
        final List<Component> children = title.children();

        assertEquals(0, UtilFont.componentWidth(title));
        assertEquals(ShadowColor.none(), title.shadowColor());
        // The title draws from 15 px left of the screen centre, the canvas from half its width left of it.
        assertEquals(30 - 100 + 15, position(children, indexOf(children, "Skills"))[1]);
    }

    @Test
    @DisplayName("AC4: the body has enough lines for the lowest element to fit inside it")
    void ac4_bodyFitsTallestElement() {
        final DialogCanvas canvas = new DialogCanvas(200);
        canvas.place(0, 30, ui("x"), 10, 40);

        final Component body = DialogCompiler.body(canvas, index -> null);
        final long newlines = body.children().stream().filter(DialogCompilerTest::isNewline).count();

        assertEquals(8, DialogCompiler.lines(canvas));
        assertEquals(7, newlines);
    }
}
