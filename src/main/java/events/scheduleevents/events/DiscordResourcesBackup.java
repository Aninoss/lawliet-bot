package events.scheduleevents.events;

import constants.ExceptionRunnable;
import core.LocalFile;
import core.MainLogger;
import core.Program;
import core.ShardManager;
import events.scheduleevents.ScheduleEventFixedRate;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageHistory;
import net.dv8tion.jda.api.entities.channel.concrete.Category;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.ExecutionException;

@ScheduleEventFixedRate(rateUnit = ChronoUnit.DAYS, rateValue = 7)
public class DiscordResourcesBackup implements ExceptionRunnable {

    @Override
    public void run() throws Throwable {
        execute();
    }

    public static void execute() {
        if (!Program.publicInstance()) {
            return;
        }

        ShardManager.getLocalGuildById(368531164861825024L).ifPresent(guild -> {
            MainLogger.get().info("Starting Discord resources backup for {}...", guild.getName());
            backupMessageFiles(guild);
            backupEmojis(guild);
            MainLogger.get().info("Backup completed!");
        });

        ShardManager.getLocalGuildById(1077252197978488854L).ifPresent(guild -> {
            MainLogger.get().info("Starting Discord resources backup for {}...", guild.getName());
            backupEmojis(guild);
            MainLogger.get().info("Backup completed!");
        });
    }

    private static void backupMessageFiles(Guild guild) {
        for (long channelCategoryId : new long[]{ 499629838940897280L, 736241270883876946L, 736269230080655370L, 834490361052659812L }) {
            Category category = guild.getCategoryById(channelCategoryId);
            category.getTextChannels()
                    .forEach(c -> backupMessageFilesChannel(category, c));
        }
    }

    private static void backupMessageFilesChannel(Category category, GuildMessageChannel channel) {
        MessageHistory history = channel.getHistory();
        boolean full  = true;
        while (full) {
            full = history.retrievePast(100).complete().size() == 100;
        }

        String dirPath = "files/" + category.getName() + "/" + channel.getName();
        new LocalFile(LocalFile.Directory.DISCORD_BACKUP, dirPath).mkdirs();

        for (Message message : history.getRetrievedHistory()) {
            Message.Attachment attachment = message.getAttachments().get(0);
            LocalFile localFile = new LocalFile(LocalFile.Directory.DISCORD_BACKUP, dirPath + "/" + attachment.getIdLong() + "_" + attachment.getFileName());
            if (localFile.exists()) {
                continue;
            }

            try {
                message.getAttachments().get(0).getProxy().downloadToFile(localFile).get();
            } catch (InterruptedException | ExecutionException e) {
                MainLogger.get().error("Download failed for {}", attachment.getUrl());
            }
        }
    }

    private static void backupEmojis(Guild guild) {
        String dirPath = "emojis/" + guild.getId();
        new LocalFile(LocalFile.Directory.DISCORD_BACKUP, dirPath).mkdirs();

        for (RichCustomEmoji emoji : guild.getEmojis()) {
            String extension = emoji.getImageUrl().substring(emoji.getImageUrl().lastIndexOf("."));
            LocalFile localFile = new LocalFile(LocalFile.Directory.DISCORD_BACKUP, dirPath + "/" + emoji.getName() + extension);
            if (localFile.exists()) {
                continue;
            }

            try {
                emoji.getImage().downloadToFile(localFile).get();
            } catch (InterruptedException | ExecutionException e) {
                MainLogger.get().error("Download failed for {}", emoji.getName());
            }
        }
    }

}
