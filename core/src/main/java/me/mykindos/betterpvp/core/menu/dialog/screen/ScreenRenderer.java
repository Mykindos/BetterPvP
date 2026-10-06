package me.mykindos.betterpvp.core.menu.dialog.screen;

import me.mykindos.betterpvp.core.menu.dialog.CanvasElement;
import me.mykindos.betterpvp.core.menu.dialog.DialogButton;
import me.mykindos.betterpvp.core.menu.dialog.DialogCanvas;
import me.mykindos.betterpvp.core.menu.dialog.DialogClick;
import me.mykindos.betterpvp.core.menu.dialog.DialogField;
import me.mykindos.betterpvp.core.menu.dialog.DialogScreen;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.ShadowColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Turns a screen and its state into a {@link DialogScreen}: the active switch cases and when branches, one copy of a
 * repeat per list entry, fields with their bound values, and every click wired to an action.
 */
final class ScreenRenderer {

    /**
     * Colour of animated sprites. Its low three bits per channel (5, 4, 5) are the signature the pack's text shader
     * looks for, kept in step with {@code rendertype_text.vsh}.
     */
    static final TextColor ANIMATION_SIGNATURE = TextColor.color(0xFDFCFD);

    private ScreenRenderer() {
    }

    static DialogScreen render(Player player, ScreenDefinition screen, ScreenState state, GuiRegistry registry,
                               AssetTable assets, BiFunction<ActionSpec, Function<String, Object>, DialogClick> clicks) {
        final DialogCanvas backdrop = new DialogCanvas(screen.getCanvasWidth());
        final DialogCanvas canvas = new DialogCanvas(screen.getCanvasWidth(), screen.getCanvasHeight());
        final Function<String, Object> lookup = state::get;

        final RenderContext backdropContext = new RenderContext(player, screen, backdrop, 0, 0, lookup, registry, assets, clicks, null);
        renderBackdrop(screen.getBackdrop(), backdropContext);
        final RenderContext context = new RenderContext(player, screen, canvas, 0, 0, lookup, registry, assets, clicks, null);
        renderAll(screen.getElements(), context);

        final DialogScreen.DialogScreenBuilder builder = DialogScreen.builder()
                .name(screen.getName() == null ? Component.text(screen.getId()) : context.text(screen.getName()))
                .backdrop(backdrop)
                .canvas(canvas)
                .columns(screen.getColumns())
                .escapable(screen.isEscapable());
        for (ScreenDefinition.Field field : screen.getFields()) {
            builder.field(field(field, state, context));
        }
        for (ScreenDefinition.NativeButton button : screen.getButtons()) {
            builder.button(nativeButton(button, context, button.getOnClick()));
        }
        final ScreenDefinition.NativeButton exit = screen.getExit();
        if (exit != null) {
            builder.exit(nativeButton(exit, context, exit.getOnClick() == null ? new ActionSpec.Close() : exit.getOnClick()));
        }
        return builder.build();
    }

    private static void renderBackdrop(List<Node> nodes, RenderContext context) {
        for (Node node : nodes) {
            if (node instanceof Node.Box box) {
                // Backdrop art carries its height in its glyph ascent, so it sits on the title line.
                final String asset = ScreenAssets.backdrop(box.getStyle(), box.getWidth(), box.getHeight(), context.getOriginY() + box.getY());
                context.offset(0, -context.getOriginY()).art(asset, box.getX(), 0);
            } else if (node instanceof Node.Group group) {
                for (int index = 0; index < group.getChildren().size(); index++) {
                    final int[] cell = ScreenAssets.cell(group, index);
                    renderBackdrop(List.of(group.getChildren().get(index)), context.offset(cell[0], cell[1]));
                }
            } else {
                throw new IllegalArgumentException("The backdrop holds boxes and groups only, not " + node.getClass().getSimpleName());
            }
        }
    }

    static void renderAll(List<Node> nodes, RenderContext context) {
        for (Node node : nodes) {
            render(node, context);
        }
    }

    private static void render(Node node, RenderContext context) {
        switch (node) {
            case Node.Text text -> {
                final CanvasElement element = context.text(text.getX(), text.getY(), context.styled(text.getText(), text.getStyle()),
                        text.getAlign(), text.getWidth());
                interact(element, context, text.getTooltip(), text.getOnClick(), null, null);
            }
            case Node.Box box -> context.art(ScreenAssets.box(box.getStyle(), box.getWidth(), box.getHeight()), box.getX(), box.getY());
            case Node.Button button -> renderButton(button, context);
            case Node.Icon icon -> {
                final String asset = ScreenAssets.sprite(icon.getSprite(), icon.getWidth(), icon.getHeight(), icon.getFrames(), icon.getFps());
                // No shadow: its darkened colour loses the signature and would draw the whole strip.
                final Component glyph = icon.getFrames() > 1
                        ? context.glyph(asset).color(ANIMATION_SIGNATURE).shadowColor(ShadowColor.none())
                        : context.glyph(asset);
                final CanvasElement element = context.getCanvas().place(context.getOriginX() + icon.getX(),
                        context.getOriginY() + icon.getY(), glyph, icon.getWidth() + 1, icon.getHeight());
                interact(element, context, icon.getTooltip(), icon.getOnClick(), null, null);
            }
            case Node.Group group -> {
                for (int index = 0; index < group.getChildren().size(); index++) {
                    final int[] cell = ScreenAssets.cell(group, index);
                    render(group.getChildren().get(index), context.offset(cell[0], cell[1]));
                }
            }
            case Node.Repeat repeat -> {
                final Object value = context.evaluate("{" + repeat.getList() + "}");
                final List<?> entries = value instanceof List<?> list ? list : List.of();
                for (int index = 0; index < Math.min(entries.size(), repeat.getMax()); index++) {
                    final int[] cell = ScreenAssets.cell(repeat.getLayout(), index);
                    renderAll(repeat.getChildren(), context.offset(cell[0], cell[1])
                            .bind(Map.of(repeat.getAs(), entries.get(index), "index", (double) index)));
                }
            }
            case Node.Switch choice -> {
                final String value = Expressions.text(context.evaluate(choice.getValue()));
                renderAll(choice.getCases().getOrDefault(value, choice.getFallback()), context);
            }
            case Node.When when -> renderAll(context.test(when.getCondition()) ? when.getThen() : when.getOtherwise(), context);
            case Node.Use use -> {
                final ScreenDefinition.Component component = context.components().apply(context.getScreen(), use.getComponent());
                if (component == null) {
                    throw new IllegalArgumentException("Unknown component " + use.getComponent());
                }
                final Map<String, Object> params = new HashMap<>();
                use.getWith().forEach((name, template) -> params.put(name, context.evaluate(template)));
                renderAll(component.getElements(), context.offset(use.getX(), use.getY()).bind(params));
            }
            case Node.Custom custom -> {
                final ElementType type = context.registry().elementType(context.getScreen().getNamespace(), custom.getType());
                if (type == null) {
                    throw new IllegalArgumentException("Unknown element type " + custom.getType());
                }
                final Set<String> allowed = ScreenAssets.collect(context.getScreen(), custom.getAssets(),
                        context.getOriginX() + custom.getX(), context.getOriginY() + custom.getY(), context.components());
                type.render(custom, context.offset(custom.getX(), custom.getY()).restrict(allowed));
            }
        }
    }

    private static void renderButton(Node.Button button, RenderContext context) {
        final boolean selected = button.getSelected() != null && context.test(button.getSelected());
        final String style = selected && button.getSelectedStyle() != null ? button.getSelectedStyle() : button.getStyle();
        final int width = button.getWidth();
        final int height = button.getHeight();
        if (ScreenAssets.parts(ScreenAssets.box(style, width, height)) > 1) {
            throw new IllegalArgumentException("A button is at most " + ScreenAssets.MAX_GLYPH + " px wide");
        }
        final Component hover = button.getHover() == null ? null : context.glyph(ScreenAssets.hover(button.getHover(), width, height,
                context.getScreen().getCanvasWidth(), context.getScreen().getCanvasHeight(),
                context.getOriginX() + button.getX(), context.getOriginY() + button.getY()));
        final Component pressed = button.isPressed() ? context.glyph(ScreenAssets.pressed(style, width, height)) : null;

        final CanvasElement art = context.art(ScreenAssets.box(style, width, height), button.getX(), button.getY()).getFirst();
        interact(art, context, button.getTooltip(), button.getOnClick(), hover, pressed);
        if (button.getLabel() != null) {
            final String labelStyle = selected && button.getSelectedLabelStyle() != null ? button.getSelectedLabelStyle() : button.getLabelStyle();
            final Component label = context.styled(button.getLabel(), labelStyle);
            // Centred on the 7 px cap height, which leaves room for a shadow below.
            final CanvasElement text = context.text(button.getX(), button.getY() + (height - 7) / 2, label, Node.Align.CENTER, width);
            interact(text, context, button.getTooltip(), button.getOnClick(), hover, null);
        }
    }

    private static void interact(CanvasElement element, RenderContext context, TextSpec tooltip, ActionSpec action,
                                 Component hover, Component pressed) {
        if (hover != null) {
            element.hover(hover);
        } else if (tooltip != null) {
            element.tooltip(context.text(tooltip));
        }
        final DialogClick click = context.click(action);
        if (click != null) {
            element.onClick(click);
            if (pressed != null) {
                element.pressed(pressed);
            }
        }
    }

    private static DialogField field(ScreenDefinition.Field field, ScreenState state, RenderContext context) {
        final Component label = context.styled(field.getLabel(), "body");
        final Object value = state.get(field.getBind());
        return switch (field.getKind()) {
            case TEXT -> DialogField.Text.builder().key(field.getBind()).label(label).width(field.getWidth())
                    .maxLength(field.getMaxLength()).initial(value == null ? "" : Expressions.text(value)).build();
            case TOGGLE -> DialogField.Toggle.builder().key(field.getBind()).label(label).initial(Expressions.truthy(value)).build();
            case SLIDER -> DialogField.Slider.builder().key(field.getBind()).label(label).width(field.getWidth())
                    .start(field.getStart()).end(field.getEnd()).step(field.getStep())
                    .initial(value instanceof Number number ? number.floatValue() : null).build();
            case CHOICE -> {
                final DialogField.Choice.ChoiceBuilder builder = DialogField.Choice.builder().key(field.getBind()).label(label)
                        .width(field.getWidth()).initial(value == null ? field.getOptions().getFirst().getId() : Expressions.text(value));
                field.getOptions().forEach(option -> builder.option(new DialogField.Option(option.getId(), context.text(option.getLabel()))));
                yield builder.build();
            }
        };
    }

    private static DialogButton nativeButton(ScreenDefinition.NativeButton button, RenderContext context, ActionSpec action) {
        return DialogButton.builder()
                .label(context.styled(button.getLabel(), "on_primary"))
                .tooltip(button.getTooltip() == null ? null : context.text(button.getTooltip()))
                .width(button.getWidth())
                .click(context.click(action))
                .build();
    }
}
