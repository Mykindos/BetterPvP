package me.mykindos.betterpvp.core.quest.conversation;

import lombok.Value;
import me.mykindos.betterpvp.core.utilities.Resources;
import me.mykindos.betterpvp.core.utilities.UtilFont;
import me.mykindos.betterpvp.core.utilities.model.display.FontCanvas;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders a single conversation node into a centered action-bar {@link Component}: the letterboxed
 * dialogue box, the typewriter-revealed body text, the speaker nameplate, and (once the body is
 * fully revealed) the response list and input hint.
 * <p>
 * Pure with respect to conversation state — it reads a {@link ConvNodeData} plus the current
 * typewriter/selection position and returns the drawn component; flow and side effects stay in
 * {@link ConversationManager}. Every element is positioned with {@link FontCanvas#space(int)} cursor
 * shifts that are paid back so the box's net advance — which the client centers on — never changes
 * as text types out.
 */
public class ConversationRenderer {

    // dialogue.png is 209px wide; its advance is width + 1px glyph spacing. Text is inset by the padding.
    private static final int BOX_WIDTH = 209;
    private static final int BOX_PADDING = 10;

    Component render(ConvNodeData data, int shownChars, int selectedIndex) {
        final String body = data.getBody();
        final String shown = body.substring(0, Math.max(0, shownChars));

        final FontCanvas canvas = new FontCanvas();
        drawBox(canvas);
        drawBody(canvas, body, shown);
        drawNameplate(canvas, data.getSpeaker());
        if (shownChars >= body.length()) {
            drawResponsesAndHint(canvas, data.getResponses(), selectedIndex);
        }
        return canvas.build();
    }

    // Letterbox bars + dialogue background, then step inside the box. The step is paid back after the
    // body (in drawBody) so the box's net advance is untouched and it never shifts while typing.
    private void drawBox(FontCanvas canvas) {
        final TextComponent.Builder bars = Component.text();
        int offset = 0;
        for (int i = 0; i < 12; i++) {
            bars.append(Component.text("\uE000", NamedTextColor.BLACK).font(FontCanvas.font("conversation"))
                    .append(Component.translatable("space.-1").font(Resources.Font.SPACE)));
            offset += 256;
        }
        canvas.append(Component.translatable("offset.-" + offset / 2, bars.build()).font(Resources.Font.SPACE));

        canvas.space(-offset).glyph('\uE001', NamedTextColor.WHITE, "conversation");
        canvas.space(-(BOX_WIDTH + 1 - BOX_PADDING));
    }

    // Word-wrap the full body up front so words never split across lines; only the typed portion of
    // each line is drawn. Each line is drawn then rewound by its own width, so every line starts at the
    // same X and contributes zero net advance.
    private void drawBody(FontCanvas canvas, String body, String shown) {
        final List<WrappedLine> lines = wrap(body, BOX_WIDTH - BOX_PADDING * 2);

        int heightOffset = 18;
        for (WrappedLine line : lines) {
            final String text = line.getText().substring(0, Math.clamp(shown.length() - line.getStart(), 0, line.getText().length()));
            if (!text.isEmpty()) {
                canvas.text(text, NamedTextColor.WHITE, lineFont(heightOffset)).space(-UtilFont.textWidth(text));
            }
            heightOffset -= 8 + 2; // 8px glyph + 2px line gap
        }

        // Pay back the step into the box so the body contributes zero net advance.
        canvas.space(BOX_WIDTH + 1 - BOX_PADDING);
    }

    private void drawNameplate(FontCanvas canvas, String speaker) {
        final int width = UtilFont.textWidth(speaker);
        final int mids = width / 2; // the mid-nameplate glyph is 2px wide per char

        canvas.space(-(BOX_WIDTH - 10));
        canvas.glyph('\uE004', NamedTextColor.WHITE, "conversation").space(-1);
        for (int i = 0; i < mids; i++) {
            canvas.glyph('\uE005', NamedTextColor.WHITE, "conversation").space(-1);
        }
        canvas.glyph('\uE006', NamedTextColor.WHITE, "conversation");
        canvas.space(-(width + 3));
        canvas.text(speaker, TextColor.color(0x3aba44), "offset/up_32");

        // Pay back the net cursor movement: -(BOX_WIDTH-10) jump, +3 left cap, +2 per mid, +4 right cap,
        // and the name rewind/draw pair nets -3.
        final int net = -(BOX_WIDTH - 10) + 3 + mids * 2 + 4 - 3;
        canvas.space(-net);
    }

    // Responses (if any) followed by the input hint — only reached once the body is fully revealed.
    private void drawResponsesAndHint(FontCanvas canvas, List<ConvResponse> responses, int selectedIndex) {
        String hint = "to close";
        if (!responses.isEmpty()) {
            drawResponses(canvas, responses, selectedIndex);
            hint = "to select";
        }

        // Input instruction. Pay back: +6 gap, +8 key glyph, +3 gap, +label width.
        canvas.space(6).glyph('\uE7EB', "input/up_40").space(3).text(hint, "offset/up_32");
        canvas.space(-(6 + 8 + 3 + UtilFont.textWidth(hint)));
    }

    private void drawResponses(FontCanvas canvas, List<ConvResponse> responses, int selectedIndex) {
        final int responseWidth = 134;
        canvas.space(-(responseWidth / 2));
        canvas.glyph('\uE002', NamedTextColor.WHITE, "conversation");
        canvas.space(-(responseWidth - 12));

        // Only three row offsets exist (69/59/49); a single response sits on the middle row (centered).
        int yOffset = responses.size() == 1 ? 59 : 69;
        int lastLineWidth = 0;
        for (int i = 0; i < responses.size(); i++) {
            final ConvResponse response = responses.get(i);
            final boolean selected = i == selectedIndex;
            if (lastLineWidth != 0) {
                canvas.space(-lastLineWidth);
            }
            if (selected) {
                drawHandMarker(canvas, yOffset);
            } else {
                drawNumberMarker(canvas, yOffset);
            }
            canvas.text(response.getLabel(), selected ? NamedTextColor.YELLOW : NamedTextColor.WHITE, "offset/up_" + yOffset);

            yOffset -= 8 + 2;
            // charWidth already includes the 1px spacing, so the string's full advance IS textWidth.
            lastLineWidth = UtilFont.textWidth(response.getLabel()) + 2;
        }

        // Pay back the block's net movement: -(w/2) jump, +(w+1) box, -(w-12) step inside, telescoped
        // line rewinds leave the last line's width, and both marker trios net zero.
        final int net = -(responseWidth / 2) + (responseWidth + 1) - (responseWidth - 12) + lastLineWidth;
        canvas.space(-net);
    }

    // Selected rows show a pointing hand; the glyph differs per row. -10/+3 leaves a 3px gap to the label.
    private void drawHandMarker(FontCanvas canvas, int yOffset) {
        final char hand = switch (yOffset) {
            case 69 -> '\uE00A';
            case 59 -> '\uE009';
            case 49 -> '\uE008';
            default -> 't';
        };
        canvas.space(-10).glyph(hand, NamedTextColor.WHITE, "conversation").space(3);
    }

    // Unselected rows show a hotbar number; both glyph and lift font differ per row. The num glyph is
    // 10px wide (advance 11), so -10/+2 leaves the same 3px gap to the label as the hand trio.
    private void drawNumberMarker(FontCanvas canvas, int yOffset) {
        final char num = switch (yOffset) {
            case 69 -> '\uE033';
            case 59 -> '\uE034';
            case 49 -> '\uE035';
            default -> 't';
        };
        final String font = switch (yOffset) {
            case 69 -> "input/up_79";
            case 59 -> "input/up_69";
            case 49 -> "input/up_59";
            default -> "t";
        };
        canvas.space(-10).glyph(num, NamedTextColor.WHITE, font).space(2);
    }

    private static String lineFont(int heightOffset) {
        if (heightOffset == 0) return "offset/center";
        return heightOffset < 0 ? "offset/down_" + Math.abs(heightOffset) : "offset/up_" + heightOffset;
    }

    private static List<WrappedLine> wrap(String body, int maxWidth) {
        final List<WrappedLine> lines = new ArrayList<>();
        int start = 0;
        while (start < body.length()) {
            int end = start;
            int lastSpace = -1;
            int width = 0;
            while (end < body.length()) {
                final char c = body.charAt(end);
                if (width + UtilFont.charWidth(c) > maxWidth) break;
                width += UtilFont.charWidth(c);
                if (c == ' ') lastSpace = end;
                end++;
            }
            if (end < body.length() && lastSpace > start) {
                end = lastSpace; // next word would be cut off — wrap it whole onto the next line
            }
            if (end == start) end++; // single word wider than the box — hard-split it
            lines.add(new WrappedLine(start, body.substring(start, end)));
            start = end;
            while (start < body.length() && body.charAt(start) == ' ') start++;
        }
        return lines;
    }

    @Value
    private static class WrappedLine {
        int start;
        String text;
    }
}
