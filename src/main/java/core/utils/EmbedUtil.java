package core.utils;

import commands.Command;
import commands.runnables.configurationcategory.AlertsCommand;
import constants.Emojis;
import constants.LogStatus;
import core.TextManager;
import mysql.modules.tracker.DBTracker;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;

import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

public class EmbedUtil {

    public static EmbedBuilder setMemberAuthor(EmbedBuilder eb, Member member) {
        return setMemberAuthor(eb, member.getEffectiveName(), member.getEffectiveAvatarUrl());
    }

    public static EmbedBuilder setMemberAuthor(EmbedBuilder eb, String memberName, String memberAvatarUrl) {
        return eb.setAuthor(memberName, null, memberAvatarUrl);
    }

    public static EmbedBuilder addNoResultsLog(EmbedBuilder eb, Locale locale, String searchString) {
        return addLog(eb, LogStatus.FAILURE, TextManager.getNoResultsString(locale, searchString));
    }

    public static EmbedBuilder addTrackerRemoveLog(EmbedBuilder eb, Locale locale) {
        return addLog(eb, LogStatus.WARNING, TextManager.getString(locale, TextManager.GENERAL, "tracker_remove"));
    }

    public static EmbedBuilder addTrackerNoteLog(Locale locale, Member member, EmbedBuilder eb, String prefix, String trigger) {
        if (BotPermissionUtil.can(member, Command.getCommandProperties(AlertsCommand.class).userGuildPermissions()) &&
                DBTracker.getInstance().retrieve(member.getGuild().getIdLong()).values().stream().noneMatch(s -> s.getCommandTrigger().equals(trigger))
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
                add = switch (logStatus) {
                    case FAILURE -> "❌ ";
                    case SUCCESS -> "✅ ";
                    case WIN -> "🎉 ";
                    case LOSE -> "☠️ ";
                    case WARNING -> "⚠️️ ";
                    case TIME -> "⏲️ ";
                };
            }
            eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), "`" + StringUtil.shortenString(add + log, 500) + "`", false);
        }

        return eb;
    }

    public static EmbedBuilder setFooter(EmbedBuilder eb, Command command) {
        Optional<String> userTagOpt = command.getUsername();
        if (userTagOpt.isPresent()) {
            eb.setFooter(userTagOpt.get());
        } else {
            eb.setFooter(null);
        }

        return eb;
    }

    public static EmbedBuilder setFooter(EmbedBuilder eb, Command command, String footer) {
        if (footer == null || footer.isEmpty()) {
            return setFooter(eb, command);
        }

        Optional<String> userTagOpt = command.getUsername();
        if (userTagOpt.isPresent()) {
            eb = eb.setFooter(userTagOpt.get() + "｜" + footer);
        } else {
            eb.setFooter(footer);
        }
        return eb;
    }

    public static void addFieldSplit(EmbedBuilder eb, String name, String value, boolean inline) {
        addFieldSplit(eb, name, value, inline, "\n");
    }

    public static void addFieldSplit(EmbedBuilder eb, String name, String value, boolean inline, String splitCharacter) {
        ArrayList<StringBuilder> values = new ArrayList<>();
        String[] splits = value.split(splitCharacter);
        int splitIndex = -1;
        for (String split : splits) {
            if (splitIndex >= 0 && values.get(splitIndex).length() + split.length() + splitCharacter.length() <= MessageEmbed.VALUE_MAX_LENGTH) {
                values.get(splitIndex).append(split).append(splitCharacter);
                continue;
            }
            StringBuilder sb = new StringBuilder(split).append(splitCharacter);
            values.add(sb);
            splitIndex += 1;
        }

        for (int i = 0; i < values.size(); i++) {
            String cutName;
            if (name != null) {
                cutName = values.size() > 1
                        ? name + " (" + (i + 1) + "/" + values.size() + ")"
                        : name;
            } else {
                cutName = Emojis.ZERO_WIDTH_SPACE.getFormatted();
            }
            String cutValue = values.get(i).toString();
            eb.addField(cutName, cutValue, inline);
        }
    }

}
