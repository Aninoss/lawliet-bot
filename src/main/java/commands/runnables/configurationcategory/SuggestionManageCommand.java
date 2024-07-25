package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.ListAbstract;
import constants.LogStatus;
import core.CustomObservableMap;
import core.MemberCacheController;
import core.TextManager;
import core.atomicassets.AtomicUser;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import javafx.util.Pair;
import modules.suggestions.SuggestionMessage;
import modules.suggestions.Suggestions;
import mysql.hibernate.EntityManagerWrapper;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.staticreactionmessages.DBStaticReactionMessages;
import mysql.modules.staticreactionmessages.StaticReactionMessageData;
import mysql.modules.suggestions.DBSuggestions;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@CommandProperties(
        trigger = "suggmanage",
        userGuildPermissions = Permission.MANAGE_SERVER,
        emoji = "❕",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = {"suggestionmanage", "suggestionsmanage"}
)
public class SuggestionManageCommand extends ListAbstract {

    public static final String BUTTON_ID_ACCEPT = "accept";
    public static final String BUTTON_ID_DECLINE = "decline";

    private List<SuggestionMessage> suggestions;

    public SuggestionManageCommand(Locale locale, String prefix) {
        super(locale, prefix, 1);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) throws Throwable {
        registerList(event.getMember(), args);
        return true;
    }

    @Override
    public boolean onButton(@NotNull ButtonInteractionEvent event) throws Throwable {
        SuggestionMessage suggestionMessage = suggestions.get(getPage());

        switch (event.getComponentId()) {
            case BUTTON_ID_ACCEPT -> {
                boolean success = manageSuggestion(suggestionMessage, true);
                refresh(event.getMember());

                if (success) {
                    setLog(LogStatus.SUCCESS, getString("log_accepted"));

                    EntityManagerWrapper entityManager = getEntityManager();
                    entityManager.getTransaction().begin();
                    BotLogEntity.log(entityManager, BotLogEntity.Event.SERVER_SUGGESTIONS_MANAGE_ACCEPT, event.getMember(), suggestionMessage.getMessageId());
                    entityManager.getTransaction().commit();
                }
                return true;
            }
            case BUTTON_ID_DECLINE -> {
                boolean success = manageSuggestion(suggestionMessage, false);
                refresh(event.getMember());

                if (success) {
                    setLog(LogStatus.SUCCESS, getString("log_declined"));

                    EntityManagerWrapper entityManager = getEntityManager();
                    entityManager.getTransaction().begin();
                    BotLogEntity.log(entityManager, BotLogEntity.Event.SERVER_SUGGESTIONS_MANAGE_DECLINE, event.getMember(), suggestionMessage.getMessageId());
                    entityManager.getTransaction().commit();
                }
                return true;
            }
            default -> {
                return super.onButton(event);
            }
        }
    }

    @Override
    protected int configure(Member member, int orderBy) throws Throwable {
        suggestions = DBSuggestions.getInstance().retrieve(member.getGuild().getIdLong()).getSuggestionMessages().values().stream()
                .sorted(Comparator.comparingLong(SuggestionMessage::getMessageId))
                .collect(Collectors.toList());
        return suggestions.size();
    }

    @Override
    protected Pair<String, String> getEntry(Member member, int i, int orderBy) {
        return null;
    }

    @Override
    protected void postProcessEmbed(EmbedBuilder eb, int orderBy) {
        if (suggestions.isEmpty()) {
            return;
        }

        SuggestionMessage suggestionMessage = suggestions.get(getPage());
        StaticReactionMessageData staticReactionMessageData = DBStaticReactionMessages.getInstance().retrieve(suggestionMessage.getGuildId()).get(suggestionMessage.getMessageId());
        if (staticReactionMessageData != null) {
            staticReactionMessageData.getGuildMessageChannel()
                    .ifPresent(suggestionMessage::loadVoteValuesifAbsent);
        }

        String author;
        if (suggestionMessage.getUserId() != null) {
            MemberCacheController.getInstance().loadMember(getGuild().get(), suggestionMessage.getUserId()).join();
            author = new AtomicUser(suggestionMessage.getUserId()).getName(getLocale());
        } else {
            author = suggestionMessage.getAuthor();
        }

        int likes = suggestionMessage.getUpvotes();
        int dislikes = suggestionMessage.getDownvotes();
        eb.setDescription(suggestionMessage.getContent())
                .addField(getString("header_author"), author, true)
                .addField(getString("header_updown"), (likes + dislikes > 0) ? StringUtil.numToString((int) Math.round((double) likes / (likes + dislikes) * 100)) + "%" : "-%", true);
    }

    @Override
    protected List<ActionRow> postProcessAddActionRows() {
        if (suggestions.isEmpty()) {
            return Collections.emptyList();
        }

        return List.of(ActionRow.of(
                Button.of(ButtonStyle.SUCCESS, BUTTON_ID_ACCEPT, getString("button_accept")),
                Button.of(ButtonStyle.DANGER, BUTTON_ID_DECLINE, getString("button_decline"))
        ));
    }

    private boolean manageSuggestion(SuggestionMessage suggestionMessage, boolean accept) {
        CustomObservableMap<Long, SuggestionMessage> suggestionMessages = DBSuggestions.getInstance().retrieve(suggestionMessage.getGuildId()).getSuggestionMessages();
        CustomObservableMap<Long, StaticReactionMessageData> staticReactionMessages = DBStaticReactionMessages.getInstance().retrieve(suggestionMessage.getGuildId());

        StaticReactionMessageData staticReactionMessageData = staticReactionMessages.get(suggestionMessage.getMessageId());
        if (staticReactionMessageData == null) {
            suggestionMessages.remove(suggestionMessage.getMessageId());
            setLog(LogStatus.FAILURE, getString("log_notfound"));
            return false;
        }

        GuildMessageChannel channel = staticReactionMessageData.getGuildMessageChannel().orElse(null);
        if (channel == null) {
            suggestionMessages.remove(suggestionMessage.getMessageId());
            setLog(LogStatus.FAILURE, getString("log_notfound"));
            return false;
        }
        if (!BotPermissionUtil.canWriteEmbed(channel, Permission.MESSAGE_HISTORY, Permission.MESSAGE_MANAGE)) {
            setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "permission_channel_manage", "#" + StringUtil.escapeMarkdownInField(channel.getName())));
            return false;
        }

        suggestionMessages.remove(suggestionMessage.getMessageId());
        staticReactionMessages.remove(suggestionMessage.getMessageId());

        EmbedBuilder eb = Suggestions.generateEmbed(getLocale(), getGuild().get(), suggestionMessage);
        EmbedUtil.addLog(eb, accept ? LogStatus.SUCCESS : LogStatus.FAILURE, getString(accept ? "message_accepted" : "message_declined"));
        channel.editMessageEmbedsById(suggestionMessage.getMessageId(), eb.build())
                .and(channel.clearReactionsById(suggestionMessage.getMessageId()))
                .queue();
        return true;
    }

}
