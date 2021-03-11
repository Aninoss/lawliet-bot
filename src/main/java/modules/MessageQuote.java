package modules;

import java.util.Locale;
import commands.Command;
import commands.runnables.gimmickscategory.QuoteCommand;
import constants.Category;
import core.EmbedFactory;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;

public class MessageQuote {

    public static void postQuote(Locale locale, TextChannel channel, Message searchedMessage, boolean showAutoQuoteTurnOff) {
        if (BotPermissionUtil.canWriteEmbed(channel)) {
            return;
        }

        if (searchedMessage.getTextChannel().isNSFW() && !channel.isNSFW()) {
            channel.sendMessage(EmbedFactory.getNSFWBlockEmbed(locale).build()).queue();
            return;
        }

        EmbedBuilder eb;
        String footerAdd = showAutoQuoteTurnOff ? " | " + TextManager.getString(locale, Category.GIMMICKS, "quote_turningoff") : "";

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
                                StringUtil.escapeMarkdownInField(searchedMessage.getMember().getEffectiveName()), "#" + searchedMessage.getChannel().getName()
                        ),
                        "",
                        searchedMessage.getAuthor().getEffectiveAvatarUrl()
                );

        channel.sendMessage(eb.build()).queue();
    }

}
