package Modules;

import CommandSupporters.CommandManager;
import Commands.ModerationCategory.ModSettingsCommand;
import Commands.ModerationCategory.SelfPromotionBlockCommand;
import Constants.Settings;
import Core.DiscordApiCollection;
import Core.EmbedFactory;
import Core.PermissionCheck;
import Core.TextManager;
import MySQL.Modules.SPBlock.DBSPBlock;
import MySQL.Modules.SPBlock.SPBlockBean;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.DiscordEntity;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.server.invite.RichInvite;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class SPCheck {

    public static boolean check(Message message) throws ExecutionException, InstantiationException, IllegalAccessException, InterruptedException {
        Server server = message.getServer().get();
        User author = message.getUserAuthor().get();
        SPBlockBean spBlockBean = DBSPBlock.getInstance().getBean(server.getId());
        Locale locale = spBlockBean.getServerBean().getLocale();

        if (hasPostedSP(server, message, spBlockBean)) {
            SelfPromotionBlockCommand selfPromotionBlockCommand = (SelfPromotionBlockCommand) CommandManager.createCommandByClass(SelfPromotionBlockCommand.class, locale, spBlockBean.getServerBean().getPrefix());

            informMessageAuthor(spBlockBean, selfPromotionBlockCommand, locale, message, author);

            boolean successful = safeDeleteMessage(message);
            successful = successful && safeKick(spBlockBean, server, author);
            successful = successful && safeBan(spBlockBean, server, author);

            informLogReceivers(spBlockBean, selfPromotionBlockCommand, locale, message, author, successful);

            EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(selfPromotionBlockCommand)
                    .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_maction"), TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_mactionlist").split("\n")[spBlockBean.getAction().ordinal()], true)
                    .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_channel"), message.getServerTextChannel().get().getMentionTag(), true);
            if (successful) eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_successful", author.getMentionTag()));
            else eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_failed", author.getMentionTag()));

            ModSettingsCommand.postLog(CommandManager.createCommandByClass(SelfPromotionBlockCommand.class, spBlockBean.getServerBean().getLocale()), eb, server);
            ModSettingsCommand.insertWarning(spBlockBean.getServerBean().getLocale(), server, author, DiscordApiCollection.getInstance().getYourself(), TextManager.getString(spBlockBean.getServerBean().getLocale(), TextManager.COMMANDS, "spblock_title"));

            return false;
        }

        return true;
    }

    private static void informLogReceivers(SPBlockBean spBlockBean, SelfPromotionBlockCommand selfPromotionBlockCommand, Locale locale, Message message, User author, boolean successful) {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(selfPromotionBlockCommand)
                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_maction"), TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_mactionlist").split("\n")[spBlockBean.getAction().ordinal()], true)
                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_content"), message.getContent(), true);
        if (successful) eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_successful", author.getMentionTag()));
        else eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_failed", author.getMentionTag()));

        spBlockBean.getLogReceiverUserIds().transform(message.getServer().get()::getMemberById, DiscordEntity::getId).forEach(user -> {
            try {
                user.sendMessage(eb).get();
            } catch (ExecutionException | InterruptedException e) {
                //Ignore
            }
        });
    }

    private static boolean safeBan(SPBlockBean spBlockBean, Server server, User author) throws InterruptedException {
        if (spBlockBean.getAction() == SPBlockBean.ActionList.KICK_USER) {
            try {
                server.kickUser(author, TextManager.getString(spBlockBean.getServerBean().getLocale(), TextManager.COMMANDS, "spblock_auditlog_sp")).get();
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
                server.kickUser(author, TextManager.getString(spBlockBean.getServerBean().getLocale(), TextManager.COMMANDS, "spblock_auditlog_sp")).get();
            } catch (ExecutionException e) {
                return false;
                //Ignore
            }
        }

        return true;
    }

    private static void informMessageAuthor(SPBlockBean spBlockBean, SelfPromotionBlockCommand selfPromotionBlockCommand, Locale locale, Message message, User author) throws InterruptedException {
        EmbedBuilder ebUser = EmbedFactory.getCommandEmbedStandard(selfPromotionBlockCommand)
                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_maction"), TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_mactionlist").split("\n")[spBlockBean.getAction().ordinal()], true)
                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_content"), message.getContent(), true)
                .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_successful_user"));
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

    private static boolean hasPostedSP(Server server, Message message, SPBlockBean spBlockBean) throws ExecutionException, InterruptedException {
        if (spBlockBean.isActive() &&
                !spBlockBean.getIgnoredUserIds().contains(message.getUserAuthor().get().getId()) &&
                !spBlockBean.getIgnoredChannelIds().contains(message.getServerTextChannel().get().getId()) &&
                !PermissionCheck.hasAdminPermissions(server, message.getUserAuthor().get()) &&
                !message.getUserAuthor().get().isBot()
        ) {
            String content = message.getContent();
            content = content.replaceAll("(?i)" + Settings.SERVER_INVITE_URL, "");
            if (contentContainsDiscordLink(content)) {
                for(RichInvite richInvite: server.getInvites().get()) {
                    content = content.replace(" ", "").replaceAll("(?i)discord.gg/"+richInvite.getCode(), "");
                }

                return contentContainsDiscordLink(content);
            }
        }

        return false;
    }

    private static boolean contentContainsDiscordLink(String string) {
        return string.toLowerCase().contains("discord.gg/");
    }

}
