package modules;

import commands.CommandManager;
import commands.runnables.moderationcategory.InviteFilterCommand;
import constants.Category;
import constants.ExternalLinks;
import core.DiscordApiCollection;
import core.EmbedFactory;
import core.utils.PermissionUtil;
import core.TextManager;
import mysql.modules.spblock.DBSPBlock;
import mysql.modules.spblock.SPBlockBean;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.server.invite.RichInvite;
import org.javacord.api.entity.user.User;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

public class InviteFilterCheck {

    public static boolean check(Message message) throws ExecutionException, InstantiationException, IllegalAccessException, InterruptedException, InvocationTargetException {
        Server server = message.getServer().get();
        User author = message.getUserAuthor().get();
        SPBlockBean spBlockBean = DBSPBlock.getInstance().getBean(server.getId());
        Locale locale = spBlockBean.getServerBean().getLocale();

        if (hasPostedSP(server, message, spBlockBean)) {
            InviteFilterCommand inviteFilterCommand = (InviteFilterCommand) CommandManager.createCommandByClass(InviteFilterCommand.class, locale, spBlockBean.getServerBean().getPrefix());

            informMessageAuthor(spBlockBean, inviteFilterCommand, locale, message, author);

            boolean successful = safeDeleteMessage(message);
            successful = successful && safeKick(spBlockBean, server, author);
            successful = successful && safeBan(spBlockBean, server, author);

            informLogReceivers(spBlockBean, inviteFilterCommand, locale, message, author, successful);

            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(inviteFilterCommand)
                    .addField(TextManager.getString(locale, Category.MODERATION, "invitefilter_state0_maction"), TextManager.getString(locale, Category.MODERATION, "invitefilter_state0_mactionlist").split("\n")[spBlockBean.getAction().ordinal()], true)
                    .addField(TextManager.getString(locale, Category.MODERATION, "invitefilter_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                    .addField(TextManager.getString(locale, Category.MODERATION, "invitefilter_log_content"), message.getContent(), true);;

            if (successful) eb.setDescription(TextManager.getString(locale, Category.MODERATION, "invitefilter_log_successful", author.getMentionTag()));
            else eb.setDescription(TextManager.getString(locale, Category.MODERATION, "invitefilter_log_failed", author.getMentionTag()));

            Mod.postLog(CommandManager.createCommandByClass(InviteFilterCommand.class, spBlockBean.getServerBean().getLocale(), spBlockBean.getServerBean().getPrefix()), eb, server);
            Mod.insertWarning(spBlockBean.getServerBean().getLocale(), server, author, DiscordApiCollection.getInstance().getYourself(), TextManager.getString(spBlockBean.getServerBean().getLocale(), Category.MODERATION, "invitefilter_title"), true);

            return false;
        }

        return true;
    }

    private static void informLogReceivers(SPBlockBean spBlockBean, InviteFilterCommand inviteFilterCommand, Locale locale, Message message, User author, boolean successful) {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(inviteFilterCommand)
                .addField(TextManager.getString(locale, Category.MODERATION, "invitefilter_state0_maction"), TextManager.getString(locale, Category.MODERATION, "invitefilter_state0_mactionlist").split("\n")[spBlockBean.getAction().ordinal()], true)
                .addField(TextManager.getString(locale, Category.MODERATION, "invitefilter_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                .addField(TextManager.getString(locale, Category.MODERATION, "invitefilter_log_content"), message.getContent(), true);

        if (successful) eb.setDescription(TextManager.getString(locale, Category.MODERATION, "invitefilter_log_successful", author.getMentionTag()));
        else eb.setDescription(TextManager.getString(locale, Category.MODERATION, "invitefilter_log_failed", author.getMentionTag()));

        spBlockBean.getLogReceiverUserIds().transform(message.getServer().get()::getMemberById, DiscordEntity::getId).forEach(user -> {
            try {
                user.sendMessage(eb).get();
            } catch (ExecutionException | InterruptedException e) {
                //Ignore
            }
        });
    }

    private static boolean safeBan(SPBlockBean spBlockBean, Server server, User author) throws InterruptedException {
        if (spBlockBean.getAction() == SPBlockBean.ActionList.BAN_USER) {
            try {
                server.banUser(author, 0, TextManager.getString(spBlockBean.getServerBean().getLocale(), Category.MODERATION, "invitefilter_auditlog_sp")).get();
            } catch (ExecutionException e) {
                return false;
                //Ignore
            }
        }

        return true;
    }

    private static boolean safeKick(SPBlockBean spBlockBean, Server server, User author) throws InterruptedException {
        if (spBlockBean.getAction() == SPBlockBean.ActionList.KICK_USER) {
            try {
                server.kickUser(author, TextManager.getString(spBlockBean.getServerBean().getLocale(), Category.MODERATION, "invitefilter_auditlog_sp")).get();
            } catch (ExecutionException e) {
                return false;
                //Ignore
            }
        }

        return true;
    }

    private static void informMessageAuthor(SPBlockBean spBlockBean, InviteFilterCommand inviteFilterCommand, Locale locale, Message message, User author) throws InterruptedException {
        EmbedBuilder ebUser = EmbedFactory.getCommandEmbedStandard(inviteFilterCommand)
                .addField(TextManager.getString(locale, Category.MODERATION, "invitefilter_state0_maction"), TextManager.getString(locale, Category.MODERATION, "invitefilter_state0_mactionlist").split("\n")[spBlockBean.getAction().ordinal()], true)
                .addField(TextManager.getString(locale, Category.MODERATION, "invitefilter_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                .addField(TextManager.getString(locale, Category.MODERATION, "invitefilter_log_content"), message.getContent(), true)
                .setDescription(TextManager.getString(locale, Category.MODERATION, "invitefilter_log_successful_user"));
        try {
            author.sendMessage(ebUser).get();
        } catch (ExecutionException e) {
            //Ignore
        }
    }

    private static boolean safeDeleteMessage(Message message) throws InterruptedException {
        try {
            message.delete().get();
        } catch (ExecutionException e) {
            return false;
            //Ignore
        }
        return true;
    }

    private static boolean hasPostedSP(Server server, Message message, SPBlockBean spBlockBean) throws InterruptedException {
        if (spBlockBean.isActive() &&
                !spBlockBean.getIgnoredUserIds().contains(message.getUserAuthor().get().getId()) &&
                !spBlockBean.getIgnoredChannelIds().contains(message.getServerTextChannel().get().getId()) &&
                !PermissionUtil.hasAdminPermissions(server, message.getUserAuthor().get()) &&
                !message.getUserAuthor().get().isBot()
        ) {
            String content = message.getContent();
            content = content.replaceAll("(?i)" + Pattern.quote(ExternalLinks.SERVER_INVITE_URL), "");
            if (contentContainsDiscordLink(content)) {
                try {
                    for (RichInvite richInvite : server.getInvites().get()) {
                        content = content.replace(" ", "").replaceAll("(?i)" + Pattern.quote("discord.gg/" + richInvite.getCode()), "");
                    }
                    return contentContainsDiscordLink(content);
                } catch (ExecutionException e) {
                    //Ignore
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean contentContainsDiscordLink(String string) {
        return string.toLowerCase().contains("discord.gg/") || string.toLowerCase().contains("discordapp.com/invite/") || string.toLowerCase().contains("discord.com/invite/");
    }

}
