package commands.runnables;

import commands.Command;
import commands.CommandEvent;
import commands.listeners.OnTriggerListener;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.TextManager;
import modules.fishery.FisheryStatus;
import org.jetbrains.annotations.NotNull;

public interface FisheryInterface extends OnTriggerListener {

    boolean onFisheryAccess(CommandEvent event, String args) throws Throwable;

    @Override
    default boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        return onFisheryTrigger(event, args);
    }

    default boolean onFisheryTrigger(CommandEvent event, String args) throws Throwable {
        Command command = (Command) this;
        FisheryStatus status = command.getGuildEntity().getFishery().getFisheryStatus();
        if (status == FisheryStatus.ACTIVE) {
            return onFisheryAccess(event, args);
        } else {
            command.drawMessageNew(
                    EmbedFactory.getEmbedError(
                            command,
                            TextManager.getString(command.getLocale(), TextManager.GENERAL, "fishing_notactive_description").replace("{PREFIX}", command.getPrefix()),
                            TextManager.getString(command.getLocale(), TextManager.GENERAL, "fishing_notactive_title")
                    )
            ).exceptionally(ExceptionLogger.get());
            return false;
        }
    }

}
