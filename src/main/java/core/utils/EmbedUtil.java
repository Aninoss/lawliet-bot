package core.utils;

import commands.Command;
import commands.runnables.utilitycategory.AlertsCommand;
import constants.Emojis;
import constants.LogStatus;
import core.TextManager;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;

import java.time.Instant;
import java.util.Locale;

public class EmbedUtil {

    public static EmbedBuilder addNoResultsLog(EmbedBuilder eb, Locale locale, String searchString) {
        return addLog(eb, LogStatus.FAILURE, TextManager.getString(locale, TextManager.GENERAL, "no_results_description", searchString));
    }

    public static EmbedBuilder addTrackerRemoveLog(EmbedBuilder eb, Locale locale) {
        return addLog(eb, LogStatus.WARNING, TextManager.getString(locale, TextManager.GENERAL, "tracker_remove"));
    }

    public static EmbedBuilder addTrackerNoteLog(Locale locale, Server server, User user, EmbedBuilder eb, String prefix, String trigger) {
        if (PermissionUtil.getMissingPermissionListForUser(server, null, user, Command.getClassProperties(AlertsCommand.class).userPermissions()).isEmpty()) {
            addLog(eb, LogStatus.WARNING, TextManager.getString(locale, TextManager.GENERAL, "tracker", prefix, trigger));
        }
        return eb;
    }

    public static EmbedBuilder addLog(EmbedBuilder eb, LogStatus logStatus, String log) {
        if (log != null && log.length() > 0) {
            String add = "";
            if (logStatus != null) {
                switch (logStatus) {
                    case FAILURE:
                        add = "❌ ";
                        break;

                    case SUCCESS:
                        add = "✅ ";
                        break;

                    case WIN:
                        add = "\uD83C\uDF89 ";
                        break;

                    case LOSE:
                        add = "☠️ ";
                        break;

                    case WARNING:
                        add = "⚠️️ ";
                        break;

                    case TIME:
                        add = "⏲️ ";
                        break;
                }
            }
            eb.addField(Emojis.EMPTY_EMOJI, "`" + add + log + "`");
        }

        return eb;
    }

    public static EmbedBuilder addReminaingTime(Locale locale, EmbedBuilder eb, Instant instant) {
        if (instant.isAfter(Instant.now()))
            eb.setTimestamp(instant);
        return eb;
    }

    public static EmbedBuilder setFooter(EmbedBuilder eb, Command command) {
        if (command.getStarterMessage() != null)
            eb.setFooter(command.getStarterMessage().getUserAuthor().get().getDiscriminatedName());
        return eb;
    }

    public static EmbedBuilder setFooter(EmbedBuilder eb, Command command, String footer) {
        if (footer == null || footer.isEmpty())
            return setFooter(eb, command);
        if (command.getStarterMessage() != null)
            eb.setFooter(command.getStarterMessage().getUserAuthor().get().getDiscriminatedName() + " | " + footer);
        return eb;
    }

}
