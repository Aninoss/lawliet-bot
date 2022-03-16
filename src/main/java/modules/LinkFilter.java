package modules;

import constants.AssetIds;
import core.utils.BotPermissionUtil;
import core.utils.InternetUtil;
import core.utils.JDAUtil;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;

public class LinkFilter {

    public static boolean check(Message message) {
        Guild guild = message.getGuild();

        if ((guild.getIdLong() == AssetIds.ANICORD_SERVER_ID || guild.getIdLong() == AssetIds.SUPPORT_SERVER_ID) &&
                !BotPermissionUtil.can(message.getMember(), Permission.MESSAGE_EMBED_LINKS) &&
                InternetUtil.stringHasURL(message.getContentRaw())
        ) {
            message.delete().queue();
            if (guild.getIdLong() == AssetIds.ANICORD_SERVER_ID) {
                String text = "⚠️ Du benötigst den ersten Fischereirang, bevor du Links auf **Anicord** senden kannst!\nMehr Informationen dazu findest du auf <#608455541978824739>";
                JDAUtil.openPrivateChannel(message.getMember())
                        .flatMap(messageChannel -> messageChannel.sendMessage(text))
                        .queue();
                message.getGuild().getTextChannelById(819350890263085097L)
                        .sendMessage("LINK BLOCK FOR " + message.getAuthor().getAsTag() + " IN " + message.getChannel().getAsMention() + ": " + message.getContentRaw())
                        .queue();
            }
            return false;
        }

        return true;
    }

}
