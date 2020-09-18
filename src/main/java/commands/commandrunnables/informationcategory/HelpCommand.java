package commands.commandrunnables.informationcategory;

import commands.commandlisteners.CommandProperties;
import commands.commandlisteners.OnNavigationListener;
import commands.commandlisteners.OnTrackerRequestListener;
import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.commandrunnables.PornPredefinedAbstract;
import commands.commandrunnables.PornSearchAbstract;
import constants.*;
import core.*;
import core.emojiconnection.BackEmojiConnection;
import core.emojiconnection.EmojiConnection;
import core.utils.PermissionUtil;
import core.utils.StringUtil;
import mysql.modules.commandmanagement.CommandManagementBean;
import mysql.modules.commandmanagement.DBCommandManagement;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Locale;

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

    public HelpCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    protected boolean onMessageReceived(MessageCreateEvent event, String followedString) throws Throwable {
        searchTerm = followedString;
        authorEvent = event;
        commandManagementBean = DBCommandManagement.getInstance().getBean(event.getServer().get().getId());
        return true;
    }

    @ControllerMessage(state = DEFAULT_STATE)
    public Response onMessage(MessageCreateEvent event, String inputString) throws Throwable {
        return null;
    }

    @ControllerReaction(state = DEFAULT_STATE)
    public boolean onReaction(SingleReactionEvent event, int i) throws Throwable {
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

                    CommandManager.manage(authorEvent, command, "", Instant.now());

                    return false;
                }

                return true;
            }
        }

        return false;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDraw(DiscordApi api) throws Throwable {
        String arg = StringUtil.trimString(searchTerm);
        if (arg.startsWith("<") && arg.endsWith(">")) arg = arg.substring(1,arg.length()-1);

        ServerTextChannel channel = getStarterMessage().getServerTextChannel().get();

        setOptions(null);

        EmbedBuilder eb;
        if ((eb = checkCommand(channel, arg)) == null) {
            if ((eb = checkCategory(channel,arg)) == null) {
                eb = checkMainPage(channel);
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
        for (Class<? extends Command> clazz : CommandContainer.getInstance().getFullCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();

            if (commandTrigger.equalsIgnoreCase(arg) && !commandTrigger.equals(getTrigger())) {
                emojiConnections = new ArrayList<>();
                emojiConnections.add(new BackEmojiConnection(channel.canYouUseExternalEmojis() || isNavigationPrivateMessage(), command.getCategory()));

                StringBuilder usage = new StringBuilder();
                for(String line: TextManager.getString(getLocale(), command.getCategory(),commandTrigger+"_usage").split("\n")) {
                    usage.append("• ").append(getPrefix()).append(commandTrigger).append(" ").append(line).append("\n");
                }

                StringBuilder examples = new StringBuilder();
                int exampleNumber = 0;
                for(String line: TextManager.getString(getLocale(), command.getCategory(),commandTrigger+"_examples").split("\n")) {
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

                String permissionsList = new ListGen<Integer>().getList(PermissionUtil.permissionsToNumberList(command.getUserPermissions()), getLocale(), ListGen.SLOT_TYPE_BULLET,
                        i -> TextManager.getString(getLocale(), TextManager.PERMISSIONS, String.valueOf(i))
                );

                EmbedBuilder eb =  EmbedFactory.getEmbed()
                        .setTitle(
                                TextManager.getString(getLocale(), TextManager.COMMANDS, "categories") + " » " +
                                        TextManager.getString(getLocale(), TextManager.COMMANDS, command.getCategory()) + " » " +
                                        command.getEmoji()+" "+TextManager.getString(getLocale(), command.getCategory(),commandTrigger+"_title")
                        )
                        .setFooter(getString("command_args"))
                        .setDescription(TextManager.getString(getLocale(), command.getCategory(),commandTrigger+"_helptext") + addNotExecutable)
                        .addField(Settings.EMPTY_EMOJI, getString("command_usage") + "\n" + usage.toString(),true)
                        .addField(Settings.EMPTY_EMOJI, getString( "command_example", exampleNumber > 1) + "\n" + examples.toString(),true);

                if (command.getUserPermissions() != 0)
                    eb.addField(Settings.EMPTY_EMOJI, getString("command_userpermissions") + "\n" + permissionsList,false);

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
                        case Category.INTERACTIONS:
                            categoryInteractionsEmotes(eb, Category.INTERACTIONS);
                            break;

                        case Category.EMOTES:
                            categoryInteractionsEmotes(eb, Category.EMOTES);
                            break;

                        case Category.NSFW:
                            categoryNSFW(eb);
                            break;

                        default:
                            categoryDefault(eb, category);
                    }

                    return eb;
                }
            }
        }

        return null;
    }

    private void categoryInteractionsEmotes(EmbedBuilder eb, String category) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        eb.setDescription(getString("emotes_desc"));

        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandCategoryMap().get(category)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();
            if (commandManagementBean.commandIsTurnedOn(command) ||
                    PermissionUtil.hasAdminPermissions(authorEvent.getServer().get(), authorEvent.getMessage().getUserAuthor().get())
            ) {
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

    private void categoryDefault(EmbedBuilder eb, String category) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandCategoryMap().get(category)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();
            User author = getStarterMessage().getUserAuthor().get();
            if (!commandTrigger.equals(getTrigger()) &&
                    (commandManagementBean.commandIsTurnedOn(command) || PermissionUtil.hasAdminPermissions(authorEvent.getServer().get(), authorEvent.getMessage().getUserAuthor().get()))
            ) {
                StringBuilder commands = new StringBuilder();
                boolean canAccess = PermissionUtil.getMissingPermissionListForUser(authorEvent.getServer().get(), authorEvent.getServerTextChannel().get(), author, command.getUserPermissions()).size() == 0 &&
                        (!command.isNsfw() || authorEvent.getServerTextChannel().get().isNsfw()) &&
                        !command.isPatreonRequired() || PatreonCache.getInstance().getPatreonLevel(author.getId()) > 0;

                commands.append("**")
                        .append(LetterEmojis.LETTERS[i])
                        .append(" → ")
                        .append(command.getEmoji())
                        .append(" ");

                if (!canAccess) commands.append("~~");

                commands.append(TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_title").toUpperCase());

                if (!canAccess) commands.append("~~");
                if (command.isModCommand()) commands.append(Settings.EMPTY_EMOJI).append(DiscordApiCollection.getInstance().getHomeEmojiById(652188097911717910L).getMentionTag());
                if (command instanceof OnTrackerRequestListener) commands.append(Settings.EMPTY_EMOJI).append(DiscordApiCollection.getInstance().getHomeEmojiById(654051035249115147L).getMentionTag());
                if (command.isNsfw()) commands.append(Settings.EMPTY_EMOJI).append(DiscordApiCollection.getInstance().getHomeEmojiById(652188472295292998L).getMentionTag());
                if (command.isPatreonRequired()) commands.append(Settings.EMPTY_EMOJI).append(DiscordApiCollection.getInstance().getHomeEmojiById(703937256070709258L).getMentionTag());

                commands.append("**\n").append("`").append(getPrefix()).append(commandTrigger).append("`")
                        .append(" - ")
                        .append(TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_description"))
                        .append("\n\n");
                emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], command.getTrigger()));
                i++;

                eb.addField(Settings.EMPTY_EMOJI, commands.toString());
            }
        }

        eb.addField(Settings.EMPTY_EMOJI, getIconDescriptions());
    }

    private void categoryNSFW(EmbedBuilder eb) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        eb.setDescription(getString("nsfw"));
        String patreonIcon = DiscordApiCollection.getInstance().getHomeEmojiById(703937256070709258L).getMentionTag();

        StringBuilder withSearchKey = new StringBuilder();
        StringBuilder withoutSearchKey = new StringBuilder();

        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandCategoryMap().get(Category.NSFW)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());

            if (commandManagementBean.commandIsTurnedOn(command) ||
                    PermissionUtil.hasAdminPermissions(authorEvent.getServer().get(), authorEvent.getMessage().getUserAuthor().get())
            ) {
                String title = TextManager.getString(getLocale(), command.getCategory(), command.getTrigger() + "_title");

                if (command instanceof PornSearchAbstract)
                    withSearchKey.append(getString("nsfw_slot", command.isPatreonRequired(), command.getTrigger(), patreonIcon, title)).append("\n");
                else if (command instanceof PornPredefinedAbstract)
                    withoutSearchKey.append(getString("nsfw_slot", command.isPatreonRequired(), command.getTrigger(), patreonIcon, title)).append("\n");
            }
        }

        if (withSearchKey.length() > 0) {
            withSearchKey.append("\n\n").append(getString("nsfw_searchkey_on_eg"));
            eb.addField(getString("nsfw_searchkey_on"), withSearchKey.toString(), true);
        }

        if (withoutSearchKey.length() > 0) {
            eb.addField(getString("nsfw_searchkey_off"), withoutSearchKey.toString(), true);
        }

        eb.addField(Settings.EMPTY_EMOJI, getIconDescriptions());
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

    private EmbedBuilder checkMainPage(ServerTextChannel channel) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getEmbed()
                .setTitle(TextManager.getString(getLocale(), TextManager.COMMANDS, "categories"));

        StringBuilder categoriesSB = new StringBuilder();
        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(channel.canYouUseExternalEmojis() || isNavigationPrivateMessage(), "quit"));


        int i = 0;
        for (String string : Category.LIST) {
            if (!commandManagementBean.getSwitchedOffElements().contains(string) || PermissionUtil.hasAdminPermissions(authorEvent.getServer().get(), authorEvent.getMessage().getUserAuthor().get())) {
                categoriesSB.append(LetterEmojis.LETTERS[i]).append(" → ").append(TextManager.getString(getLocale(), TextManager.COMMANDS, string)).append("\n");
                emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], string));
                i++;
            }
        }

        categoriesSB.append("\n").append(getString("sp")).append("\n").append(Settings.EMPTY_EMOJI);
        eb.setDescription(categoriesSB.toString());

        eb
                .addField(getString("links_title"), getString("links_content",
                        Settings.LAWLIET_WEBSITE,
                        Settings.SERVER_INVITE_URL,
                        Settings.BOT_INVITE_URL,
                        Settings.UPVOTE_URL,
                        Settings.PATREON_PAGE,
                        Settings.FEATURE_REQUESTS_WEBSITE
                ), true);
        if (Settings.GIVEAWAY_RUNNING) eb.addField(getString("giveaway_title"), getString("giveaway_desc", Settings.SERVER_INVITE_URL), false);
        return eb;
    }

}
