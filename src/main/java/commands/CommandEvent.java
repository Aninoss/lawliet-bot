package commands;

import java.util.Collection;
import java.util.List;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import core.utils.JDAUtil;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.channel.text.GenericTextChannelEvent;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.requests.RestAction;
import org.jetbrains.annotations.NotNull;

public class CommandEvent extends GenericTextChannelEvent {

    private final Member member;
    private final SlashCommandEvent slashCommandEvent;
    private final GuildMessageReceivedEvent guildMessageReceivedEvent;

    public CommandEvent(@NotNull SlashCommandEvent event) {
        super(event.getJDA(), event.getResponseNumber(), event.getTextChannel());
        this.slashCommandEvent = event;
        this.guildMessageReceivedEvent = null;
        this.member = event.getMember();
    }

    public CommandEvent(@NotNull GuildMessageReceivedEvent event) {
        super(event.getJDA(), event.getResponseNumber(), event.getChannel());
        this.slashCommandEvent = null;
        this.guildMessageReceivedEvent = event;
        this.member = event.getMember();
    }

    @Nullable
    public SlashCommandEvent getSlashCommandEvent() {
        return slashCommandEvent;
    }

    @Nullable
    public GuildMessageReceivedEvent getGuildMessageReceivedEvent() {
        return guildMessageReceivedEvent;
    }

    public boolean isSlashCommandEvent() {
        return slashCommandEvent != null;
    }

    public boolean isGuildMessageReceivedEvent() {
        return guildMessageReceivedEvent != null;
    }

    public RestAction<Message> replyMessage(String content, Collection<ActionRow> actionRows) {
        if (isGuildMessageReceivedEvent()) {
            return JDAUtil.replyMessage(guildMessageReceivedEvent.getMessage(), content)
                    .setActionRows(actionRows);
        } else {
            return slashCommandEvent.getHook().sendMessage(content)
                    .addActionRows(actionRows);
        }
    }

    public RestAction<Message> replyMessageEmbeds(List<MessageEmbed> embeds, Collection<ActionRow> actionRows) {
        if (isGuildMessageReceivedEvent()) {
            return JDAUtil.replyMessageEmbeds(guildMessageReceivedEvent.getMessage(), embeds)
                    .setActionRows(actionRows);
        } else {
            return slashCommandEvent.getHook().sendMessageEmbeds(embeds)
                    .addActionRows(actionRows);
        }
    }

    @Nonnull
    public User getUser() {
        return getMember().getUser();
    }

    @Nonnull
    public Member getMember() {
        return member;
    }

}
