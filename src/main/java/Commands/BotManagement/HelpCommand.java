package Commands.BotManagement;

import CommandListeners.*;
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

@CommandProperties(
        trigger = "help",
        thumbnail = "http://icons.iconarchive.com/icons/graphicloads/100-flat-2/128/information-icon.png",
        emoji = "❕",
        executable = true,
        deleteOnTimeOut = false,
        aliases = {"commands"}
)
public class HelpCommand extends Command implements onNavigationListener {

    private ArrayList<EmojiConnection> emojiConnections;
    private String searchTerm;
    private MessageCreateEvent authorEvent;

    public HelpCommand() {
        super();
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state, boolean firstTime) throws Throwable {
        if (firstTime) {
            searchTerm = inputString;
            authorEvent = event;
            return Response.TRUE;
        }

        return null;
    }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        for (EmojiConnection emojiConnection: emojiConnections) {
            if (emojiConnection.isEmoji(event.getEmoji())) {
                searchTerm = emojiConnection.getConnection();

                if (searchTerm.equals("quit")) {
                    deleteNavigationMessage();
                    return false;
                }

                if (searchTerm.startsWith("exec:")) {
                    String className = searchTerm.split(":")[1];
                    Command command = CommandManager.createCommandByClassName(className, getLocale(), getPrefix());
                    command.setReactionUserID(event.getUser().getId());
                    command.blockLoading();

                    CommandManager.manage(authorEvent, command, "");

                    return false;
                }

                return true;
            }
        }

        return false;
    }

    @Override
    public EmbedBuilder draw(DiscordApi api, int state) throws Throwable {
        String arg = Tools.cutSpaces(searchTerm);
        if (arg.startsWith("<") && arg.endsWith(">")) arg = arg.substring(1,arg.length()-1);

        ServerTextChannel channel = getStarterMessage().getServerTextChannel().get();

        EmbedBuilder eb;
        if ((eb = checkCommand(channel, arg)) == null) {
            if ((eb = checkCategory(channel ,arg)) == null) {
                eb = checkMainPage(channel ,arg);
                if (arg.length() > 0) setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_results_description", arg));
            }
        }

        return eb;
    }

    @Override
    public void onNavigationTimeOut(Message message) throws Throwable {}

    @Override
    public int getMaxReactionNumber() {
        return 12;
    }

    private EmbedBuilder checkCommand(ServerTextChannel channel, String arg) throws Throwable {
        for (Class clazz : CommandContainer.getInstance().getCommands().values()) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();
            if (commandTrigger.equalsIgnoreCase(arg) && !commandTrigger.equals(getTrigger()) && (!command.isPrivate() || getStarterMessage().getUserAuthor().get().isBotOwner())) {
                emojiConnections = new ArrayList<>();
                emojiConnections.add(new BackEmojiConnection(channel.getApi(), channel.canYouUseExternalEmojis() || isNavigationPrivateMessage(), command.getCategory()));

                StringBuilder usage = new StringBuilder();
                for(String line: TextManager.getString(getLocale(),TextManager.COMMANDS,commandTrigger+"_usage").split("\n")) {
                    usage.append("• ").append(getPrefix()).append(commandTrigger).append(" ").append(line).append("\n");
                }

                StringBuilder examples = new StringBuilder();
                int exampleNumber = 0;
                for(String line: TextManager.getString(getLocale(),TextManager.COMMANDS,commandTrigger+"_examples").split("\n")) {
                    line = Tools.solveVariablesOfCommandText(line, getStarterMessage(), getPrefix());
                    examples.append("• ").append(getPrefix()).append(commandTrigger).append(" ").append(line).append("\n");
                    exampleNumber++;
                }

                String addNotExecutable = "";
                if (!command.isExecutable()) {
                    addNotExecutable = "\n" + getString("command_notexecutable");
                } else if (!isNavigationPrivateMessage()) {
                    setOptions(getString("command_execute").split("\n"));
                    emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[0],"exec:"+command.getClass().getName()));
                }

                return EmbedFactory.getEmbed()
                        .setTitle(
                                TextManager.getString(getLocale(), TextManager.COMMANDS, "categories") + " » " +
                                        TextManager.getString(getLocale(), TextManager.COMMANDS, command.getCategory()) + " » " +
                                        command.getEmoji()+" "+TextManager.getString(getLocale(),TextManager.COMMANDS,commandTrigger+"_title")
                        )
                        .setThumbnail(command.getThumbnail())
                        .setFooter(getString("command_args"))
                        .setDescription(TextManager.getString(getLocale(),TextManager.COMMANDS,commandTrigger+"_helptext") + addNotExecutable)
                        .addField(getString("command_usage"),usage.toString(),true)
                        .addField(getString( "command_example", exampleNumber > 1),examples.toString(),true);
            }
        }
        return null;
    }

    private EmbedBuilder checkCategory(ServerTextChannel channel, String arg) throws Throwable {
        if (arg.length() > 0) {
            for (String string : Category.LIST) {
                if ((string.toLowerCase().contains(arg.toLowerCase()) || TextManager.getString(getLocale(), TextManager.COMMANDS, string).toLowerCase().contains(arg.toLowerCase())) && (!string.equals(Category.BOT_OWNER) || getStarterMessage().getUserAuthor().get().isBotOwner())) {
                    EmbedBuilder eb = EmbedFactory.getEmbed()
                            .setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "reaction_navigation"))
                            .setTitle(
                                    TextManager.getString(getLocale(), TextManager.COMMANDS, "categories") + " » " +
                                            TextManager.getString(getLocale(), TextManager.COMMANDS, string)
                            );

                    emojiConnections = new ArrayList<>();
                    emojiConnections.add(new BackEmojiConnection(channel.getApi(), channel.canYouUseExternalEmojis() || isNavigationPrivateMessage(), ""));

                    StringBuilder commands = new StringBuilder();

                    //Interactions and Emotes Category
                    if (string.equals(Category.INTERACTIONS) || string.equals(Category.EMOTES)) {
                        for (Class clazz : CommandContainer.getInstance().getCommandList()) {
                            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
                            String commandTrigger = command.getTrigger();
                            if (!commandTrigger.equals(getTrigger()) && command.getCategory().equals(string) && (!command.isPrivate() || getStarterMessage().getUserAuthor().get().isBotOwner())) {
                                commands
                                        .append(" `")
                                        .append(command.getEmoji())
                                        .append("⠀")
                                        .append(getPrefix())
                                        .append(commandTrigger)
                                        .append("`⠀⠀");
                            }
                        }
                        String commandsString = commands.toString();
                        commandsString = commandsString.substring(0, commandsString.length() - 1);
                        if (string.equals(Category.INTERACTIONS)) {
                            eb.setDescription(getString("interactions_desc"));
                            eb.addField(getString("interactions_title"), commandsString);
                        } else {
                            eb.setDescription(getString("emotes_desc"));
                            eb.addField(getString("emotes_title"), commandsString);
                        }
                    }

                    //All other categories
                    else {
                        int i = 0;
                        for (Class clazz : CommandContainer.getInstance().getCommandList()) {
                            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
                            String commandTrigger = command.getTrigger();
                            if (!commandTrigger.equals(getTrigger()) && command.getCategory().equals(string) && (!command.isPrivate() || getStarterMessage().getUserAuthor().get().isBotOwner())) {
                                commands
                                        .append("**")
                                        .append(LetterEmojis.LETTERS[i])
                                        .append(" → ")
                                        .append(command.getEmoji())
                                        .append(" ")
                                        .append(TextManager.getString(getLocale(), TextManager.COMMANDS, commandTrigger + "_title").toUpperCase())
                                        .append("**\n").append("**").append(getPrefix()).append(commandTrigger).append("**")
                                        .append(" - ")
                                        .append(TextManager.getString(getLocale(), TextManager.COMMANDS, commandTrigger + "_description"))
                                        .append("\n\n");
                                emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], command.getTrigger()));
                                i++;
                            }
                        }
                        eb.setDescription(commands.toString());
                    }
                    return eb;
                }
            }
        }

        return null;
    }

    private EmbedBuilder checkMainPage(ServerTextChannel channel, String arg) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setFooter(getString("donate"))
                .setTitle(TextManager.getString(getLocale(), TextManager.COMMANDS, "categories"));

        StringBuilder categoriesSB = new StringBuilder();
        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(channel.getApi(), channel.canYouUseExternalEmojis() || isNavigationPrivateMessage(), "quit"));


        int i = 0;
        for (String string : Category.LIST) {
            //categoriesSB.append(LetterEmojis.LETTERS[i]).append(" | ").append(CategoryCalculator.getEmojiOfCategory(channel.getApi(), string)).append(" ").append(TextManager.getString(getLocale(), TextManager.COMMANDS, string)).append("\n");
            categoriesSB.append(LetterEmojis.LETTERS[i]).append(" → ").append(TextManager.getString(getLocale(), TextManager.COMMANDS, string)).append("\n");
            emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], string));
            i++;
        }

        categoriesSB.append("\n").append(getString("sp")).append("\n").append(Tools.getEmptyCharacter());
        eb.setDescription(categoriesSB.toString());

        eb
                .addField(getString("links_title"), getString("links_content", Settings.SERVER_INVITE_URL, Settings.BOT_INVITE_URL, Settings.UPVOTE_URL), true)
                .addField(getString("giveaway_title"), getString("giveaway_desc", Settings.SERVER_INVITE_URL), true);
        return eb;
    }

}
