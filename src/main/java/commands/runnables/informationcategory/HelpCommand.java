package commands.runnables.informationcategory;

import commands.Command;
import commands.CommandContainer;
import commands.CommandManager;
import commands.listeners.CommandProperties;
import commands.listeners.OnNavigationListenerOld;
import commands.listeners.OnTrackerRequestListener;
import commands.runnables.PornPredefinedAbstract;
import commands.runnables.PornSearchAbstract;
import constants.*;
import core.*;
import core.emojiconnection.BackEmojiConnection;
import core.emojiconnection.EmojiConnection;
import core.utils.EmbedUtil;
import core.utils.BotPermissionUtil;
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
public class HelpCommand extends Command implements OnNavigationListenerOld {

    String[] LIST = new String[]{ Category.GIMMICKS, Category.AI_TOYS, Category.CONFIGURATION, Category.UTILITY, Category.MODERATION, Category.INFORMATION, Category.FISHERY_SETTINGS, Category.FISHERY, Category.CASINO, Category.EMOTES, Category.INTERACTIONS, Category.EXTERNAL, Category.NSFW, Category.PATREON_ONLY, Category.SPLATOON_2 };

    private ArrayList<EmojiConnection> emojiConnections = new ArrayList<>();
    private String searchTerm;
    private MessageCreateEvent authorEvent;
    private CommandManagementBean commandManagementBean;
    private String currentCategory = null;

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
        if (emojiConnections.stream().anyMatch(c -> c.getConnection().equalsIgnoreCase(inputString))) {
            searchTerm = inputString;
            return Response.TRUE;
        }
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

        String arg = searchTerm.trim();
        if (arg.startsWith("<") && arg.endsWith(">")) arg = arg.substring(1, arg.length()-1);

        ServerTextChannel channel = getStarterMessage().getServerTextChannel().get();
        setOptions(null);

        EmbedBuilder eb;
        if ((eb = checkCommand(channel, arg)) == null) {
            if ((eb = checkCategory(channel,arg)) == null) {
                eb = checkMainPage(channel);
                if (arg.length() > 0)
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), arg));
            }
        }

        return eb;
    }

    @Override
    public void onNavigationTimeOut(Message message) {}

    @Override
    public int getMaxReactionNumber() {
        return LIST.length;
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
                if (currentCategory == null)
                    currentCategory = command.getCategory();

                emojiConnections = new ArrayList<>();
                emojiConnections.add(new BackEmojiConnection(channel.canYouUseExternalEmojis() || isNavigationPrivateMessage(), currentCategory));

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

                String permissionsList = new ListGen<Integer>().getList(
                        BotPermissionUtil.permissionsToNumberList(command.getUserPermissions()), getLocale(), ListGen.SLOT_TYPE_BULLET,
                        i -> TextManager.getString(getLocale(), TextManager.PERMISSIONS, String.valueOf(i))
                );

                EmbedBuilder eb =  EmbedFactory.getEmbedDefault()
                        .setTitle(
                                TextManager.getString(getLocale(), TextManager.COMMANDS, "categories") + " » " +
                                        TextManager.getString(getLocale(), TextManager.COMMANDS, currentCategory) + " » " +
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
            for (String category : LIST) {
                if ((category.toLowerCase().contains(arg.toLowerCase()) || TextManager.getString(getLocale(), TextManager.COMMANDS, category).toLowerCase().contains(arg.toLowerCase()))) {
                    currentCategory = category;

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

                        case Category.PATREON_ONLY:
                            categoryPatreon(eb);
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
                    BotPermissionUtil.hasAdminPermissions(authorEvent.getServer().get(), authorEvent.getMessage().getUserAuthor().get())
            ) {
                stringBuilder
                        .append("• `")
                        .append(command.getEmoji())
                        .append("⠀")
                        .append(getPrefix())
                        .append(commandTrigger)
                        .append("`");

                if (command.isNsfw())
                    stringBuilder.append(generateCommandIcons(command, true, true));
                stringBuilder.append("\n");

                emojiConnections.add(new EmojiConnection("", command.getTrigger()));
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

        addIconDescriptions(eb, false, false, category.equals(Category.INTERACTIONS), false);
        if (category.equals(Category.INTERACTIONS)) {
            eb.setDescription(getString("interactions_desc"));
        } else {
            eb.setDescription(getString("emotes_desc"));
        }

    }

    private void categoryPatreon(EmbedBuilder eb) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        boolean includeLocked = false;
        boolean includeAlerts = false;
        boolean includeNSFW = false;

        int i = 0;
        for(String category : Category.LIST) {
            for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandCategoryMap().get(category)) {
                Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
                String commandTrigger = command.getTrigger();
                if (command.isPatreonRequired() &&
                        !commandTrigger.equals(getTrigger()) &&
                        (commandManagementBean.commandIsTurnedOn(command) || BotPermissionUtil.hasAdminPermissions(authorEvent.getServer().get(), authorEvent.getMessage().getUserAuthor().get()))
                ) {
                    StringBuilder title = new StringBuilder();
                    title.append(command.getEmoji())
                            .append(" `")
                            .append(getPrefix())
                            .append(commandTrigger)
                            .append("`");

                    if (command.getReleaseDate().orElse(LocalDate.now()).isAfter(LocalDate.now()))
                        title.append(" ").append(getString("beta"));
                    title.append(generateCommandIcons(command, true, false));

                    if (command.isModCommand()) includeLocked = true;
                    if (command instanceof OnTrackerRequestListener) includeAlerts = true;
                    if (command.isNsfw()) includeNSFW = true;

                    emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], command.getTrigger()));
                    i++;
                    eb.addField(
                            title.toString(),
                            TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_description") + "\n" + Emojis.EMPTY_EMOJI,
                            true
                    );
                }
            }
        }

        eb.setDescription(getString("premium", ExternalLinks.PATREON_PAGE) + "\n" + Emojis.EMPTY_EMOJI);
        addIconDescriptions(eb, includeLocked, includeAlerts, includeNSFW, false);
    }

    private void categoryDefault(EmbedBuilder eb, String category) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        boolean includeLocked = false;
        boolean includeAlerts = false;
        boolean includeNSFW = false;
        boolean includePatreon = false;

        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandCategoryMap().get(category)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();
            if (!commandTrigger.equals(getTrigger()) &&
                    (commandManagementBean.commandIsTurnedOn(command) || BotPermissionUtil.hasAdminPermissions(authorEvent.getServer().get(), authorEvent.getMessage().getUserAuthor().get()))
            ) {
                StringBuilder title = new StringBuilder();
                title.append(command.getEmoji())
                        .append(" `")
                        .append(getPrefix())
                        .append(commandTrigger)
                        .append("`");

                if (command.getReleaseDate().orElse(LocalDate.now()).isAfter(LocalDate.now()))
                    title.append(" ").append(getString("beta"));
                title.append(generateCommandIcons(command, true, true));

                if (command.isModCommand()) includeLocked = true;
                if (command instanceof OnTrackerRequestListener) includeAlerts = true;
                if (command.isNsfw()) includeNSFW = true;
                if (command.isPatreonRequired()) includePatreon = true;

                emojiConnections.add(new EmojiConnection(LetterEmojis.LETTERS[i], command.getTrigger()));
                i++;
                eb.addField(
                        title.toString(),
                        TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_description") + "\n" + Emojis.EMPTY_EMOJI,
                        true
                );
            }
        }

        addIconDescriptions(eb, includeLocked, includeAlerts, includeNSFW, includePatreon);
    }

    private void categoryNSFW(EmbedBuilder eb) throws InstantiationException, IllegalAccessException, InvocationTargetException {
        eb.setDescription(getString("nsfw"));

        StringBuilder withSearchKey = new StringBuilder();
        StringBuilder withoutSearchKey = new StringBuilder();

        for (Class<? extends Command> clazz : CommandContainer.getInstance().getCommandCategoryMap().get(Category.NSFW)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());

            if (commandManagementBean.commandIsTurnedOn(command) ||
                    BotPermissionUtil.hasAdminPermissions(authorEvent.getServer().get(), authorEvent.getMessage().getUserAuthor().get())
            ) {
                String title = TextManager.getString(getLocale(), command.getCategory(), command.getTrigger() + "_title");

                StringBuilder extras = new StringBuilder(generateCommandIcons(command, false, true));
                if (command instanceof PornSearchAbstract)
                    withSearchKey.append(getString("nsfw_slot", command.getTrigger(), extras.toString(), title)).append("\n");
                else if (command instanceof PornPredefinedAbstract)
                    withoutSearchKey.append(getString("nsfw_slot", command.getTrigger(), extras.toString(), title)).append("\n");

                emojiConnections.add(new EmojiConnection("", command.getTrigger()));
            }
        }

        if (withSearchKey.length() > 0) {
            withSearchKey.append("\n\n").append(getString("nsfw_searchkey_on_eg"));
            eb.addField(getString("nsfw_searchkey_on"), withSearchKey.toString(), true);
        }

        if (withoutSearchKey.length() > 0) {
            eb.addField(getString("nsfw_searchkey_off"), withoutSearchKey.toString(), true);
        }

        addIconDescriptions(eb, false, true, false, true);
    }

    private String generateCommandIcons(Command command, boolean includeNsfw, boolean includePatreon) {
        StringBuilder sb = new StringBuilder();

        if (command.isModCommand()) sb.append(CommandIcon.LOCKED);
        if (command instanceof OnTrackerRequestListener) sb.append(CommandIcon.ALERTS);
        if (includeNsfw && command.isNsfw()) sb.append(CommandIcon.NSFW);
        if (includePatreon && command.isPatreonRequired()) sb.append(CommandIcon.PATREON);

        return sb.length() == 0 ? "" : "┊" + sb.toString();
    }

    private void addIconDescriptions(EmbedBuilder eb, boolean includeLocked, boolean includeAlerts, boolean includeNSFW, boolean includePatreon) {
        if (!isNavigationPrivateMessage()) {
            StringBuilder sb = new StringBuilder(getString("commandproperties")).append("\n\n");
            if (includeLocked) sb.append(getString("commandproperties_LOCKED", CommandIcon.LOCKED.toString())).append("\n");
            if (includeAlerts) sb.append(getString("commandproperties_ALERTS", CommandIcon.ALERTS.toString())).append("\n");
            if (includeNSFW) sb.append(getString("commandproperties_NSFW", CommandIcon.NSFW.toString())).append("\n");
            if (includePatreon) sb.append(getString("commandproperties_PATREON", CommandIcon.PATREON.toString(), ExternalLinks.PATREON_PAGE)).append("\n");

            eb.addField(Emojis.EMPTY_EMOJI, sb.toString());
        }
    }

    private EmbedBuilder checkMainPage(ServerTextChannel channel) throws Throwable {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(TextManager.getString(getLocale(), TextManager.COMMANDS, "categories"));
        EmbedUtil.setFooter(eb, this);

        StringBuilder categoriesSB = new StringBuilder();
        emojiConnections = new ArrayList<>();
        emojiConnections.add(new BackEmojiConnection(channel.canYouUseExternalEmojis() || isNavigationPrivateMessage(), "quit"));


        int i = 0;
        for (String string : LIST) {
            if (!commandManagementBean.getSwitchedOffElements().contains(string) || BotPermissionUtil.hasAdminPermissions(authorEvent.getServer().get(), authorEvent.getMessage().getUserAuthor().get())) {
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

        public static CommandIcon LOCKED = new CommandIcon(Emojis.COMMAND_ICON_LOCKED);
        public static CommandIcon ALERTS = new CommandIcon(Emojis.COMMAND_ICON_ALERTS);
        public static CommandIcon NSFW = new CommandIcon(Emojis.COMMAND_ICON_NSFW);
        public static CommandIcon PATREON = new CommandIcon(Emojis.COMMAND_ICON_PATREON);

        private final String emojiTag;

        public CommandIcon(String emojiTag) {
            this.emojiTag = emojiTag;
        }

        @Override
        public String toString() {
            return emojiTag;
        }

    }

}
