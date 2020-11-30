package commands.runnables.informationcategory;

import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import commands.listeners.OnNavigationListener;
import commands.listeners.OnTrackerRequestListener;
import commands.runnables.PornPredefinedAbstract;
import commands.runnables.PornSearchAbstract;
import constants.*;
import core.*;
import core.emojiconnection.BackEmojiConnection;
import core.emojiconnection.EmojiConnection;
import core.utils.EmbedUtil;
import core.utils.PermissionUtil;
import core.utils.StringUtil;
import mysql.modules.commandmanagement.CommandManagementBean;
import mysql.modules.commandmanagement.DBCommandManagement;
import org.javacord.api.DiscordApi;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

@CommandProperties(
        trigger = "help",
        emoji = "❕",
        executableWithoutArgs = true,
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
        searchTerm = inputString;
        return Response.FALSE;
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
                    command.setReactionUserID(event.getUserId());
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
        if (arg.startsWith("<") && arg.endsWith(">")) arg = arg.substring(1, arg.length()-1);

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
        return Category.LIST.length;
    }

    private EmbedBuilder checkCommand(ServerTextChannel channel, String arg) throws Throwable {
        boolean noArgs = false;
        if (getAttachments().containsKey("noargs")) {
            getAttachments().remove("noargs");
            noArgs = true;
        }

        for (Class<? extends Command> clazz : CommandContainer.getInstance().getFullCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();

            if ((commandTrigger.equalsIgnoreCase(arg) || Arrays.stream(command.getAliases()).anyMatch(arg::equalsIgnoreCase)) &&
                    !commandTrigger.equals(getTrigger())
            ) {
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
                if (!command.isExecutableWithoutArgs()) {
                    addNotExecutable = "\n" + getString("command_notexecutable");
                } else if (!isNavigationPrivateMessage()) {
                    setOptions(getString("command_execute").split("\n"));
                    emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[0],"exec:"+command.getClass().getName()));
                }

                String permissionsList = new ListGen<Integer>().getList(PermissionUtil.permissionsToNumberList(command.getUserPermissions()), getLocale(), ListGen.SLOT_TYPE_BULLET,
                        i -> TextManager.getString(getLocale(), TextManager.PERMISSIONS, String.valueOf(i))
                );

                EmbedBuilder eb =  EmbedFactory.getEmbedDefault()
                        .setTitle(
                                TextManager.getString(getLocale(), TextManager.COMMANDS, "categories") + " » " +
                                        TextManager.getString(getLocale(), TextManager.COMMANDS, command.getCategory()) + " » " +
                                        command.getEmoji()+" "+TextManager.getString(getLocale(), command.getCategory(),commandTrigger+"_title")
                        )
                        .setDescription(TextManager.getString(getLocale(), command.getCategory(),commandTrigger+"_helptext") + addNotExecutable)
                        .addField(Emojis.EMPTY_EMOJI, getString("command_usage") + "\n" + usage.toString(),true)
                        .addField(Emojis.EMPTY_EMOJI, getString( "command_example", exampleNumber > 1) + "\n" + examples.toString(),true);
                EmbedUtil.setFooter(eb, this, getString("command_args"));

                if (command.getUserPermissions() != 0)
                    eb.addField(Emojis.EMPTY_EMOJI, getString("command_userpermissions") + "\n" + permissionsList,false);
                if (noArgs)
                    EmbedUtil.addLog(eb, LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args"));

                return eb;
            }
        }
        return null;
    }

    private EmbedBuilder checkCategory(ServerTextChannel channel, String arg) throws Throwable {
        if (arg.length() > 0) {
            for (String category : Category.LIST) {
                if ((category.toLowerCase().contains(arg.toLowerCase()) || TextManager.getString(getLocale(), TextManager.COMMANDS, category).toLowerCase().contains(arg.toLowerCase()))) {
                    EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                            .setTitle(
                                    TextManager.getString(getLocale(), TextManager.COMMANDS, "categories") + " » " +
                                            TextManager.getString(getLocale(), TextManager.COMMANDS, category)
                            );
                    EmbedUtil.setFooter(eb, this, TextManager.getString(getLocale(), TextManager.GENERAL, "reaction_navigation"));

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
                        .append(commandTrigger)
                        .append("`");

                if (command.isNsfw())
                    stringBuilder.append(generateCommandIcons(command, true));
                stringBuilder.append("\n");

                i++;
                if (i >= 10) {
                    if (stringBuilder.length() > 0) eb.addField(Emojis.EMPTY_EMOJI, stringBuilder.toString(), true);
                    stringBuilder = new StringBuilder();
                    i = 0;
                }
            }
        }
        if (stringBuilder.length() > 0)
            eb.addField(Emojis.EMPTY_EMOJI, stringBuilder.toString(), true);

        addIconDescriptions(eb, category.equals(Category.INTERACTIONS));
        if (category.equals(Category.INTERACTIONS)) {
            eb.setDescription(getString("interactions_desc"));
        } else {
            eb.setDescription(getString("emotes_desc"));
        }

    }

    private void categoryDefault(EmbedBuilder eb, String category) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandCategoryMap().get(category)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();
            if (!commandTrigger.equals(getTrigger()) &&
                    (commandManagementBean.commandIsTurnedOn(command) || PermissionUtil.hasAdminPermissions(authorEvent.getServer().get(), authorEvent.getMessage().getUserAuthor().get()))
            ) {
                StringBuilder title = new StringBuilder();
                title.append(command.getEmoji())
                        .append(" `")
                        .append(getPrefix())
                        .append(commandTrigger)
                        .append("`");

                if (command.getReleaseDate().isAfter(LocalDate.now()))
                    title.append(" ").append(getString("beta"));
                title.append(generateCommandIcons(command, true));

                emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], command.getTrigger()));
                i++;
                eb.addField(
                        title.toString(),
                        TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_description") + "\n" + Emojis.EMPTY_EMOJI,
                        true
                );
            }
        }

        addIconDescriptions(eb, false);
    }

    private void categoryNSFW(EmbedBuilder eb) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        eb.setDescription(getString("nsfw"));

        StringBuilder withSearchKey = new StringBuilder();
        StringBuilder withoutSearchKey = new StringBuilder();

        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandCategoryMap().get(Category.NSFW)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());

            if (commandManagementBean.commandIsTurnedOn(command) ||
                    PermissionUtil.hasAdminPermissions(authorEvent.getServer().get(), authorEvent.getMessage().getUserAuthor().get())
            ) {
                String title = TextManager.getString(getLocale(), command.getCategory(), command.getTrigger() + "_title");

                StringBuilder extras = new StringBuilder(generateCommandIcons(command, false));
                if (command instanceof PornSearchAbstract)
                    withSearchKey.append(getString("nsfw_slot", command.getTrigger(), extras.toString(), title)).append("\n");
                else if (command instanceof PornPredefinedAbstract)
                    withoutSearchKey.append(getString("nsfw_slot", command.getTrigger(), extras.toString(), title)).append("\n");
            }
        }

        if (withSearchKey.length() > 0) {
            withSearchKey.append("\n\n").append(getString("nsfw_searchkey_on_eg"));
            eb.addField(getString("nsfw_searchkey_on"), withSearchKey.toString(), true);
        }

        if (withoutSearchKey.length() > 0) {
            eb.addField(getString("nsfw_searchkey_off"), withoutSearchKey.toString(), true);
        }

        addIconDescriptions(eb, false);
    }

    private String generateCommandIcons(Command command, boolean includeNsfw) {
        StringBuilder sb = new StringBuilder();

        if (command.isModCommand()) sb.append(CommandIcon.LOCKED);
        if (command instanceof OnTrackerRequestListener) sb.append(CommandIcon.ALERTS);
        if (includeNsfw && command.isNsfw()) sb.append(CommandIcon.NSFW);
        if (command.isPatreonRequired()) sb.append(CommandIcon.PATREON);

        return sb.length() == 0 ? "" : "┊" + sb.toString();
    }

    private void addIconDescriptions(EmbedBuilder eb, boolean showNsfwInfo) {
        String str = getString("commandproperties", showNsfwInfo,
                CommandIcon.LOCKED.toString(),
                CommandIcon.ALERTS.toString(),
                CommandIcon.NSFW.toString(),
                CommandIcon.PATREON.toString(),
                ExternalLinks.PATREON_PAGE
        );
        eb.addField(Emojis.EMPTY_EMOJI, str);
    }

    private EmbedBuilder checkMainPage(ServerTextChannel channel) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(TextManager.getString(getLocale(), TextManager.COMMANDS, "categories"));
        EmbedUtil.setFooter(eb, this);

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

        categoriesSB.append("\n").append(getString("sp")).append("\n").append(Emojis.EMPTY_EMOJI);
        eb.setDescription(categoriesSB.toString());

        if (Bot.isPublicVersion()) {
            eb.addField(getString("links_title"), getString(
                    "links_content",
                    ExternalLinks.LAWLIET_WEBSITE,
                    ExternalLinks.SERVER_INVITE_URL,
                    ExternalLinks.BOT_INVITE_URL,
                    ExternalLinks.UPVOTE_URL,
                    ExternalLinks.PATREON_PAGE,
                    ExternalLinks.FEATURE_REQUESTS_WEBSITE
            ), true);
        }
        return eb;
    }


    private static class CommandIcon {

        public static CommandIcon LOCKED = new CommandIcon(652188097911717910L);
        public static CommandIcon ALERTS = new CommandIcon(654051035249115147L);
        public static CommandIcon NSFW = new CommandIcon(652188472295292998L);
        public static CommandIcon PATREON = new CommandIcon(703937256070709258L);

        private final long emojiId;

        public CommandIcon(long emojiId) {
            this.emojiId = emojiId;
        }

        @Override
        public String toString() {
            return DiscordApiCollection.getInstance().getHomeEmojiById(emojiId).getMentionTag();
        }

    }

}
