package modules;

import commands.Command;
import commands.runnables.configurationcategory.PrefixCommand;
import core.utils.BotPermissionUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.BotLogEntity;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;

import java.util.Locale;

public class Prefix {

    public static void changePrefix(Member member, Locale locale, String prefix, GuildEntity guildEntity) {
        guildEntity.beginTransaction();
        BotLogEntity.log(guildEntity.entityManager, BotLogEntity.Event.PREFIX, member, guildEntity.getPrefix(), prefix);
        guildEntity.setPrefix(prefix);
        guildEntity.commitTransaction();

        Guild guild = member.getGuild();
        Member self = guild.getSelfMember();
        if (BotPermissionUtil.can(guild, Permission.NICKNAME_CHANGE)) {
            String nickname = self.getEffectiveName().trim();
            String[] nicknameArray = nickname.split("\\[");

            String effectiveNickname;
            if (nicknameArray.length == 2 && nicknameArray[1].contains("]")) {
                effectiveNickname = nicknameArray[0].trim() + " [" + prefix + "]";
            } else {
                effectiveNickname = nickname + " [" + prefix + "]";
            }

            guild.modifyNickname(self, StringUtil.shortenString(effectiveNickname, 32))
                    .reason(Command.getCommandLanguage(PrefixCommand.class, locale).getTitle())
                    .queue();
        }
    }

}
