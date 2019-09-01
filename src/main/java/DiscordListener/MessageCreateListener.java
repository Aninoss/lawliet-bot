package DiscordListener;

import CommandListeners.*;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import Commands.General.QuoteCommand;
import Commands.PowerPlant.PowerPlantSetupCommand;
import Constants.FishingCategoryInterface;
import Constants.PowerPlantStatus;
import GUIPackage.GUI;
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
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Random;

public class MessageCreateListener {
    public MessageCreateListener() {}

    public void onMessageCreate(MessageCreateEvent event) {
        if (!event.getMessage().getUserAuthor().isPresent() || event.getMessage().getAuthor().isYourself() || event.getMessage().getUserAuthor().get().isBot()) return;

        if (!event.getServer().isPresent()) {
            event.getChannel().sendMessage(new EmbedBuilder()
                    .setColor(Color.RED)
                    .setTitle("❌ Not Supported!".toUpperCase())
                    .setDescription("Commands via pm aren't supported!"));
            return;
        }

        //Server protections
        if (!Tools.serverIsBotListServer(event.getServer().get())) {
            if (SPCheck.checkForSelfPromotion(event.getServer().get(), event.getMessage())) return;
            if (BannedWordsCheck.checkForBannedWordUsaqe(event.getServer().get(), event.getMessage())) return;
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
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        try {
            Locale locale = DBServer.getServerLocale(event.getServer().get());
            if (manageForwardedMessages(event)) return;

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
                GUI.getInstance().addLog(event.getServer().get(), event.getMessageAuthor().asUser().get(), event.getMessageContent());

                String newContent = Tools.cutSpaces(content.substring(prefixes[prefixFound].length()));
                while (newContent.contains("  ")) newContent = newContent.replaceAll(" {2}", " ");
                String commandTrigger = newContent.split(" ")[0].toLowerCase();
                String followedString = Tools.cutSpaces(newContent.substring(commandTrigger.length()));

                if (commandTrigger.length() > 0) {
                    if (commandTrigger.equals("rmess")) commandTrigger = "reactionroles";
                    if (commandTrigger.equals("basicroles")) commandTrigger = "autoroles";
                    if (commandTrigger.equals("fishingsetup")) commandTrigger = "fishery";
                    if (commandTrigger.equals("modsettings")) commandTrigger = "mod";

                    Class clazz = CommandContainer.getInstance().getCommands().get(commandTrigger);
                    if (clazz != null) {
                        if (event.getChannel().canYouWrite() || commandTrigger.equalsIgnoreCase("help")) {
                            if (Tools.userHasAdminPermissions(event.getServer().get(), event.getMessage().getUserAuthor().get()) || DBServer.isChannelWhitelisted(event.getServer().get(), event.getServerTextChannel().get())) {
                                Command command = CommandManager.createCommandByClass(clazz, locale, prefix);

                                CommandManager.manage(event, command, followedString);
                            }
                        } else {
                            if (event.getChannel().canYouAddNewReactions()) {
                                event.addReactionsToMessage("✏");
                                event.addReactionsToMessage("❌");
                                event.getMessage().getUserAuthor().get().sendMessage(TextManager.getString(locale, TextManager.GENERAL, "no_writing_permissions", event.getServerTextChannel().get().getMentionTag())).get();
                            }
                        }
                    }
                }
            } else {

                //Add Fisch & Manage 100 Fish Message
                try {
                    if (!Tools.serverIsBotListServer(event.getServer().get()) &&
                            !event.getMessage().getUserAuthor().get().isBot() &&
                            DBUser.addJoule(event.getServer().get(), event.getServerTextChannel().get(), event.getMessage().getUserAuthor().get()) &&
                            event.getChannel().canYouWrite() &&
                            event.getChannel().canYouEmbedLinks()
                    ) {

                        event.getChannel().sendMessage(new EmbedBuilder()
                                .setColor(Color.WHITE)
                                .setAuthor(event.getMessage().getUserAuthor().get())
                                .setTitle(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_title"))
                                .setDescription(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_description").replace("%PREFIX", prefix))
                                .setFooter(TextManager.getString(locale, TextManager.GENERAL, "hundret_joule_collected_footer").replace("%PREFIX", prefix)));

                    }
                } catch (Throwable t) {
                    //Ignore
                }

                //Manage Treasure Chests
                if (!Tools.serverIsBotListServer(event.getServer().get()) &&
                        new Random().nextInt(400) == 0 &&
                        DBServer.getPowerPlantStatusFromServer(event.getServer().get()) == PowerPlantStatus.ACTIVE &&
                        DBServer.getPowerPlantTreasureChestsFromServer(event.getServer().get()) &&
                        event.getChannel().canYouWrite() &&
                        event.getChannel().canYouEmbedLinks() &&
                        event.getChannel().canYouAddNewReactions() &&
                        !event.getMessage().getUserAuthor().get().isBot()
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
                        EmbedBuilder eb = EmbedFactory.getEmbed()
                                .setTitle(PowerPlantSetupCommand.treasureEmoji + " " + TextManager.getString(locale, TextManager.COMMANDS, "fishery_treasure_title") + Tools.getEmptyCharacter())
                                .setDescription(TextManager.getString(locale, TextManager.COMMANDS, "fishery_treasure_desription", PowerPlantSetupCommand.keyEmoji))
                                .setImage(ResourceManager.getFile(ResourceManager.RESOURCES, "treasure_closed.png"));

                        Message message = event.getChannel().sendMessage(eb).get();
                        message.addReaction(PowerPlantSetupCommand.keyEmoji);
                    }

                }

                //Manage Message Quoting
                if (!Tools.serverIsBotListServer(event.getServer().get()) && event.getChannel().canYouWrite() && event.getChannel().canYouEmbedLinks()) {
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
        } catch (Throwable e) {
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

                RunningCommandManager.getInstance().add(event.getMessage().getUserAuthor().get(), command.getTrigger());

                boolean canPost = false;
                try {
                    canPost = message != null && message.getLatestInstance().get() != null;
                } catch (Throwable e) {
                    //Ignore
                }

                try {
                    if (canPost) {
                        if (command instanceof onForwardedRecievedListener) {
                            command.onForwardedRecievedSuper(event);

                            RunningCommandManager.getInstance().remove(event.getMessage().getUserAuthor().get(), command.getTrigger());
                            return true;
                        }
                        if (command instanceof onNavigationListener) {
                            boolean end = command.onNavigationMessageSuper(event, event.getMessage().getContent(), false);
                            if (end) {
                                RunningCommandManager.getInstance().remove(event.getMessage().getUserAuthor().get(), command.getTrigger());
                                return true;
                            }
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