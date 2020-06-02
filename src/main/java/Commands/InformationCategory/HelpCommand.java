package Commands.InformationCategory;

import CommandListeners.*;
import CommandSupporters.Command;
import CommandSupporters.CommandContainer;
import CommandSupporters.CommandManager;
import Commands.PornPredefinedAbstract;
import Commands.PornSearchAbstract;
import Constants.*;
import Core.*;
import Core.EmojiConnection.BackEmojiConnection;
import Core.EmojiConnection.EmojiConnection;
import Core.Utils.BotUtil;
import Core.Utils.StringUtil;
import MySQL.Modules.CommandManagement.CommandManagementBean;
import MySQL.Modules.CommandManagement.DBCommandManagement;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "help",
        emoji = "❕",
        executable = true,
        requiresEmbeds = false,
        aliases = {"commands"}
)
public class HelpCommand extends Command implements OnNavigationListener {

    private ArrayList<EmojiConnection> emojiConnections;
    private String searchTerm;
    private MessageCreateEvent authorEvent;
    private CommandManagementBean commandManagementBean;

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        searchTerm = followedString;
        authorEvent = event;
        commandManagementBean = DBCommandManagement.getInstance().getBean(event.getServer().get().getId());
        return true;
    }

    @Override
    public Response controllerMessage(MessageCreateEvent event, String inputString, int state) throws Throwable { return null; }

    @Override
    public boolean controllerReaction(SingleReactionEvent event, int i, int state) throws Throwable {
        for (EmojiConnection emojiConnection: emojiConnections) {
            if (emojiConnection.isEmoji(event.getEmoji()) || (i == -1 && emojiConnection instanceof BackEmojiConnection)) {
                searchTerm = emojiConnection.getConnection();

                if (searchTerm.equals("quit")) {
                    removeNavigationWithMessage();
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
        String arg = StringUtil.trimString(searchTerm);
        if (arg.startsWith("<") && arg.endsWith(">")) arg = arg.substring(1,arg.length()-1);

        ServerTextChannel channel = getStarterMessage().getServerTextChannel().get();

        setOptions(null);

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
    public void onNavigationTimeOut(Message message) {}

    @Override
    public int getMaxReactionNumber() {
        return 15;
    }

    private EmbedBuilder checkCommand(ServerTextChannel channel, String arg) throws Throwable {
        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();

            if (commandTrigger.equalsIgnoreCase(arg) && !commandTrigger.equals(getTrigger())) {
                emojiConnections = new ArrayList<>();
                emojiConnections.add(new BackEmojiConnection(channel.canYouUseExternalEmojis() || isNavigationPrivateMessage(), command.getCategory()));

                StringBuilder usage = new StringBuilder();
                for(String line: TextManager.getString(getLocale(),TextManager.COMMANDS,commandTrigger+"_usage").split("\n")) {
                    usage.append("• ").append(getPrefix()).append(commandTrigger).append(" ").append(line).append("\n");
                }

                StringBuilder examples = new StringBuilder();
                int exampleNumber = 0;
                for(String line: TextManager.getString(getLocale(),TextManager.COMMANDS,commandTrigger+"_examples").split("\n")) {
                    line = StringUtil.solveVariablesOfCommandText(line, getStarterMessage(), getPrefix());
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

                String permissionsList = new ListGen<Integer>().getList(PermissionCheck.permissionsToNumberList(command.getUserPermissions()), getLocale(), ListGen.SLOT_TYPE_BULLET,
                        i -> TextManager.getString(getLocale(), TextManager.PERMISSIONS, String.valueOf(i))
                );

                EmbedBuilder eb =  EmbedFactory.getEmbed()
                        .setTitle(
                                TextManager.getString(getLocale(), TextManager.COMMANDS, "categories") + " » " +
                                        TextManager.getString(getLocale(), TextManager.COMMANDS, command.getCategory()) + " » " +
                                        command.getEmoji()+" "+TextManager.getString(getLocale(),TextManager.COMMANDS,commandTrigger+"_title")
                        )
                        .setFooter(getString("command_args"))
                        .setDescription(TextManager.getString(getLocale(),TextManager.COMMANDS,commandTrigger+"_helptext") + addNotExecutable)
                        .addField(Settings.EMPTY_EMOJI, getString("command_usage") + "\n" + usage.toString(),true)
                        .addField(Settings.EMPTY_EMOJI, getString( "command_example", exampleNumber > 1) + "\n" + examples.toString(),true);

                if (command.getUserPermissions() > 0) eb.addField(Settings.EMPTY_EMOJI, getString("command_userpermissions") + "\n" + permissionsList,false);

                return eb;
            }
        }
        return null;
    }

    private EmbedBuilder checkCategory(ServerTextChannel channel, String arg) throws Throwable {
        if (arg.length() > 0) {
            for (String category : Category.LIST) {
                if ((category.toLowerCase().contains(arg.toLowerCase()) || TextManager.getString(getLocale(), TextManager.COMMANDS, category).toLowerCase().contains(arg.toLowerCase()))) {
                    EmbedBuilder eb = EmbedFactory.getEmbed()
                            .setFooter(TextManager.getString(getLocale(), TextManager.GENERAL, "reaction_navigation"))
                            .setTitle(
                                    TextManager.getString(getLocale(), TextManager.COMMANDS, "categories") + " » " +
                                            TextManager.getString(getLocale(), TextManager.COMMANDS, category)
                            );

                    emojiConnections = new ArrayList<>();
                    emojiConnections.add(new BackEmojiConnection(channel.canYouUseExternalEmojis() || isNavigationPrivateMessage(), ""));

                    switch (category) {
                        case Category.INTERACTIONS: categoryInteractionsEmotes(eb, Category.INTERACTIONS); break;
                        case Category.EMOTES: categoryInteractionsEmotes(eb, Category.EMOTES); break;
                        case Category.NSFW: categoryNSFW(eb); break;
                        default: categoryDefault(eb, category);
                    }

                    return eb;
                }
            }
        }

        return null;
    }

    private void categoryInteractionsEmotes(EmbedBuilder eb, String category) throws InstantiationException, IllegalAccessException {
        eb.setDescription(getString("emotes_desc"));

        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();
            if (!commandTrigger.equals(getTrigger()) && command.getCategory().equals(category)) {
                stringBuilder
                        .append("• `")
                        .append(command.getEmoji())
                        .append("⠀")
                        .append(getPrefix())
                        .append(commandTrigger);

                if (command.isNsfw()) stringBuilder.append(" ").append(getString("interaction_nsfw"));
                stringBuilder.append("`\n");

                i++;
                if (i >= 10) {
                    if (stringBuilder.length() > 0) eb.addField(Settings.EMPTY_EMOJI, stringBuilder.toString(), true);
                    stringBuilder = new StringBuilder();
                    i = 0;
                }
            }
        }
        if (stringBuilder.length() > 0)
            eb.addField(Settings.EMPTY_EMOJI, stringBuilder.toString(), true);

        if (category.equals(Category.INTERACTIONS))
            eb.addField(Settings.EMPTY_EMOJI, getString("interaction_nsfw_desc"))
                    .setDescription(getString("interactions_desc"));
        else
            eb.setDescription(getString("emotes_desc"));

    }

    private void categoryDefault(EmbedBuilder eb, String category) throws InstantiationException, IllegalAccessException, ExecutionException {
        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();
            User author = getStarterMessage().getUserAuthor().get();
            if (!commandTrigger.equals(getTrigger()) && command.getCategory().equals(category)) {
                StringBuilder commands = new StringBuilder();
                boolean canAccess = PermissionCheck.getMissingPermissionListForUser(authorEvent.getServer().get(), authorEvent.getServerTextChannel().get(), author, command.getUserPermissions()).size() == 0 &&
                        (!command.isNsfw() || authorEvent.getServerTextChannel().get().isNsfw()) &&
                        commandManagementBean.commandIsTurnedOn(command) &&
                        !command.isPatreonRequired() || PatreonCache.getInstance().getPatreonLevel(author.getId()) > 0;

                commands.append("**")
                        .append(LetterEmojis.LETTERS[i])
                        .append(" → ")
                        .append(command.getEmoji())
                        .append(" ");

                if (!canAccess) commands.append("~~");

                commands.append(TextManager.getString(getLocale(), TextManager.COMMANDS, commandTrigger + "_title").toUpperCase());

                if (!canAccess) commands.append("~~");
                if (command.getUserPermissions() > 0) commands.append(Settings.EMPTY_EMOJI).append(DiscordApiCollection.getInstance().getHomeEmojiById(652188097911717910L).getMentionTag());
                if (command instanceof OnTrackerRequestListener) commands.append(Settings.EMPTY_EMOJI).append(DiscordApiCollection.getInstance().getHomeEmojiById(654051035249115147L).getMentionTag());
                if (command.isNsfw()) commands.append(Settings.EMPTY_EMOJI).append(DiscordApiCollection.getInstance().getHomeEmojiById(652188472295292998L).getMentionTag());
                if (command.isPatreonRequired()) commands.append(Settings.EMPTY_EMOJI).append(DiscordApiCollection.getInstance().getHomeEmojiById(703937256070709258L).getMentionTag());

                commands.append("**\n").append("`").append(getPrefix()).append(commandTrigger).append("`")
                        .append(" - ")
                        .append(TextManager.getString(getLocale(), TextManager.COMMANDS, commandTrigger + "_description"))
                        .append("\n\n");
                emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], command.getTrigger()));
                i++;

                eb.addField(Settings.EMPTY_EMOJI, commands.toString());
            }
        }

        eb.addField(Settings.EMPTY_EMOJI, getIconDescriptions());
    }

    private void categoryNSFW(EmbedBuilder eb) throws InstantiationException, IllegalAccessException {
        eb.setDescription(getString("nsfw"));
        String patreonIcon = DiscordApiCollection.getInstance().getHomeEmojiById(703937256070709258L).getMentionTag();

        StringBuilder withSearchKey = new StringBuilder();
        StringBuilder withoutSearchKey = new StringBuilder();

        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());

            if (command.getCategory().equals(Category.NSFW)) {
                String title = TextManager.getString(getLocale(), TextManager.COMMANDS, command.getTrigger() + "_title");

                if (command instanceof PornSearchAbstract)
                    withSearchKey.append(getString("nsfw_slot", command.isPatreonRequired(), command.getTrigger(), patreonIcon, title)).append("\n");
                else if (command instanceof PornPredefinedAbstract)
                    withoutSearchKey.append(getString("nsfw_slot", command.isPatreonRequired(), command.getTrigger(), patreonIcon, title)).append("\n");
            }
        }

        withSearchKey.append("\n\n").append(getString("nsfw_searchkey_on_eg"));

        eb.addField(getString("nsfw_searchkey_on"), withSearchKey.toString(), true)
                .addField(getString("nsfw_searchkey_off"), withoutSearchKey.toString(), true)
                .addField(Settings.EMPTY_EMOJI, getIconDescriptions());
    }

    private String getIconDescriptions() {
        return getString("commandproperties",
                DiscordApiCollection.getInstance().getHomeEmojiById(652188097911717910L).getMentionTag(),
                DiscordApiCollection.getInstance().getHomeEmojiById(654051035249115147L).getMentionTag(),
                DiscordApiCollection.getInstance().getHomeEmojiById(652188472295292998L).getMentionTag(),
                DiscordApiCollection.getInstance().getHomeEmojiById(703937256070709258L).getMentionTag(),
                Settings.PATREON_PAGE
        );
    }

    private EmbedBuilder checkMainPage(ServerTextChannel channel, String arg) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setTitle(TextManager.getString(getLocale(), TextManager.COMMANDS, "categories"));

        StringBuilder categoriesSB = new StringBuilder();
        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(channel.canYouUseExternalEmojis() || isNavigationPrivateMessage(), "quit"));


        int i = 0;
        for (String string : Category.LIST) {
            categoriesSB.append(LetterEmojis.LETTERS[i]).append(" → ").append(TextManager.getString(getLocale(), TextManager.COMMANDS, string)).append("\n");
            emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], string));
            i++;
        }

        categoriesSB.append("\n").append(getString("sp")).append("\n").append(Settings.EMPTY_EMOJI);
        eb.setDescription(categoriesSB.toString());

        eb
                .addField(getString("links_title"), getString("links_content",
                        Settings.LAWLIET_WEBSITE,
                        Settings.SERVER_INVITE_URL,
                        Settings.BOT_INVITE_URL,
                        Settings.UPVOTE_URL,
                        Settings.PATREON_PAGE
                ), true);
                //.addField(getString("giveaway_title"), getString("giveaway_desc", Settings.SERVER_INVITE_URL), false);
        return eb;
    }

}
