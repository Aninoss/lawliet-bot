package core.utils;

import commands.Command;
import commands.runnables.utilitycategory.AlertsCommand;
import constants.Emojis;
import constants.LogStatus;
import core.TextManager;
import mysql.modules.tracker.DBTracker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import java.time.Instant;
import java.util.Locale;

public class EmbedUtil {

    public static EmbedBuilder addNoResultsLog(EmbedBuilder eb, Locale locale, String searchString) {
        return addLog(eb, LogStatus.FAILURE, TextManager.getNoResultsString(locale, searchString));
    }

    public static EmbedBuilder addTrackerRemoveLog(EmbedBuilder eb, Locale locale) {
        return addLog(eb, LogStatus.WARNING, TextManager.getString(locale, TextManager.GENERAL, "tracker_remove"));
    }

    public static EmbedBuilder addTrackerNoteLog(Locale locale, Member member, EmbedBuilder eb, String prefix, String trigger) {
        if (BotPermissionUtil.can(member, Command.getCommandProperties(AlertsCommand.class).userGuildPermissions()) &&
                DBTracker.getInstance().retrieve().getSlots().stream().noneMatch(s -> s.getGuildId() == member.getGuild().getIdLong() && s.getCommandTrigger().equals(trigger))
        ) {
            addLog(eb, LogStatus.WARNING, TextManager.getString(locale, TextManager.GENERAL, "tracker", prefix, trigger));
        }
        return eb;
    }

    public static EmbedBuilder addLog(EmbedBuilder eb, String log) {
        return addLog(eb, null, log);
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
            eb.addField(Emojis.EMPTY_EMOJI, "`" + add + log + "`", false);
        }

        return eb;
    }

    public static EmbedBuilder addRemainingTime(EmbedBuilder eb, Instant instant) {
        if (instant.isAfter(Instant.now()))
            eb.setTimestamp(instant);
        return eb;
    }

    public static EmbedBuilder setFooter(EmbedBuilder eb, Command command) {
        command.getMember().ifPresent(member -> eb.setFooter(member.getUser().getAsTag()));

        return eb;
    }

    public static EmbedBuilder setFooter(EmbedBuilder eb, Command command, String footer) {
        if (footer == null || footer.isEmpty())
            return setFooter(eb, command);

        command.getMember().ifPresent(member -> eb.setFooter(member.getUser().getAsTag() + "｜" + footer));

        return eb;
    }

}
