package commands.runnables.configurationcategory;

import java.util.Locale;
import commands.Command;
import commands.listeners.CommandProperties;
import core.EmbedFactory;
import core.TextManager;
import core.utils.BotPermissionUtil;
import mysql.modules.guild.DBGuild;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;

@CommandProperties(
        trigger = "prefix",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "\uD83D\uDCDB",
        executableWithoutArgs = false
)
public class PrefixCommand extends Command {

    public PrefixCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        Guild guild = event.getGuild();
        if (args.length() > 0) {
            if (args.length() <= 5) {
                DBGuild.getInstance().retrieve(event.getGuild().getIdLong()).setPrefix(args);

                if (BotPermissionUtil.can(guild, Permission.NICKNAME_CHANGE)) {
                    Member self = guild.getSelfMember();
                    String nickname = self.getEffectiveName().trim();
                    String[] nicknameArray = nickname.split("\\[");

                    if (nicknameArray.length == 1) {
                        guild.modifyNickname(self, nickname + " [" + args + "]")
                                .reason(getCommandLanguage().getTitle())
                                .queue();
                    } else if (nicknameArray.length == 2 && nicknameArray[1].contains("]")) {
                        guild.modifyNickname(self, nicknameArray[0].trim() + " [" + args + "]")
                                .reason(getCommandLanguage().getTitle())
                                .queue();
                    }
                }

                event.getChannel().sendMessage(EmbedFactory.getEmbedDefault(this, getString("changed", args)).build()).queue();
                return true;
            } else {
                event.getChannel().sendMessage(EmbedFactory.getEmbedError(
                        this,
                        TextManager.getString(getLocale(), TextManager.GENERAL, "args_too_long", "5")
                ).build()).queue();
                return false;
            }
        } else {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError(
                    this,
                    getString("no_arg")
            ).build()).queue();
            return false;
        }
    }

}
