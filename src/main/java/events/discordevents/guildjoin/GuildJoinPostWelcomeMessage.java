package events.discordevents.guildjoin;

import java.util.Locale;
import constants.ExternalLinks;
import core.EmbedFactory;
import core.ExceptionLogger;
import core.ShardManager;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.JDAUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildJoinAbstract;
import mysql.hibernate.EntityManagerWrapper;
import mysql.modules.guild.DBGuild;
import mysql.modules.guild.GuildData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

@DiscordEvent
public class GuildJoinPostWelcomeMessage extends GuildJoinAbstract {

    @Override
    public boolean onGuildJoin(GuildJoinEvent event, EntityManagerWrapper entityManager) {
        JDAUtil.getFirstWritableChannelOfGuild(event.getGuild())
                .ifPresent(this::sendNewMessage);
        return true;
    }

    private void sendNewMessage(TextChannel channel) {
        GuildData guildData = DBGuild.getInstance().retrieve(channel.getGuild().getIdLong());
        String prefix = guildData.getPrefix();
        Locale locale = guildData.getLocale();
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(TextManager.getString(locale, TextManager.GENERAL, "bot_invite_text").replace("{PREFIX}", prefix))
                .setThumbnail(ShardManager.getSelf().getEffectiveAvatarUrl());

        Button dashboardButton = Button.of(ButtonStyle.LINK, ExternalLinks.DASHBOARD_WEBSITE, TextManager.getString(locale, TextManager.GENERAL, "bot_invite_dashboard"));
        Button commandsButton = Button.of(ButtonStyle.LINK, ExternalLinks.COMMANDS_WEBSITE, TextManager.getString(locale, TextManager.GENERAL, "bot_invite_allcommands"));
        Button faqButton = Button.of(ButtonStyle.LINK, ExternalLinks.FAQ_WEBSITE, TextManager.getString(locale, TextManager.GENERAL, "bot_invite_faq"));
        Button supportServerButton = Button.of(ButtonStyle.LINK, ExternalLinks.SERVER_INVITE_URL, TextManager.getString(locale, TextManager.GENERAL, "bot_invite_supportserver"));

        EmbedUtil.setMemberAuthor(eb, channel.getGuild().getSelfMember());
        if (BotPermissionUtil.canWriteEmbed(channel)) {
            channel.sendMessageEmbeds(eb.build())
                    .addComponents(ActionRow.of(dashboardButton, commandsButton, faqButton, supportServerButton))
                    .submit()
                    .exceptionally(ExceptionLogger.get("50013"));
        }
    }

}
