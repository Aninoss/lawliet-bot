package modules;

import constants.Category;
import core.EmbedFactory;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import java.util.Locale;

public class MessageQuote {

    public static void postQuote(Locale locale, TextChannel channel, Message searchedMessage, boolean showAutoQuoteTurnOff) {
        if (BotPermissionUtil.canWriteEmbed(channel))
            return;

        if (searchedMessage.getTextChannel().isNSFW() && !channel.isNSFW()) {
            channel.sendMessage(EmbedFactory.getNSFWBlockEmbed(locale).build()).queue();
            return;
        }

        EmbedBuilder eb;
        String footerAdd = showAutoQuoteTurnOff ? " | " + TextManager.getString(locale, Category.GIMMICKS, "quote_turningoff") : "";

        if (searchedMessage.getEmbeds().size() == 0) {
            eb = EmbedFactory.getEmbedDefault()
                    .setFooter(TextManager.getString(locale, Category.GIMMICKS, "quote_title") + footerAdd);
            if (searchedMessage.getContentRaw().length() > 0) eb.setDescription("\"" + searchedMessage.getContentRaw() + "\"");
            if (searchedMessage.getAttachments().size() > 0) eb.setImage(searchedMessage.getAttachments().get(0).getUrl());
        } else {
            MessageEmbed embed = searchedMessage.getEmbeds().get(0);
            eb = new EmbedBuilder(embed);

            if (embed.getImage() != null)
                eb.setImage(embed.getImage().getUrl());
            else if (searchedMessage.getAttachments().size() > 0)
                eb.setImage(searchedMessage.getAttachments().get(0).getUrl());

            if (embed.getFooter() != null)
                eb.setFooter(embed.getFooter().getText() + " - " + TextManager.getString(locale, Category.GIMMICKS, "quote_title") + footerAdd);
            else
                eb.setFooter(TextManager.getString(locale, Category.GIMMICKS, "quote_title") + footerAdd);
        }

        eb.setTimestamp(searchedMessage.getTimeCreated())
                .setAuthor(
                        TextManager.getString(
                                locale,
                                Category.GIMMICKS,
                                "quote_sendby",
                                StringUtil.escapeMarkdownInField(searchedMessage.getMember().getEffectiveName()), "#" + searchedMessage.getChannel().getName()
                        ),
                        "",
                        searchedMessage.getAuthor().getEffectiveAvatarUrl());

        channel.sendMessage(eb.build()).queue();
    }

}
