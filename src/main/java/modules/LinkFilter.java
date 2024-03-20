package modules;

import constants.AssetIds;
import core.utils.BotPermissionUtil;
import core.utils.InternetUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;

public class LinkFilter {

    public static boolean check(Message message) {
        Guild guild = message.getGuild();

        if ((guild.getIdLong() == AssetIds.ANICORD_SERVER_ID || guild.getIdLong() == AssetIds.SUPPORT_SERVER_ID) &&
                !BotPermissionUtil.can(message.getMember(), Permission.MESSAGE_EMBED_LINKS) &&
                InternetUtil.stringHasURL(message.getContentRaw())
        ) {
            message.delete().queue();
            if (guild.getIdLong() == AssetIds.ANICORD_SERVER_ID) {
                String text = "⚠️ Du musst verifiziert sein, bevor du Links auf **Anicord** senden kannst!\nMehr Informationen dazu findest du auf <#1004011415499190292>";
                JDAUtil.openPrivateChannel(message.getMember())
                        .flatMap(messageChannel -> messageChannel.sendMessage(text))
                        .queue();
                message.getGuild().getChannelById(GuildMessageChannel.class, 819350890263085097L)
                        .sendMessage("LINK BLOCK FOR " + StringUtil.escapeMarkdown(message.getAuthor().getName()) + " IN " + message.getChannel().getAsMention() + ": " + message.getContentRaw())
                        .queue();
            }
            return false;
        }

        return true;
    }

}
