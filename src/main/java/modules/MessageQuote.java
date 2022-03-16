package modules;

import java.util.Locale;
import commands.Category;
import commands.Command;
import commands.runnables.gimmickscategory.QuoteCommand;
import core.EmbedFactory;
import core.TextManager;
import core.components.ActionRows;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.BaseGuildMessageChannel;
import net.dv8tion.jda.api.entities.GuildMessageChannel;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;

public class MessageQuote {

    public static Message postQuote(String prefix, Locale locale, GuildMessageChannel channel, Message searchedMessage,
                                    boolean showAutoQuoteTurnOff) {
        boolean channelIsNSFW = false;
        if (channel instanceof BaseGuildMessageChannel) {
            channelIsNSFW = ((BaseGuildMessageChannel) channel).isNSFW();
        }

        if (((BaseGuildMessageChannel) searchedMessage.getChannel()).isNSFW() && !channelIsNSFW) {
            return new MessageBuilder()
                    .setEmbeds(EmbedFactory.getNSFWBlockEmbed(locale).build())
                    .setActionRows(ActionRows.of(EmbedFactory.getNSFWBlockButton(locale)))
                    .build();
        }

        EmbedBuilder eb;
        String footerAdd = showAutoQuoteTurnOff ? " | " + TextManager.getString(locale, Category.GIMMICKS, "quote_turningoff", prefix) : "";

        if (searchedMessage.getEmbeds().size() == 0) {
            eb = EmbedFactory.getEmbedDefault()
                    .setFooter(Command.getCommandLanguage(QuoteCommand.class, locale).getTitle() + footerAdd);
            if (searchedMessage.getContentRaw().length() > 0) {
                eb.setDescription("\"" + searchedMessage.getContentRaw() + "\"");
            }
            if (searchedMessage.getAttachments().size() > 0) {
                eb.setImage(searchedMessage.getAttachments().get(0).getUrl());
            }
        } else {
            MessageEmbed embed = searchedMessage.getEmbeds().get(0);
            eb = new EmbedBuilder(embed);

            if (embed.getImage() != null) {
                eb.setImage(embed.getImage().getUrl());
            } else if (searchedMessage.getAttachments().size() > 0) {
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
                                StringUtil.escapeMarkdownInField(searchedMessage.getAuthor().getAsTag()), "#" + searchedMessage.getChannel().getName()
                        ),
                        null,
                        searchedMessage.getAuthor().getEffectiveAvatarUrl()
                );

        return new MessageBuilder()
                .setEmbeds(eb.build())
                .build();
    }

}
