package events.discordevents.guildusercontextinteraction;

import commands.Category;
import commands.Command;
import commands.runnables.moderationcategory.WarnCommand;
import constants.Language;
import core.EmbedFactory;
import core.TextManager;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.eventtypeabstracts.GuildUserContextInteractionAbstract;
import modules.moduserinteractions.ModUserInteractionManager;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;

import java.util.Arrays;
import java.util.Locale;

@DiscordEvent
public class GuildUserContextInteractionModUserAction extends GuildUserContextInteractionAbstract {

    @Override
    public boolean onGuildUserContextInteraction(UserContextInteractionEvent event, EntityManagerWrapper entityManager) {
        if (Arrays.stream(Language.values())
                .noneMatch(language -> TextManager.getString(language.getLocale(), Category.MODERATION, "user_interaction").equals(event.getCommandString()))
        ) {
            return true;
        }

        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(TextManager.getString(guildEntity.getLocale(), Category.MODERATION, "user_interaction_title"))
                .setDescription(TextManager.getString(guildEntity.getLocale(), Category.MODERATION, "user_interaction_desc", StringUtil.escapeMarkdown(event.getTarget().getAsTag())));

        event.replyEmbeds(eb.build())
                .addActionRow(generateSelectMenu(guildEntity.getLocale(), event.getTarget().getIdLong()))
                .setEphemeral(true)
                .queue();

        return true;
    }

    private StringSelectMenu generateSelectMenu(Locale locale, long targetUserId) {
        SelectOption[] selectOptions = new SelectOption[ModUserInteractionManager.MOD_CLASSES.size()];
        for (int i = 0; i < ModUserInteractionManager.MOD_CLASSES.size(); i++) {
            Class<? extends WarnCommand> clazz = ModUserInteractionManager.MOD_CLASSES.get(i);
            String title = Command.getCommandLanguage(clazz, locale).getTitle();
            String trigger = Command.getCommandProperties(clazz).trigger();
            selectOptions[i] = SelectOption.of(title, trigger);
        }
        return StringSelectMenu.create(ModUserInteractionManager.SELECT_MENU_ID + targetUserId)
                .addOptions(selectOptions)
                .build();
    }

}
