package Commands.BotManagement;

import CommandListeners.*;
import CommandSupporters.CategoryCalculator;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import Constants.*;
import General.*;
import General.EmojiConnection.BackEmojiConnection;
import General.EmojiConnection.EmojiConnection;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.awt.*;
import java.util.ArrayList;

public class HelpCommand extends Command implements onNavigationListener {
    private Message messageCache;
    private ArrayList<EmojiConnection> emojiConnections;
    private Message authorMessage;
    private String searchTerm;
    private MessageCreateEvent authorEvent;

    public HelpCommand() {
        super();
        trigger = "help";
        privateUse = false;
        botPermissions = 0;
        userPermissions = 0;
        nsfw = false;
        withLoadingBar = false;
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/information-icon.png";
        emoji = "❕";
        executable = true;
        deleteOnTimeOut = false;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, boolean firstTime) throws Throwable {
        if (firstTime) {
            searchTerm = inputString;
            authorEvent = event;
            return Response.TRUE;
        }

        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i) throws Throwable {
        for (EmojiConnection emojiConnection: emojiConnections) {
            if (emojiConnection.isEmoji(event.getEmoji())) {
                searchTerm = emojiConnection.getConnection();

                if (searchTerm.equals("quit")) {
                    deleteNavigationMessage();
                    return false;
                }

                if (searchTerm.startsWith("exec:")) {
                    String className = searchTerm.split(":")[1];
                    Command command = CommandManager.createCommandByClassName(className, locale, prefix);
                    command.setReactionUserID(event.getUser().getId());
                    command.setWithLoadingBar(false);

                    CommandManager.manage(authorEvent, command, "");

                    return false;
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api) throws Throwable {
        String arg = Tools.cutSpaces(searchTerm);
        if (arg.startsWith("<") && arg.endsWith(">")) arg = arg.substring(1,arg.length()-1);

        ServerTextChannel channel = getAuthorMessage().getServerTextChannel().get();

        EmbedBuilder eb;
        if ((eb = checkCommand(channel, arg)) == null) {
            if ((eb = checkCategory(channel ,arg)) == null) {
                eb = checkMainPage(channel ,arg);
                if (arg.length() > 0) setLog(LogStatus.FAILURE, TextManager.getString(locale, TextManager.GENERAL, "no_results_description", arg));
            }
        }

        return eb;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 18;
    }

    private EmbedBuilder checkCommand(ServerTextChannel channel, String arg) throws Throwable {
        for (Class clazz : CommandContainer.getInstance().getCommands().values()) {
            Command command = CommandManager.createCommandByClass(clazz, locale, prefix);
            String commandTrigger = command.getTrigger();
            if (commandTrigger.equalsIgnoreCase(arg) && !commandTrigger.equals(trigger) && (!command.isPrivate() || getAuthorMessage().getUserAuthor().get().isBotOwner())) {
                emojiConnections = new ArrayList<>();
                emojiConnections.add(new BackEmojiConnection(channel.getApi(), channel.canYouUseExternalEmojis() || isNavigationPrivateMessage(), command.getCategory()));

                StringBuilder usage = new StringBuilder();
                for(String line: TextManager.getString(locale,TextManager.COMMANDS,commandTrigger+"_usage").split("\n")) {
                    usage.append("• ").append(getPrefix()).append(commandTrigger).append(" ").append(line).append("\n");
                }

                StringBuilder examples = new StringBuilder();
                int exampleNumber = 0;
                for(String line: TextManager.getString(locale,TextManager.COMMANDS,commandTrigger+"_examples").split("\n")) {
                    line = Tools.solveVariablesOfCommandText(line, getAuthorMessage(), getPrefix(), trigger);
                    examples.append("• ").append(getPrefix()).append(commandTrigger).append(" ").append(line).append("\n");
                    exampleNumber++;
                }

                String addNotExecutable = "";
                if (!command.isExecutable()) {
                    addNotExecutable = "\n" + getString("command_notexecutable");
                } else if (!isNavigationPrivateMessage()) {
                    options = getString("command_execute").split("\n");
                    emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[0],"exec:"+command.getClass().getName()));
                }

                return new EmbedBuilder()
                        .setColor(Color.WHITE)
                        .setTitle(command.getEmoji()+" "+TextManager.getString(locale,TextManager.COMMANDS,commandTrigger+"_title"))
                        .setThumbnail(command.getThumbnail())
                        .setFooter(getString("command_args"))
                        .setDescription(TextManager.getString(locale,TextManager.COMMANDS,commandTrigger+"_helptext") + addNotExecutable)
                        .addField(getString("command_usage"),usage.toString(),true)
                        .addField(getString( "command_example", exampleNumber > 1),examples.toString(),true);
            }
        }
        return null;
    }

    private EmbedBuilder checkCategory(ServerTextChannel channel, String arg) throws Throwable {
        if (arg.length() > 0) {
            for (String string : Category.LIST) {
                if ((string.toLowerCase().contains(arg.toLowerCase()) || TextManager.getString(locale, TextManager.COMMANDS, string).toLowerCase().contains(arg.toLowerCase())) && (!string.equals(Category.BOT_OWNER) || getAuthorMessage().getUserAuthor().get().isBotOwner())) {
                    EmbedBuilder eb = new EmbedBuilder()
                            .setColor(Color.WHITE)
                            .setFooter(TextManager.getString(locale,TextManager.GENERAL,"reaction_navigation"))
                            .setTitle(CategoryCalculator.getEmojiOfCategory(channel.getApi(),string) +" "+TextManager.getString(locale, TextManager.COMMANDS, string));

                    StringBuilder commands = new StringBuilder();
                    emojiConnections = new ArrayList<>();
                    emojiConnections.add(new BackEmojiConnection(channel.getApi(), channel.canYouUseExternalEmojis() || isNavigationPrivateMessage(), ""));
                    int i = 0;
                    for (Class clazz : CommandContainer.getInstance().getCommandList()) {
                        Command command = CommandManager.createCommandByClass(clazz, locale, prefix);
                        String commandTrigger = command.getTrigger();
                        if (!commandTrigger.equals(trigger) && command.getCategory().equals(string) && (!command.isPrivate() || getAuthorMessage().getUserAuthor().get().isBotOwner())) {
                            commands
                                    .append("**")
                                    .append(LetterEmojis.LETTERS[i])
                                    //.append(" | ")
                                    .append(" → ")
                                    .append(command.getEmoji())
                                    .append(" ")
                                    .append(TextManager.getString(locale, TextManager.COMMANDS, commandTrigger + "_title").toUpperCase())
                                    .append("**\n").append("**").append(getPrefix()).append(commandTrigger).append("**")
                                    .append(" - ")
                                    .append(TextManager.getString(locale, TextManager.COMMANDS, commandTrigger + "_description"))
                                    .append("\n\n");
                            emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], command.getTrigger()));
                            i++;
                        }
                    }
                    eb.setDescription(commands.toString());
                    return eb;
                }
            }
        }

        return null;
    }

    private EmbedBuilder checkMainPage(ServerTextChannel channel, String arg) throws Throwable {
        EmbedBuilder eb = new EmbedBuilder()
                .setColor(Color.WHITE)
                .setFooter(getString("donate"))
                .setTitle(TextManager.getString(locale, TextManager.COMMANDS, "categories"));

        StringBuilder categoriesSB = new StringBuilder();
        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(channel.getApi(), channel.canYouUseExternalEmojis() || isNavigationPrivateMessage(), "quit"));


        int i = 0;
        for (String string : Category.LIST) {
            //categoriesSB.append(LetterEmojis.LETTERS[i]).append(" | ").append(CategoryCalculator.getEmojiOfCategory(channel.getApi(), string)).append(" ").append(TextManager.getString(locale, TextManager.COMMANDS, string)).append("\n");
            categoriesSB.append(LetterEmojis.LETTERS[i]).append(" → ").append(TextManager.getString(locale, TextManager.COMMANDS, string)).append("\n");
            emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], string));
            i++;
        }

        categoriesSB.append("\n").append(getString("sp")).append("\n").append(Tools.getEmptyCharacter());
        eb.setDescription(categoriesSB.toString());

        eb.addField(getString("links_title"), getString("links_content", Settings.SERVER_INVITE_URL, Settings.BOT_INVITE_URL, Settings.UPVOTE_URL), true);
        return eb;
    }

}
