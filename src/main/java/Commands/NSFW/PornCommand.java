package Commands.NSFW;

import CommandSupporters.Command;
import General.*;
import General.Porn.PornImage;
import General.Porn.PornImageDownloader;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class PornCommand extends Command {

    protected String domain;
    protected String imageTemplate;
    private final boolean WITH_COMMENTS = false;

    public boolean onPornRequestRecieved(MessageCreateEvent event, String followedString, String stringAdd) throws IOException, InterruptedException, ExecutionException {
        boolean emptyKey = false;
        if (followedString.length() == 0) {
            emptyKey = true;
            followedString = "animated_gif";
        }

        followedString = Tools.cutSpaces(followedString.replace(".", ""));
        PornImage pornImage = PornImageDownloader.getPicture(domain, followedString, stringAdd, imageTemplate, false);
        if (pornImage == null) {
            EmbedBuilder eb = EmbedFactory.getCommandEmbedError(this)
                    .setTitle(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results"))
                    .setDescription(TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", followedString));
            event.getChannel().sendMessage(eb).get();
            return false;
        } else {
            String footerAdd = "";
            if (emptyKey) footerAdd = " - ⚠️ " + TextManager.getString(getLocale(), TextManager.COMMANDS,"porn_nokey").toUpperCase();

            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedStandard(this, TextManager.getString(getLocale(), TextManager.COMMANDS,"porn_link", pornImage.getPageUrl()))
                    .setImage(pornImage.getImageUrl())
                    .setTimestamp(pornImage.getInstant())
                    .setFooter(TextManager.getString(getLocale(), TextManager.COMMANDS,"porn_footer", Tools.numToString(getLocale(), pornImage.getScore()), Tools.numToString(getLocale(), pornImage.getnComments())) + footerAdd)).get();
            if ( pornImage.getComments().size() > 0 && WITH_COMMENTS) {
                EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this).setTitle( TextManager.getString(getLocale(), TextManager.COMMANDS, "porn_comments"));
                for (int i = Math.max(0, pornImage.getComments().size() - 10); i < pornImage.getComments().size(); i++) {
                    Comment comment = pornImage.getComments().get(i);
                    if (comment.getAuthor().length() > 0 && comment.getContent().length() > 0) eb.addField(Tools.shortenString(comment.getAuthor(), 256), Tools.shortenString(comment.getContent(), 400));
                }
                event.getChannel().sendMessage(eb).get();
            }
            return true;
        }
    }

}
