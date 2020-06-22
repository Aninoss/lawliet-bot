package Commands.GimmicksCategory;

import CommandListeners.CommandProperties;

import CommandSupporters.Command;
import Constants.Permission;
import Core.*;
import Core.Mention.MentionUtil;
import Core.Mention.MentionList;
import Core.Utils.StringUtil;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.message.embed.EmbedField;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "quote",
        botPermissions = Permission.READ_MESSAGE_HISTORY,
        userPermissions = Permission.READ_MESSAGE_HISTORY,
        emoji = "\uD83D\uDCDD",
        executable = false,
        aliases = {"qoute"}
)
public class QuoteCommand extends Command {

    @Override
    public boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        User user = event.getMessage().getUserAuthor().get();

        //Message Link
        ArrayList<Message> directMessage = MentionUtil.getMessagesURL(event.getMessage(), followedString).getList();
        if (directMessage.size() > 0) {
            for(Message message : directMessage) {
                if (message.getChannel().canSee(user) && message.getChannel().canReadMessageHistory(user)) {
                    postEmbed(event.getMessage().getServerTextChannel().get(), message);
                    return true;
                }
            }
        }

        if (followedString.length() > 0) {
            MentionList<ServerTextChannel> channelMention = MentionUtil.getTextChannels(event.getMessage(), followedString);
            String newString = channelMention.getResultMessageString();
            ServerTextChannel channel = channelMention.getList().isEmpty() ? event.getServerTextChannel().get() : channelMention.getList().get(0);

            //ID with channel
            if (StringUtil.stringIsLong(newString)) {
                Message message = DiscordApiCollection.getInstance().getMessageById(channel, Long.parseLong(newString)).orElse(null);
                if (message != null) {
                    postEmbed(event.getMessage().getServerTextChannel().get(), message);
                    return true;
                }
            }

            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(),TextManager.GENERAL,"no_results"))
                    .setDescription(getString("noresult_channel", newString, channel.getMentionTag()));
            event.getChannel().sendMessage(eb).get();
            return false;
        } else {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this, getString("noarg", event.getMessage().getUserAuthor().get().getMentionTag()));
            event.getChannel().sendMessage(eb).get();
            return false;
        }
    }

    public void postEmbed(ServerTextChannel channel, Message searchedMessage) throws IOException, ExecutionException, InterruptedException {
        postEmbed(channel, searchedMessage, false);
    }

    public void postEmbed(ServerTextChannel channel, Message searchedMessage, boolean showAutoQuoteTurnOff) throws IOException, ExecutionException, InterruptedException {
        if (!channel.canYouWrite() || !channel.canYouEmbedLinks()) return;

        if (searchedMessage.getServerTextChannel().get().isNsfw() && !channel.isNsfw()) {
            channel.sendMessage(EmbedFactory.getNSFWBlockEmbed(getLocale())).get();
            return;
        }

        EmbedBuilder eb;
        String footerAdd = showAutoQuoteTurnOff ? " | " + getString("turningoff") : "";

        if (searchedMessage.getEmbeds().size() == 0) {
            eb = EmbedFactory.getEmbed()
                    .setFooter(getString("title") + footerAdd);
            if (searchedMessage.getContent().length() > 0) eb.setDescription("\""+searchedMessage.getContent()+"\"");
            if (searchedMessage.getAttachments().size() > 0) eb.setImage(searchedMessage.getAttachments().get(0).getUrl().toString());
        }

        else {
            Embed embed = searchedMessage.getEmbeds().get(0);
            eb = new EmbedBuilder();
            if (embed.getTitle().isPresent()) eb.setTitle(embed.getTitle().get());
            if (embed.getDescription().isPresent()) eb.setDescription(embed.getDescription().get());
            if (embed.getColor().isPresent()) eb.setColor(embed.getColor().get());
            if (embed.getThumbnail().isPresent()) eb.setThumbnail(embed.getThumbnail().get().getUrl().toString());
            if (embed.getImage().isPresent()) eb.setImage(embed.getImage().get().getUrl().toString());
            else if (searchedMessage.getAttachments().size() > 0) eb.setImage(searchedMessage.getAttachments().get(0).getUrl().toString());
            if (embed.getUrl().isPresent()) eb.setUrl(embed.getUrl().get().toString());
            if (embed.getFooter().isPresent()) eb.setFooter(embed.getFooter().get().getText().get() + " - " + getString("title") + footerAdd);
            else eb.setFooter(getString("title") + footerAdd);
            if (embed.getFields().size() > 0) {
                for(EmbedField ef: embed.getFields()) {
                    eb.addField(ef.getName(),ef.getValue(),ef.isInline());
                }
            }
        }

        eb
                .setTimestamp(searchedMessage.getCreationTimestamp())
                .setAuthor(getString("sendby", searchedMessage.getAuthor().getDisplayName(), "#"+searchedMessage.getServerTextChannel().get().getName()), "", searchedMessage.getAuthor().getAvatar());

        channel.sendMessage(eb).get();
    }
}
