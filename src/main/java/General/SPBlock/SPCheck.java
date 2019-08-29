package General.SPBlock;

import Commands.Moderation.SelfPromotionBlockCommand;
import Constants.SPAction;
import Constants.Settings;
import General.EmbedFactory;
import General.ModerationStatus;
import General.TextManager;
import General.Tools;
import MySQL.DBServer;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.server.Server;
import org.javacord.api.entity.server.invite.RichInvite;
import org.javacord.api.entity.user.User;

import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class SPCheck {
    public static boolean checkForSelfPromotion(Server server, Message message) {
        String content = message.getContent();
        content = content.replaceAll("(?i)"+ Settings.SERVER_INVITE_URL, "");
        if (contentContainsDiscordLink(content) && !message.getUserAuthor().get().isBot()) {
            try {
                for(RichInvite richInvite: server.getInvites().get()) {
                    content = content.replaceAll("(?i)discord.gg/"+richInvite.getCode(), "");
                }
            } catch (InterruptedException | ExecutionException e) {
                //Ignore
            }

            if (contentContainsDiscordLink(content)) {
                try {
                    SPBlock spBlock = DBServer.getSPBlockFromServer(server);
                    if (spBlock.isActive() && !spBlock.getIgnoredUser().contains(message.getUserAuthor().get()) && !spBlock.getIgnoredChannels().contains(message.getServerTextChannel().get()) && !Tools.userHasAdminPermissions(server, message.getUserAuthor().get())) {
                        boolean successful = true;
                        User author = message.getUserAuthor().get();

                        //Nachricht löschen
                        try {
                            message.delete().get();
                        } catch (Throwable throwable) {
                            successful = false;
                            //Ignore
                        }

                        //Poster informieren
                        Locale locale = DBServer.getServerLocale(server);
                        SelfPromotionBlockCommand selfPromotionBlockCommand = new SelfPromotionBlockCommand();
                        selfPromotionBlockCommand.setLocale(locale);

                        EmbedBuilder ebUser = EmbedFactory.getCommandEmbedStandard(selfPromotionBlockCommand)
                                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_maction"), TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_mactionlist").split("\n")[spBlock.getAction().ordinal()], true)
                                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_content"), message.getContent(), true)
                                .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_successful_user"));
                        try {
                            author.sendMessage(ebUser).get();
                        } catch (Throwable throwable) {
                            //Ignore
                        }

                        //User kicken
                        if (spBlock.getAction() == SPAction.KICK_USER) {
                            try {
                                server.kickUser(author, TextManager.getString(locale, TextManager.COMMANDS, "spblock_auditlog_sp")).get();
                            } catch (Throwable throwable) {
                                successful = false;
                                //Ignore
                            }
                        }

                        //User bannen
                        if (spBlock.getAction() == SPAction.BAN_USER) {
                            try {
                                server.banUser(author, 1, TextManager.getString(locale, TextManager.COMMANDS, "spblock_auditlog_sp")).get();
                            } catch (Throwable throwable) {
                                successful = false;
                                //Ignore
                            }
                        }

                        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(selfPromotionBlockCommand)
                                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_maction"), TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_mactionlist").split("\n")[spBlock.getAction().ordinal()], true)
                                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_channel"), message.getServerTextChannel().get().getMentionTag(), true)
                                .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_content"), message.getContent(), true);
                        if (successful) eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_successful", author.getMentionTag()));
                        else eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_failed", author.getMentionTag()));

                        for(User user: spBlock.getLogRecievers()) {
                            user.sendMessage(eb).get();
                        }

                        ModerationStatus moderationStatus = DBServer.getModerationFromServer(server);
                        if (moderationStatus.getChannel() != null) {
                            eb = EmbedFactory.getCommandEmbedStandard(selfPromotionBlockCommand)
                                    .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_maction"), TextManager.getString(locale, TextManager.COMMANDS, "spblock_state0_mactionlist").split("\n")[spBlock.getAction().ordinal()], true)
                                    .addField(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_channel"), message.getServerTextChannel().get().getMentionTag(), true);
                            if (successful) eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_successful", author.getMentionTag()));
                            else eb.setDescription(TextManager.getString(locale, TextManager.COMMANDS, "spblock_log_failed", author.getMentionTag()));

                            moderationStatus.getChannel().sendMessage(eb).get();
                        }

                        return true;
                    }
                } catch (Throwable throwable) {
                    throwable.printStackTrace();
                }

            }
        }

        return false;
    }

    private static boolean contentContainsDiscordLink(String string) {
        return string.toLowerCase().contains("discord.gg/");
    }
}
