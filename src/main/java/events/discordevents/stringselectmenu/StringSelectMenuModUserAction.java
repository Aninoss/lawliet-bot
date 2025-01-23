package events.discordevents.stringselectmenu;

import commands.Category;
import commands.CommandManager;
import commands.runnables.moderationcategory.WarnCommand;
import commands.runnables.moderationcategory.WarnRemoveCommand;
import core.EmbedFactory;
import core.MemberCacheController;
import core.ShardManager;
import core.TextManager;
import core.modals.ModalMediator;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import events.discordevents.DiscordEvent;
import events.discordevents.InteractionListenerHandler;
import events.discordevents.eventtypeabstracts.StringSelectMenuAbstract;
import modules.ModUserInteractionManager;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.guild.GuildEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.callbacks.IReplyCallback;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import net.dv8tion.jda.api.interactions.modals.ModalMapping;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Locale;

@DiscordEvent
public class StringSelectMenuModUserAction extends StringSelectMenuAbstract implements InteractionListenerHandler<StringSelectInteractionEvent> {

    @Override
    public boolean onStringSelectMenu(StringSelectInteractionEvent event, EntityManagerWrapper entityManager) throws Throwable {
        if (!event.getComponentId().startsWith(ModUserInteractionManager.SELECT_MENU_ID) || event.getGuild() == null) {
            return true;
        }

        GuildEntity guildEntity = entityManager.findGuildEntity(event.getGuild().getIdLong());
        String trigger = event.getSelectedOptions().get(0).getValue();
        long targetUserId = Long.parseLong(event.getComponentId().substring(ModUserInteractionManager.SELECT_MENU_ID.length()));
        WarnCommand modCommand = (WarnCommand) CommandManager.createCommandByTrigger(trigger, guildEntity.getLocale(), guildEntity.getPrefix()).get();
        modCommand.setGuildEntity(guildEntity);

        EmbedBuilder errorEmbed = ModUserInteractionManager.checkAccess(guildEntity, event.getMember(), event.getGuildChannel(), modCommand, targetUserId);
        if (errorEmbed != null) {
            if (event.isAcknowledged()) {
                event.getHook().sendMessageEmbeds(errorEmbed.build())
                        .setEphemeral(true)
                        .queue();
            } else {
                event.replyEmbeds(errorEmbed.build())
                        .setEphemeral(true)
                        .queue();
            }
            return false;
        }

        if (!modCommand.getIncludeNotInGuild()) {
            Member member = MemberCacheController.getInstance().loadMember(event.getGuild(), targetUserId).get();
            if (member == null) {
                String error = TextManager.getString(guildEntity.getLocale(), Category.MODERATION, "user_interaction_error_nomember");
                replyError(event, guildEntity.getLocale(), error);
                return false;
            }
        }

        TextInput reasonTextInput = TextInput.create(ModUserInteractionManager.REASON_ID, TextManager.getString(guildEntity.getLocale(), Category.MODERATION, "warn_reason").replace(":", ""), TextInputStyle.PARAGRAPH)
                .setRequired(false)
                .setMinLength(0)
                .setMaxLength(WarnCommand.REASON_MAX)
                .build();

        ArrayList<ActionRow> actionRowList = new ArrayList<>();
        actionRowList.add(ActionRow.of(reasonTextInput));

        if (modCommand.hasDuration()) {
            TextInput durationTextInput = TextInput.create(ModUserInteractionManager.DURATION_ID, TextManager.getString(guildEntity.getLocale(), Category.MODERATION, "moderation_duration"), TextInputStyle.SHORT)
                    .setRequired(false)
                    .setMinLength(0)
                    .setMaxLength(20)
                    .build();
            actionRowList.add(ActionRow.of(durationTextInput));
        }

        if (modCommand instanceof WarnRemoveCommand) {
            TextInput amountTextInput = TextInput.create(ModUserInteractionManager.AMOUNT_ID, TextManager.getString(guildEntity.getLocale(), Category.MODERATION, "warnremove_amount"), TextInputStyle.SHORT)
                    .setValue("1")
                    .setMinLength(1)
                    .setMaxLength(3)
                    .build();
            actionRowList.add(ActionRow.of(amountTextInput));
        }

        if (event.isAcknowledged()) {
            replyError(event, guildEntity.getLocale(), TextManager.getString(guildEntity.getLocale(), Category.MODERATION, "user_interaction_error_timeout"));
        } else {
            Modal modal = generateModal(guildEntity.getLocale(), modCommand, event.getMember().getIdLong(), targetUserId, actionRowList);
            event.replyModal(modal).queue();
        }
        return false;
    }

    @NotNull
    private static Modal generateModal(Locale locale, WarnCommand modCommand, long memberId, long targetUserId, ArrayList<ActionRow> actionRowList) {
        return ModalMediator.createModal(memberId, TextManager.getString(locale, Category.MODERATION, "user_interaction"), (event, guildEntity) -> {
                    try {
                        EmbedBuilder modalErrorEmbed = ModUserInteractionManager.checkAccess(guildEntity, event.getMember(), event.getGuildChannel(), modCommand, targetUserId);
                        if (modalErrorEmbed != null) {
                            if (event.isAcknowledged()) {
                                event.getHook().sendMessageEmbeds(modalErrorEmbed.build())
                                        .setEphemeral(true)
                                        .queue();
                            } else {
                                event.replyEmbeds(modalErrorEmbed.build())
                                        .setEphemeral(true)
                                        .queue();
                            }
                            return;
                        }

                        String reason = "";
                        long durationMinutes = 0;
                        int amount = 0;

                        for (ModalMapping value : event.getValues()) {
                            switch (value.getId()) {
                                case ModUserInteractionManager.REASON_ID -> reason = value.getAsString();
                                case ModUserInteractionManager.DURATION_ID -> {
                                    String v = value.getAsString();
                                    durationMinutes = MentionUtil.getTimeMinutes(v).getValue();
                                    if (!v.isBlank() && durationMinutes == 0) {
                                        String error = TextManager.getString(locale, Category.MODERATION, "user_interaction_error_invalidduration", StringUtil.escapeMarkdown(v));
                                        replyError(event, guildEntity.getLocale(), error);
                                        return;
                                    }
                                }
                                case ModUserInteractionManager.AMOUNT_ID -> {
                                    String v = value.getAsString();
                                    if (v.equalsIgnoreCase("all")) {
                                        amount = Integer.MAX_VALUE;
                                    } else {
                                        if (!StringUtil.stringIsInt(v)) {
                                            String error = TextManager.getString(locale, Category.MODERATION, "user_interaction_error_invalidamount", StringUtil.escapeMarkdown(v));
                                            replyError(event, guildEntity.getLocale(), error);
                                            return;
                                        }
                                        amount = Integer.parseInt(v);
                                    }
                                }
                            }
                        }

                        if (!event.isAcknowledged()) {
                            event.deferEdit().queue();
                        }
                        User targetUser = ShardManager.fetchUserById(targetUserId).get();

                        modCommand.setGuildEntity(guildEntity);
                        modCommand.userActionPrepareExecution(targetUser, reason, durationMinutes, amount);
                        modCommand.checkAndExecute(event.getGuildChannel(), event.getMember());

                        event.getHook().editOriginalEmbeds(modCommand.draw(event.getMember()).build())
                                .setComponents()
                                .queue();
                    } catch (Throwable throwable) {
                        throw new RuntimeException(throwable);
                    }
                })
                .addComponents(actionRowList)
                .build();
    }

    private static void replyError(IReplyCallback event, Locale locale, String description) {
        EmbedBuilder eb = EmbedFactory.getEmbedError()
                .setTitle(TextManager.getString(locale, TextManager.GENERAL, "wrong_args"))
                .setDescription(description);

        if (event.isAcknowledged()) {
            event.getHook().sendMessageEmbeds(eb.build())
                    .setEphemeral(true)
                    .queue();
        } else {
            event.replyEmbeds(eb.build())
                    .setEphemeral(true)
                    .queue();
        }
    }

}
