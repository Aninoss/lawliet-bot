package events.discordevents.guildjoin;

import constants.ExceptionIds;
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
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;

import java.util.Locale;

@DiscordEvent
public class GuildJoinPostWelcomeMessage extends GuildJoinAbstract {

    @Override
    public boolean onGuildJoin(GuildJoinEvent event, EntityManagerWrapper entityManager) {
        JDAUtil.getFirstWritableTextChannelOfGuild(event.getGuild())
                .ifPresent(channel -> sendNewMessage(channel, entityManager));
        return true;
    }

    private void sendNewMessage(GuildMessageChannel channel, EntityManagerWrapper entityManager) {
        GuildEntity guildEntity = entityManager.findGuildEntity(channel.getGuild().getIdLong());
        String prefix = guildEntity.getPrefix();
        Locale locale = guildEntity.getLocale();
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
                    .exceptionally(ExceptionLogger.get(ExceptionIds.MISSING_PERMISSIONS));
        }
    }

}
