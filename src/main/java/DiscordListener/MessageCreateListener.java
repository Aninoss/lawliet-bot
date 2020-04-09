package DiscordListener;

import CommandListeners.*;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import Commands.GimmicksCategory.QuoteCommand;
import Commands.FisheryCategory.FisheryCommand;
import Constants.FishingCategoryInterface;
import Constants.FisheryStatus;
import Constants.Settings;
import Core.*;
import Modules.BannedWordsCheck;
import Core.BotResources.ResourceManager;
import Modules.Fishing.FishingProfile;
import Core.Internet.Internet;
import Core.Mention.MentionTools;
import CommandSupporters.RunningCommands.RunningCommandManager;
import Modules.SPCheck;
import Core.Tools.StringTools;
import MySQL.Modules.AutoQuote.DBAutoQuote;
import MySQL.DBServerOld;
import MySQL.DBUser;
import MySQL.FisheryCache;
import MySQL.Modules.Server.DBServer;
import MySQL.Modules.Server.ServerBean;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class MessageCreateListener {

    public void onMessageCreate(MessageCreateEvent event) {
        if (!event.getMessage().getUserAuthor().isPresent() || event.getMessage().getAuthor().isYourself() || event.getMessage().getUserAuthor().get().isBot()) return;

        if (!event.getServer().isPresent()) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError()
                    .setTitle("❌ Not Supported!".toUpperCase())
                    .setDescription(String.format("Commands via dm aren't supported, you need to [\uD83D\uDD17 invite](%s) Lawliet into a server!", Settings.BOT_INVITE_URL)));
            return;
        }

        //Server protections
        if (SPCheck.checkForSelfPromotion(event.getServer().get(), event.getMessage())) return; //SPBlock
        if (BannedWordsCheck.checkForBannedWordUsaqe(event.getServer().get(), event.getMessage())) return; //Banned Words

        //Stuff that is only active for my own Aninoss Discord server
        if (event.getServer().get().getId() == 462405241955155979L && Internet.stringHasURL(event.getMessage().getContent())) {
            try {
                FishingProfile  fishingProfile = DBUser.getFishingProfile(event.getServer().get(), event.getMessage().getUserAuthor().get(), false);
                int level = fishingProfile.find(FishingCategoryInterface.ROLE).getLevel();

                if (level == 0) {
                    event.getMessage().getUserAuthor().get().sendMessage("Bevor du Links posten darfst, musst du erstmal den ersten Server-Rang erwerben!\nMehr Infos hier: <#608455541978824739>");
                    event.getServer().get().getOwner().sendMessage(event.getMessage().getUserAuthor().get().getMentionTag() + " hat Links gepostet!");
                    event.getMessage().delete().get();

                    return;
                }
            } catch (InterruptedException | ExecutionException | SQLException e) {
                e.printStackTrace();
            }
        }

        try {
            ServerBean serverBean = DBServer.getInstance().getBean(event.getServer().get().getId());
            String prefix = serverBean.getPrefix();
            String content = event.getMessage().getContent();

            String[] prefixes = {prefix, DiscordApiCollection.getInstance().getYourself().getMentionTag(), "<@!"+DiscordApiCollection.getInstance().getYourself().getIdAsString()+">"};

            int prefixFound = -1;
            for(int i=0; i < prefixes.length; i++) {
                if (prefixes[i] != null && content.toLowerCase().startsWith(prefixes[i].toLowerCase())) {
                    prefixFound = i;
                    break;
                }
            }

            if (prefixFound > -1) {
                //Forwarded Messages
                if (prefixFound > 0 && manageForwardedMessages(event)) return;

                String newContent = StringTools.trimString(content.substring(prefixes[prefixFound].length()));
                while (newContent.contains("  ")) newContent = newContent.replaceAll(" {2}", " ");
                String commandTrigger = newContent.split(" ")[0].toLowerCase();
                if (newContent.contains("<") && newContent.split("<")[0].length() < commandTrigger.length())
                    commandTrigger = newContent.split("<")[0].toLowerCase();

                String followedString = StringTools.trimString(newContent.substring(commandTrigger.length()));

                if (commandTrigger.length() > 0) {
                    Locale locale = serverBean.getLocale();
                    Class<? extends Command> clazz;
                    try {
                        clazz = CommandContainer.getInstance().getCommands().get(commandTrigger);
                        if (clazz != null) {
                            Command command = CommandManager.createCommandByClass(clazz, locale, prefix);
                            CommandManager.manage(event, command, followedString);
                        }
                    } catch (Throwable e) {
                        ExceptionHandler.handleException(e, locale, event.getServerTextChannel().get());
                    }
                }
            } else {
                //Forwarded Messages
                if (manageForwardedMessages(event)) return;

                //Add Fisch & Manage 100 Fish Message
                boolean messageRegistered = false;
                if (!event.getMessage().getContent().isEmpty())
                    messageRegistered = FisheryCache.getInstance(event.getApi().getCurrentShard()).addActivity(event.getMessage().getUserAuthor().get(), event.getServerTextChannel().get());

                //Manage Treasure Chests
                if (messageRegistered &&
                        new Random().nextInt(400) == 0 &&
                        serverBean.getFisheryStatus() == FisheryStatus.ACTIVE &&
                        serverBean.isFisheryTreasureChests() &&
                        event.getChannel().canYouWrite() &&
                        event.getChannel().canYouEmbedLinks() &&
                        event.getChannel().canYouAddNewReactions()
                ) {
                    boolean noSpamChannel = true;
                    ArrayList<ServerTextChannel> channels = DBServerOld.getPowerPlantIgnoredChannelsFromServer(event.getServer().get());
                    for (ServerTextChannel channel : channels) {
                        if (channel.getId() == event.getChannel().getId()) {
                            noSpamChannel = false;
                            break;
                        }
                    }

                    if (noSpamChannel) {
                        Locale locale = serverBean.getLocale();
                        EmbedBuilder eb = EmbedFactory.getEmbed()
                                .setTitle(FisheryCommand.treasureEmoji + " " + TextManager.getString(locale, TextManager.COMMANDS, "fishery_treasure_title") + Settings.EMPTY_EMOJI)
                                .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "fishery_treasure_desription", FisheryCommand.keyEmoji))
                                .setImage(ResourceManager.getFile(ResourceManager.RESOURCES, "treasure_closed.png"));

                        Message message = event.getChannel().sendMessage(eb).get();
                        message.addReaction(FisheryCommand.keyEmoji);
                    }
                }

                //Manage Message Quoting
                if (event.getChannel().canYouEmbedLinks()) {
                    Locale locale = serverBean.getLocale();
                    ArrayList<Message> messages = MentionTools.getMessagesURL(event.getMessage(), event.getMessage().getContent()).getList();
                    if (messages.size() > 0 && DBAutoQuote.getInstance().getBean(event.getServer().get().getId()).isActive()) {
                        try {
                            for (int i = 0; i < Math.min(3, messages.size()); i++) {
                                Message message = messages.get(i);
                                QuoteCommand quoteCommand = new QuoteCommand();
                                quoteCommand.setLocale(locale);
                                quoteCommand.setPrefix(serverBean.getPrefix());
                                quoteCommand.postEmbed(event.getServerTextChannel().get(), message, true);
                            }
                        } catch (Throwable throwable) {
                            ExceptionHandler.handleException(throwable, locale, event.getServerTextChannel().get());
                        }
                    }
                }
            }
        } catch (ExecutionException | SQLException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private boolean manageForwardedMessages(MessageCreateEvent event) {
        ArrayList<Command> list = CommandContainer.getInstance().getMessageForwardInstances();
        for (int i = list.size() - 1; i >= 0; i--) {
            Command command = list.get(i);
            if ((event.getChannel().getId() == command.getForwardChannelID() || command.getForwardChannelID() == -1) && (event.getMessage().getUserAuthor().get().getId() == command.getForwardUserID() || command.getForwardUserID() == -1)) {
                Message message = null;
                if (command instanceof OnForwardedRecievedListener) message = ((OnForwardedRecievedListener) command).getForwardedMessage();
                else if (command instanceof OnNavigationListener) message = command.getNavigationMessage();

                try {
                    RunningCommandManager.getInstance().canUserRunCommand(event.getMessage().getUserAuthor().get().getId(), event.getApi().getCurrentShard());

                    if (command instanceof OnForwardedRecievedListener) {
                        boolean end = command.onForwardedRecievedSuper(event);
                        if (end) return true;
                    }
                    if (command instanceof OnNavigationListener) {
                        boolean end = command.onNavigationMessageSuper(event, event.getMessage().getContent(), false);
                        if (end) return true;
                    }
                } catch (Throwable e) {
                    ExceptionHandler.handleException(e, command.getLocale(), event.getServerTextChannel().get());
                }
            }
        }

        return false;
    }
}