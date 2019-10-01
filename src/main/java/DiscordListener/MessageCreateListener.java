package DiscordListener;

import CommandListeners.*;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import Commands.General.QuoteCommand;
import Commands.PowerPlant.FisheryCommand;
import Constants.FishingCategoryInterface;
import Constants.PowerPlantStatus;
import General.*;
import General.BannedWords.BannedWordsCheck;
import General.BotResources.ResourceManager;
import General.Fishing.FishingProfile;
import General.Internet.Internet;
import General.Mention.MentionFinder;
import General.RunningCommands.RunningCommandManager;
import General.SPBlock.SPCheck;
import MySQL.DBServer;
import MySQL.DBUser;
import MySQL.FisheryCache;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class MessageCreateListener {
    public MessageCreateListener() {}

    public void onMessageCreate(MessageCreateEvent event) {
        if (!event.getMessage().getUserAuthor().isPresent() || event.getMessage().getAuthor().isYourself() || event.getMessage().getUserAuthor().get().isBot()) return;

        if (!event.getServer().isPresent()) {
            event.getChannel().sendMessage(EmbedFactory.getEmbedError()
                    .setTitle("❌ Not Supported!".toUpperCase())
                    .setDescription("Commands via pm aren't supported!"));
            return;
        }

        //Server protections
        if (!Tools.serverIsBotListServer(event.getServer().get())) {
            if (SPCheck.checkForSelfPromotion(event.getServer().get(), event.getMessage())) return; //SPBlock
            if (BannedWordsCheck.checkForBannedWordUsaqe(event.getServer().get(), event.getMessage())) return; //Banned Words
        }

        //Stuff that is only active for my own Aninoss Discord server
        if (event.getServer().get().getId() == 462405241955155979L && Internet.stringIsURL(event.getMessage().getContent())) {
            try {
                FishingProfile  fishingProfile = DBUser.getFishingProfile(event.getServer().get(), event.getMessage().getUserAuthor().get());
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
            String prefix = DBServer.getPrefix(event.getServer().get());
            String content = event.getMessage().getContent();

            String[] prefixes = {prefix, event.getApi().getYourself().getMentionTag(), "<@!"+event.getApi().getYourself().getIdAsString()+">"};

            int prefixFound = -1;
            for(int i=0; i<prefixes.length; i++) {
                if (content.toLowerCase().startsWith(prefixes[i].toLowerCase())) {
                    prefixFound = i;
                    break;
                }
            }

            if (prefixFound > -1) {
                String newContent = Tools.cutSpaces(content.substring(prefixes[prefixFound].length()));
                while (newContent.contains("  ")) newContent = newContent.replaceAll(" {2}", " ");
                String commandTrigger = newContent.split(" ")[0].toLowerCase();
                String followedString = Tools.cutSpaces(newContent.substring(commandTrigger.length()));

                if (commandTrigger.length() > 0) {
                    Class clazz = CommandContainer.getInstance().getCommands().get(commandTrigger);
                    if (clazz != null) {
                        Locale locale = DBServer.getServerLocale(event.getServer().get());
                        if (event.getChannel().canYouWrite() || ((commandTrigger.equalsIgnoreCase("help") || commandTrigger.equalsIgnoreCase("commands") ) && Tools.canSendPrivateMessage(event.getMessage().getUserAuthor().get()))) {
                            if (event.getServer().get().canManage(event.getMessage().getUserAuthor().get()) || DBServer.isChannelWhitelisted(event.getServer().get(), event.getServerTextChannel().get())) {
                                Command command = CommandManager.createCommandByClass(clazz, locale, prefix);

                                CommandManager.manage(event, command, followedString);
                            }
                        } else {
                            if (event.getChannel().canYouAddNewReactions()) {
                                event.addReactionsToMessage("✏");
                                event.addReactionsToMessage("❌");
                                event.getMessage().getUserAuthor().get().sendMessage(TextManager.getString(locale, TextManager.GENERAL, "no_writing_permissions", event.getServerTextChannel().get().getName())).get();
                            }
                        }
                    }
                }
            } else {
                //Forwarded Messages
                if (manageForwardedMessages(event)) return;

                //Add Fisch & Manage 100 Fish Message
                if (!Tools.serverIsBotListServer(event.getServer().get())) {
                    FisheryCache.getInstance().addActivity(event.getMessage().getUserAuthor().get(), event.getServerTextChannel().get());
                }

                //Manage Treasure Chests
                if (!Tools.serverIsBotListServer(event.getServer().get()) &&
                        new Random().nextInt(400) == 0 &&
                        DBServer.getPowerPlantStatusFromServer(event.getServer().get()) == PowerPlantStatus.ACTIVE &&
                        DBServer.getPowerPlantTreasureChestsFromServer(event.getServer().get()) &&
                        event.getChannel().canYouWrite() &&
                        event.getChannel().canYouEmbedLinks() &&
                        event.getChannel().canYouAddNewReactions()
                ) {

                    boolean noSpamChannel = true;
                    ArrayList<ServerTextChannel> channels = DBServer.getPowerPlantIgnoredChannelsFromServer(event.getServer().get());
                    for(ServerTextChannel channel: channels) {
                        if (channel.getId() == event.getChannel().getId()) {
                            noSpamChannel = false;
                            break;
                        }
                    }

                    if (noSpamChannel) {
                        Locale locale = DBServer.getServerLocale(event.getServer().get());
                        EmbedBuilder eb = EmbedFactory.getEmbed()
                                .setTitle(FisheryCommand.treasureEmoji + " " + TextManager.getString(locale, TextManager.COMMANDS, "fishery_treasure_title") + Tools.getEmptyCharacter())
                                .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "fishery_treasure_desription", FisheryCommand.keyEmoji))
                                .setImage(ResourceManager.getFile(ResourceManager.RESOURCES, "treasure_closed.png"));

                        Message message = event.getChannel().sendMessage(eb).get();
                        message.addReaction(FisheryCommand.keyEmoji);
                    }

                }

                //Manage Message Quoting
                if (!Tools.serverIsBotListServer(event.getServer().get()) && event.getChannel().canYouWrite() && event.getChannel().canYouEmbedLinks()) {
                    Locale locale = DBServer.getServerLocale(event.getServer().get());
                    try {
                        ArrayList<Message> messages = MentionFinder.getMessagesURL(event.getMessage(), event.getMessage().getContent()).getList();
                        for (int i = 0; i < Math.min(3, messages.size()); i++) {
                            Message message = messages.get(i);
                            QuoteCommand quoteCommand = new QuoteCommand();
                            quoteCommand.setLocale(locale);
                            quoteCommand.postEmbed(event.getServerTextChannel().get(), message);
                        }
                    } catch (Throwable throwable) {
                        ExceptionHandler.handleException(throwable, locale, event.getServerTextChannel().get());
                    }
                }

            }
        } catch (IOException | InstantiationException | ExecutionException | SQLException | InterruptedException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private boolean manageForwardedMessages(MessageCreateEvent event) {
        ArrayList<Command> list = CommandContainer.getInstance().getMessageForwardInstances();
        for (int i=list.size()-1; i >= 0; i--) {
            Command command = list.get(i);
            if ((event.getChannel().getId() == command.getForwardChannelID() || command.getForwardChannelID() == -1) && (event.getMessage().getUserAuthor().get().getId() == command.getForwardUserID() || command.getForwardUserID() == -1)) {
                Message message = null;
                if (command instanceof onForwardedRecievedListener) message = ((onForwardedRecievedListener) command).getForwardedMessage();
                else if (command instanceof onNavigationListener) message = command.getNavigationMessage();

                boolean canPost = false;
                try {
                    canPost = message != null && message.getLatestInstance().get() != null;
                } catch (InterruptedException | ExecutionException e) {
                    //Ignore
                }

                try {
                    if (canPost) {
                        RunningCommandManager.getInstance().add(event.getMessage().getUserAuthor().get(), command.getTrigger());

                        if (command instanceof onForwardedRecievedListener) {
                            boolean end = command.onForwardedRecievedSuper(event);
                            RunningCommandManager.getInstance().remove(event.getMessage().getUserAuthor().get(), command.getTrigger());
                            if (end) return true;
                        }
                        if (command instanceof onNavigationListener) {
                            boolean end = command.onNavigationMessageSuper(event, event.getMessage().getContent(), false);
                            RunningCommandManager.getInstance().remove(event.getMessage().getUserAuthor().get(), command.getTrigger());
                            if (end) return true;
                        }
                    }
                } catch (Throwable e) {
                    ExceptionHandler.handleException(e, command.getLocale(), event.getServerTextChannel().get());
                }

                RunningCommandManager.getInstance().remove(event.getMessage().getUserAuthor().get(), command.getTrigger());
            }
        }

        return false;
    }
}