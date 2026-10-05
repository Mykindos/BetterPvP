package me.mykindos.betterpvp.core.menu.dialog;

import com.google.inject.Singleton;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.input.DialogInput;
import io.papermc.paper.registry.data.dialog.input.NumberRangeDialogInput;
import io.papermc.paper.registry.data.dialog.input.SingleOptionDialogInput;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import org.bukkit.entity.Player;

import java.util.List;

/**
 * Builds a Paper dialog from a {@link CompiledDialog} and shows it. Clicks keep the screen open, so the server
 * decides what happens next: re-render, open another screen or close.
 */
@Singleton
public class PaperDialogSender implements DialogSender {

    @Override
    public void show(Player player, CompiledDialog compiled) {
        final DialogBase base = DialogBase.builder(compiled.getTitle())
                .externalTitle(compiled.getName())
                .canCloseWithEscape(compiled.isEscapable())
                .pause(false)
                .afterAction(DialogBase.DialogAfterAction.NONE)
                .body(List.of(DialogBody.plainMessage(compiled.getBody(), compiled.getBodyWidth())))
                .inputs(compiled.getFields().stream().map(PaperDialogSender::input).toList())
                .build();
        final ActionButton exit = compiled.getExit() == null ? null : button(compiled.getExit());
        final DialogType type = compiled.getButtons().isEmpty()
                ? (exit == null ? DialogType.notice() : DialogType.notice(exit))
                : DialogType.multiAction(compiled.getButtons().stream().map(PaperDialogSender::button).toList(), exit, compiled.getColumns());
        player.showDialog(Dialog.create(factory -> factory.empty().base(base).type(type)));
    }

    @Override
    public void close(Player player) {
        player.closeDialog();
    }

    private static ActionButton button(CompiledDialog.Button button) {
        final ActionButton.Builder builder = ActionButton.builder(button.getLabel()).width(button.getWidth());
        if (button.getTooltip() != null) {
            builder.tooltip(button.getTooltip());
        }
        if (button.getAction() != null) {
            builder.action(DialogAction.customClick(button.getAction(), null));
        }
        return builder.build();
    }

    private static DialogInput input(DialogField field) {
        return switch (field) {
            case DialogField.Text text -> DialogInput.text(text.getKey(), text.getLabel())
                    .width(text.getWidth())
                    .maxLength(text.getMaxLength())
                    .initial(text.getInitial())
                    .build();
            case DialogField.Toggle toggle -> DialogInput.bool(toggle.getKey(), toggle.getLabel())
                    .initial(toggle.isInitial())
                    .build();
            case DialogField.Slider slider -> {
                final NumberRangeDialogInput.Builder builder = DialogInput.numberRange(slider.getKey(), slider.getLabel(), slider.getStart(), slider.getEnd())
                        .width(slider.getWidth())
                        .step(slider.getStep());
                if (slider.getInitial() != null) {
                    builder.initial(slider.getInitial());
                }
                yield builder.build();
            }
            case DialogField.Choice choice -> DialogInput.singleOption(choice.getKey(), choice.getLabel(), choice.getOptions().stream()
                            .map(option -> SingleOptionDialogInput.OptionEntry.create(option.getId(), option.getDisplay(), option.getId().equals(choice.getInitial())))
                            .toList())
                    .width(choice.getWidth())
                    .build();
        };
    }
}
