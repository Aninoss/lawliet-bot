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
import java.io.IOException;
import java.sql.SQLException;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class SPCheck {

    public static boolean checkForSelfPromotion(Server server, Message message) {
        try {
            SPBlockBean spBlockBean = DBSPBlock.getInstance().getBean(server.getId());

            if (spBlockBean.isActive() &&
                    !spBlockBean.getIgnoredUserIds().contains(message.getUserAuthor().get().getId()) &&
                    !spBlockBean.getIgnoredChannelIds().contains(message.getServerTextChannel().get().getId()) &&
                    !PermissionCheck.hasAdminPermissions(server, message.getUserAuthor().get()) &&
                    !message.getUserAuthor().get().isBot()
            ) {
                String content = message.getContent();
                content = content.replaceAll("(?i)" + Settings.SERVER_INVITE_URL, "");
                if (contentContainsDiscordLink(content)) {
                    try {
                        for(RichInvite richInvite: server.getInvites().get()) {
                            content = content.replace(" ", "").replaceAll("(?i)discord.gg/"+richInvite.getCode(), "");
                        }
                    } catch (InterruptedException | ExecutionException e) {
                        //Ignore
                    }

                    if (contentContainsDiscordLink(content)) {
                        boolean successful = true;
                        User author = message.getUserAuthor().get();

                        //Nachricht löschen
                        try {
                            message.delete().get();
                        } catch (InterruptedException | ExecutionException e) {
                            successful = false;
                            //Ignore
                        }

                        //Poster informieren
                        ServerBean serverBean = spBlockBean.getServerBean();
                        Locale locale = serverBean.getLocale();
                        SelfPromotionBlockCommand selfPromotionBlockCommand = new SelfPromotionBlockCommand();
                        selfPromotionBlockCommand.setLocale(locale);

                        EmbedBuilder ebUser = EmbedFactory.getCommandEmbedStandard(selfPromotionBlockCommand)
                                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_maction"), TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_mactionlist").split("\n")[spBlockBean.getAction().ordinal()], true)
                                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_content"), message.getContent(), true)
                                .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_successful_user"));
                        try {
                            author.sendMessage(ebUser).get();
                        } catch (InterruptedException | ExecutionException e) {
                            //Ignore
                        }

                        //User kicken
                        if (spBlockBean.getAction() == SPBlockBean.ActionList.KICK_USER) {
                            try {
                                server.kickUser(author, TextManager.getString(locale, TextManager.COMMANDS, "spblock_auditlog_sp")).get();
                            } catch (InterruptedException | ExecutionException e) {
                                successful = false;
                                //Ignore
                            }
                        }

                        //User bannen
                        if (spBlockBean.getAction() == SPBlockBean.ActionList.BAN_USER) {
                            try {
                                server.banUser(author, 1, TextManager.getString(locale, TextManager.COMMANDS, "spblock_auditlog_sp")).get();
                            } catch (InterruptedException | ExecutionException e) {
                                successful = false;
                                //Ignore
                            }
                        }

                        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(selfPromotionBlockCommand)
                                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_maction"), TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_mactionlist").split("\n")[spBlockBean.getAction().ordinal()], true)
                                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_content"), message.getContent(), true);
                        if (successful) eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_successful", author.getMentionTag()));
                        else eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_failed", author.getMentionTag()));

                        EmbedBuilder finalEb = eb;
                        spBlockBean.getLogReceiverUserIds().transform(server::getMemberById, DiscordEntity::getId).forEach(user -> {
                            try {
                                user.sendMessage(finalEb).get();
                            } catch (InterruptedException | ExecutionException e) {
                                e.printStackTrace();
                            }
                        });

                        eb = EmbedFactory.getCommandEmbedStandard(selfPromotionBlockCommand)
                                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_maction"), TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_mactionlist").split("\n")[spBlockBean.getAction().ordinal()], true)
                                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_channel"), message.getServerTextChannel().get().getMentionTag(), true);
                        if (successful) eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_successful", author.getMentionTag()));
                        else eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_failed", author.getMentionTag()));

                        ModSettingsCommand.postLog(CommandManager.createCommandByClass(SelfPromotionBlockCommand.class, locale), eb, server);
                        ModSettingsCommand.insertWarning(locale, server, author, DiscordApiCollection.getInstance().getYourself(), TextManager.getString(locale, TextManager.COMMANDS, "spblock_title"));

                        return true;
                    }
                }
            }
        } catch (ExecutionException | IOException | SQLException | IllegalAccessException | InstantiationException e) {
            e.printStackTrace();
        }

        return false;
    }

    private static boolean contentContainsDiscordLink(String string) {
        return string.toLowerCase().contains("discord.gg/");
    }

}
