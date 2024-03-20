package modules;

import commands.Category;
import commands.Command;
import commands.runnables.gimmickscategory.QuoteCommand;
import core.EmbedFactory;
import core.TextManager;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;

import java.util.Locale;

public class MessageQuote {

    public static MessageCreateData postQuote(String prefix, Locale locale, GuildMessageChannel channel, Message searchedMessage,
                                              boolean showAutoQuoteTurnOff) {
        if (JDAUtil.channelIsNsfw(searchedMessage.getChannel())  &&
                !JDAUtil.channelIsNsfw(channel)
        ) {
            return new MessageCreateBuilder()
                    .setEmbeds(EmbedFactory.getNSFWBlockEmbed(locale, prefix).build())
                    .build();
        }

        EmbedBuilder eb;
        String footerAdd = showAutoQuoteTurnOff ? " | " + TextManager.getString(locale, Category.GIMMICKS, "quote_turningoff", prefix) : "";

        if (searchedMessage.getEmbeds().isEmpty()) {
            eb = EmbedFactory.getEmbedDefault()
                    .setFooter(Command.getCommandLanguage(QuoteCommand.class, locale).getTitle() + footerAdd);
            if (!searchedMessage.getContentRaw().isEmpty()) {
                eb.setDescription("\"" + searchedMessage.getContentRaw() + "\"");
            }
            if (!searchedMessage.getAttachments().isEmpty()) {
                eb.setImage(searchedMessage.getAttachments().get(0).getUrl());
            }
        } else {
            MessageEmbed embed = searchedMessage.getEmbeds().get(0);
            eb = new EmbedBuilder(embed);

            if (embed.getImage() != null) {
                eb.setImage(embed.getImage().getUrl());
            } else if (!searchedMessage.getAttachments().isEmpty()) {
                eb.setImage(searchedMessage.getAttachments().get(0).getUrl());
            }

            if (embed.getFooter() != null) {
                eb.setFooter(embed.getFooter().getText() + " - " + Command.getCommandLanguage(QuoteCommand.class, locale).getTitle() + footerAdd);
            } else {
                eb.setFooter(Command.getCommandLanguage(QuoteCommand.class, locale).getTitle() + footerAdd);
            }
        }

        eb.setTimestamp(searchedMessage.getTimeCreated())
                .setAuthor(
                        TextManager.getString(
                                locale,
                                Category.GIMMICKS,
                                "quote_sendby",
                                StringUtil.escapeMarkdownInField(searchedMessage.getAuthor().getName()), "#" + searchedMessage.getChannel().getName()
                        ),
                        null,
                        searchedMessage.getAuthor().getEffectiveAvatarUrl()
                );

        return new MessageCreateBuilder()
                .setEmbeds(eb.build())
                .build();
    }

}
