package Commands.General;

import CommandListeners.CommandProperties;
import CommandListeners.onRecievedListener;
import CommandSupporters.Command;
import Constants.Permission;
import General.*;
import General.Mention.MentionFinder;
import General.Mention.MentionList;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.Embed;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.message.embed.EmbedField;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "quote",
        botPermissions = Permission.READ_MESSAGE_HISTORY_OF_TEXT_CHANNEL,
        userPermissions = Permission.READ_MESSAGE_HISTORY_OF_TEXT_CHANNEL,
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/colorful-long-shadow/128/Book-icon.png",
        emoji = "\uD83D\uDCDD",
        executable = false,
        aliases = {"qoute"}
)
public class QuoteCommand extends Command implements onRecievedListener {

    public QuoteCommand() {
        super();
    }

    @Override
    public boolean onRecieved(MessageCreateEvent event, String followedString) throws Throwable {
        return calculateResults(event.getMessage(),followedString);
    }

    private boolean calculateResults(Message message, String followString) throws IOException, ExecutionException, InterruptedException {
        Server server = message.getServer().get();
        MentionList<ServerTextChannel> list = MentionFinder.getTextChannels(message, followString);
        User author = message.getUserAuthor().get();
        if (list.getResultMessageString().replace(" ","").length() > 0) {
            addLoadingReaction();
            followString = list.getResultMessageString();
            ArrayList<ServerTextChannel> channelArrayList = list.getList();
            if (channelArrayList.size() == 0) channelArrayList = new ArrayList<>(server.getTextChannels());
            ServerTextChannel ownChannel = message.getServerTextChannel().get();

            String add = null;
            if (channelArrayList.size() == 1) add = channelArrayList.get(0).getMentionTag();

            //Sucht nach dem Link der Nachricht
            ArrayList<Message> directMessage = MentionFinder.getMessagesURL(message, followString).getList();
            if (directMessage.size() > 0) {
                for(Message message1: directMessage) {
                    if (message1.getChannel().canSee(author) && message1.getChannel().canReadMessageHistory(author)) {
                        postEmbed(message.getServerTextChannel().get(),message1);
                        return true;
                    }
                }
            }

            //Sucht nach der Message-ID im dortigen Channel
            if (channelArrayList.contains(ownChannel)) {
                ArrayList<Message> messageArrayList = MentionFinder.getMessagesId(message, followString, message.getServerTextChannel().get()).getList();
                if (messageArrayList.size() > 0) {
                    for(Message message1: messageArrayList) {
                        if (message1.getChannel().canSee(author) && message1.getChannel().canReadMessageHistory(author)) {
                            postEmbed(message.getServerTextChannel().get(),message1);
                            return true;
                        }
                    }
                }
            }

            //Sucht nach der Message-ID in alle anderen Channels auf dem Server
            for(ServerTextChannel channel: channelArrayList) {
                if (channel != ownChannel) {
                    ArrayList<Message> messageArrayList = MentionFinder.getMessagesId(message, followString).getList();
                    if (messageArrayList.size() > 0) {
                        for(Message message1: messageArrayList) {
                            if (message1.getChannel().canSee(author) && message1.getChannel().canReadMessageHistory(author)) {
                                postEmbed(message.getServerTextChannel().get(), message1);
                                return true;
                            }
                        }
                    }
                }
            }

            //Sucht anhand dem Inhalt der Nachricht im eigene Channel
            if (channelArrayList.contains(ownChannel)) {
                Message m = MentionFinder.getMessageSearch(followString, ownChannel, message);
                if (m != null && m.getChannel().canSee(author) && m.getChannel().canReadMessageHistory(author)) {
                    postEmbed(message.getServerTextChannel().get(),m);
                    return true;
                }
            }

            //Sucht anhand dem Inhalt der Nachricht in den anderen Channels des Servers.
            for(ServerTextChannel channel: channelArrayList) {
                if (channel != ownChannel) {
                    Message m = MentionFinder.getMessageSearch(followString, channel, message);
                    if (m != null && m.getChannel().canSee(author) && m.getChannel().canReadMessageHistory(author)) {
                        postEmbed(message.getServerTextChannel().get(), m);
                        return true;
                    }
                }
            }

            //Wenn keines der Suchmethoden funktioniert hat (keine Ergebnisse)
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(),TextManager.GENERAL,"no_results"));

            if (add == null) eb.setDescription(getString("noresult",list.getResultMessageString()));
            else eb.setDescription(getString("noresult_channel",list.getResultMessageString(),add));

            message.getChannel().sendMessage(eb).get();
            return false;
        } else {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this,getString("noarg",message.getUserAuthor().get().getMentionTag()));

            message.getChannel().sendMessage(eb).get();
            return false;
        }
    }

    public void postEmbed(ServerTextChannel channel, Message searchedMessage) throws IOException, ExecutionException, InterruptedException {
        if (!channel.canYouWrite() || !channel.canYouEmbedLinks()) return;

        if (searchedMessage.getServerTextChannel().get().isNsfw() && !channel.isNsfw()) {
            channel.sendMessage(EmbedFactory.getNSFWBlockEmbed(getLocale())).get();
            return;
        }

        EmbedBuilder eb;

        if (searchedMessage.getEmbeds().size() == 0) {
            eb = EmbedFactory.getEmbed()
                    .setFooter(getString("title"));
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
            if (embed.getFooter().isPresent()) eb.setFooter(embed.getFooter().get().getText().get() + " - " + getString("title"));
            else eb.setFooter(getString("title"));
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
