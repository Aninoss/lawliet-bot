package commands.runnables.informationcategory;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import commands.*;
import commands.listeners.CommandProperties;
import commands.listeners.MessageInputResponse;
import commands.listeners.OnAlertListener;
import commands.runnables.*;
import constants.Emojis;
import constants.ExternalLinks;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.Program;
import core.TextManager;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.StringUtil;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import org.jetbrains.annotations.NotNull;
import org.json.JSONObject;

@CommandProperties(
        trigger = "help",
        emoji = "❕",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = { "commands" }
)
public class HelpCommand extends NavigationAbstract {

    private final HashMap<Integer, String> buttonMap = new HashMap<>();
    private String searchTerm;
    private Category currentCategory = null;

    public HelpCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        searchTerm = args;
        registerNavigationListener(event.getMember());
        return true;
    }

    @ControllerMessage(state = DEFAULT_STATE)
    public MessageInputResponse onMessage(MessageReceivedEvent event, String input) {
        if (input.length() > 0 && buttonMap.values().stream().anyMatch(str -> str.equalsIgnoreCase(input))) {
            searchTerm = input;
            return MessageInputResponse.SUCCESS;
        }
        return null;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButton(ButtonInteractionEvent event, int i) {
        String key = buttonMap.get(i);
        if (key != null) {
            searchTerm = key;

            if (searchTerm.equals("quit")) {
                deregisterListenersWithComponentMessage();
                return false;
            }

            if (searchTerm.startsWith("exec:")) {
                String className = searchTerm.split(":")[1];
                Command command = CommandManager.createCommandByClassName(className, getLocale(), getPrefix());

                CommandManager.manage(getCommandEvent(), command, "", getGuildEntity(), Instant.now(), false);
                return false;
            }

            return true;
        }

        return false;
    }

    @ControllerSelectMenu(state = DEFAULT_STATE)
    public boolean onSelectMenu(StringSelectInteractionEvent event, int i) throws Throwable {
        searchTerm = event.getValues().get(0);
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder onDraw(Member member) {
        String arg = searchTerm.trim();
        if (arg.startsWith("<") && arg.endsWith(">")) arg = arg.substring(1, arg.length() - 1);

        TextChannel channel = getTextChannel().get();
        setActionRows();

        EmbedBuilder eb;
        if ((eb = checkCommand(member, channel, arg)) == null) {
            if ((eb = checkCategory(member, channel, arg)) == null) {
                eb = checkMainPage(member, channel);
                if (arg.length() > 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), arg));
                }
            }
        }

        JSONObject attachments = getAttachments();
        if (attachments.has("error")) {
            setLog(LogStatus.FAILURE, attachments.getString("error"));
            attachments.remove("error");
        }
        return eb;
    }

    private EmbedBuilder checkCommand(Member member, TextChannel channel, String arg) {
        boolean noArgs = false;
        if (getAttachments().has("noargs")) {
            getAttachments().remove("noargs");
            noArgs = true;
        }

        for (Class<? extends Command> clazz : CommandContainer.getFullCommandList()) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();

            if ((commandTrigger.equalsIgnoreCase(arg) || Arrays.stream(command.getCommandProperties().aliases()).anyMatch(arg::equalsIgnoreCase)) &&
                    !commandTrigger.equals(getTrigger())
            ) {
                if (currentCategory == null) {
                    currentCategory = command.getCategory();
                }

                buttonMap.clear();
                if (command.getCommandProperties().nsfw() && !channel.isNSFW()) {
                    buttonMap.put(-1, "");
                    return EmbedFactory.getNSFWBlockEmbed(this);
                }
                buttonMap.put(-1, "cat:" + currentCategory.getId());

                StringBuilder usage = new StringBuilder();
                for (String line : TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_usage").split("\n")) {
                    usage.append("• ").append(getPrefix()).append(commandTrigger).append(" ").append(line).append("\n");
                }

                StringBuilder examples = new StringBuilder();
                int exampleNumber = 0;
                for (String line : TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_examples").split("\n")) {
                    line = StringUtil.solveVariablesOfCommandText(line, getTextChannel().get(), member, getPrefix());
                    examples.append("• ").append(getPrefix()).append(commandTrigger).append(" ").append(line).append("\n");
                    exampleNumber++;
                }

                String addNotExecutable = "";
                if (command.getCommandProperties().executableWithoutArgs()) {
                    setComponents(getString("command_execute").split("\n"));
                    buttonMap.put(0, "exec:" + command.getClass().getName());
                }

                String permissionsList = new ListGen<Permission>().getList(
                        List.of(command.getUserPermissions()),
                        getLocale(),
                        ListGen.SLOT_TYPE_BULLET,
                        permission -> TextManager.getString(getLocale(), TextManager.PERMISSIONS, permission.name())
                );

                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(
                                TextManager.getString(getLocale(), TextManager.COMMANDS, currentCategory.getId()) + " » " +
                                        command.getCommandProperties().emoji() + " " + TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_title")
                        )
                        .setDescription(TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_helptext") + addNotExecutable)
                        .addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), getString("command_usage") + "\n" + usage, true)
                        .addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), getString("command_example", exampleNumber > 1) + "\n" + examples, true);
                EmbedUtil.setFooter(eb, this, getString("command_args"));

                if (command.getUserPermissions().length > 0) {
                    eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), getString("command_userpermissions") + "\n" + permissionsList, false);
                }
                if (noArgs) {
                    EmbedUtil.addLog(eb, LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "no_args"));
                }

                return eb;
            }
        }
        return null;
    }

    private EmbedBuilder checkCategory(Member member, TextChannel channel, String arg) {
        if (arg.startsWith("cat:")) {
            arg = arg.substring(4);
        }

        if (arg.length() > 0) {
            boolean halfMatchFound = false;
            Category category = null;
            for (Category value : Category.values()) {
                if ((value.getId().equalsIgnoreCase(arg) || TextManager.getString(getLocale(), TextManager.COMMANDS, value.getId()).equalsIgnoreCase(arg))) {
                    category = value;
                    break;
                } else if ((value.getId().toLowerCase().contains(arg.toLowerCase()) || TextManager.getString(getLocale(), TextManager.COMMANDS, value.getId()).toLowerCase().contains(arg.toLowerCase())) &&
                        !halfMatchFound
                ) {
                    category = value;
                    halfMatchFound = true;
                }
            }

            if (category != null) {
                currentCategory = category;
                buttonMap.clear();
                buttonMap.put(-1, "");

                if (category.isNSFW() && !channel.isNSFW()) {
                    return EmbedFactory.getNSFWBlockEmbed(this);
                }

                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(TextManager.getString(getLocale(), TextManager.COMMANDS, category.getId()));
                EmbedUtil.setFooter(eb, this, getString("navigation"));

                switch (category) {
                    case INTERACTIONS -> categoryRolePlay(member, eb);
                    case NSFW_INTERACTIONS -> categoryNSFWRolePlay(member, eb);
                    case NSFW -> categoryNSFW(member, channel, eb);
                    case PATREON_ONLY -> categoryPatreon(member, channel, eb);
                    default -> categoryDefault(member, channel, eb, category);
                }

                setComponents(generateSelectMenu(member, channel, category));
                return eb;
            }
        }

        return null;
    }

    private void categoryRolePlay(Member member, EmbedBuilder eb) {
        AtomicInteger counter = new AtomicInteger(0);
        addRolePlayCommandList(member, eb, Category.INTERACTIONS, command -> !command.isInteractive(), counter);
        eb.addBlankField(false);

        eb.addField(getString("roleplay_interactive_title"), getString("roleplay_interactive_desc"), false);
        addRolePlayCommandList(member, eb, Category.INTERACTIONS, RolePlayAbstract::isInteractive, counter);
    }

    private void categoryNSFWRolePlay(Member member, EmbedBuilder eb) {
        eb.setDescription(getString("interaction_nsfw_desc"));

        AtomicInteger counter = new AtomicInteger(0);
        eb.addField(getString("roleplay_interactive_title"), getString("roleplay_interactive_desc"), false);
        addRolePlayCommandList(member, eb, Category.NSFW_INTERACTIONS, command -> true, counter);
    }

    private void addRolePlayCommandList(Member member, EmbedBuilder eb, Category category, Function<RolePlayAbstract, Boolean> rolePlayAbstractFilter, AtomicInteger counter) {
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getCommandCategoryMap().get(category)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();
            if (rolePlayAbstractFilter.apply((RolePlayAbstract) command) && CommandManager.commandIsTurnedOnIgnoreAdmin(command, member, getTextChannel().get())) {
                buttonMap.put(counter.getAndIncrement(), command.getTrigger());
                stringBuilder
                        .append("• `")
                        .append(command.getCommandProperties().emoji())
                        .append("⠀")
                        .append(getPrefix())
                        .append(commandTrigger)
                        .append("`")
                        .append("\n");

                i++;
                if (i >= 10) {
                    if (stringBuilder.length() > 0) {
                        eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), stringBuilder.toString(), true);
                    }
                    stringBuilder = new StringBuilder();
                    i = 0;
                }
            }
        }
        if (stringBuilder.length() > 0) {
            eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), stringBuilder.toString(), true);
        }
    }

    private void categoryPatreon(Member member, TextChannel channel, EmbedBuilder eb) {
        boolean includeLocked = false;
        boolean includeAlerts = false;
        boolean includeNSFW = false;

        int i = 0;
        for (Category category : Category.independentValues()) {
            for (Class<? extends Command> clazz : CommandContainer.getCommandCategoryMap().get(category)) {
                Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
                String commandTrigger = command.getTrigger();
                if (command.getCommandProperties().patreonRequired() &&
                        !commandTrigger.equals(getTrigger()) &&
                        CommandManager.commandIsTurnedOnIgnoreAdmin(command, member, getTextChannel().get()) &&
                        (!command.getCommandProperties().nsfw() || channel.isNSFW())
                ) {
                    StringBuilder title = new StringBuilder();
                    title.append(command.getCommandProperties().emoji())
                            .append(" `")
                            .append(getPrefix())
                            .append(commandTrigger)
                            .append("`");

                    if (command.getReleaseDate().orElse(LocalDate.now()).isAfter(LocalDate.now())) {
                        title.append(" ").append(getString("beta"));
                    }
                    if (command.getCommandProperties().obsolete()) {
                        title.append(" ").append(getString("obsolete"));
                    }
                    title.append(generateCommandIcons(channel, command, true, true, false));

                    if (command.isModCommand()) includeLocked = true;
                    if (command instanceof OnAlertListener) includeAlerts = true;
                    if (command.getCommandProperties().nsfw()) includeNSFW = true;

                    buttonMap.put(i, command.getTrigger());
                    i++;
                    eb.addField(
                            title.toString(),
                            TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_description") + "\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted(),
                            true
                    );
                }
            }
        }

        eb.setDescription(getString("premium", ExternalLinks.PREMIUM_WEBSITE) + "\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted());
        addIconDescriptions(channel, eb, includeLocked, includeAlerts, includeNSFW, false);
    }

    private void categoryDefault(Member member, TextChannel channel, EmbedBuilder eb, Category category) {
        boolean includeLocked = false;
        boolean includeAlerts = false;
        boolean includeNSFW = false;
        boolean includePatreon = false;

        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getCommandCategoryMap().get(category)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();
            if (!commandTrigger.equals(getTrigger()) &&
                    CommandManager.commandIsTurnedOnIgnoreAdmin(command, member, getTextChannel().get())
            ) {
                StringBuilder title = new StringBuilder();
                title.append(command.getCommandProperties().emoji())
                        .append(" `")
                        .append(getPrefix())
                        .append(commandTrigger)
                        .append("`");

                if (command.getReleaseDate().orElse(LocalDate.now()).isAfter(LocalDate.now())) {
                    title.append(" ").append(getString("beta"));
                }
                if (command.getCommandProperties().obsolete()) {
                    title.append(" ").append(getString("obsolete"));
                }
                title.append(generateCommandIcons(channel, command, true, true, true));

                if (command.isModCommand()) includeLocked = true;
                if (command instanceof OnAlertListener) includeAlerts = true;
                if (command.getCommandProperties().nsfw()) includeNSFW = true;
                if (command.getCommandProperties().patreonRequired()) includePatreon = true;

                buttonMap.put(i, command.getTrigger());
                i++;
                eb.addField(
                        title.toString(),
                        TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_description") + "\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted(),
                        true
                );
            }
        }

        addIconDescriptions(channel, eb, includeLocked, includeAlerts, includeNSFW, includePatreon);
    }

    private void categoryNSFW(Member member, TextChannel channel, EmbedBuilder eb) {
        eb.setDescription(getString("nsfw"));

        StringBuilder withSearchKey = new StringBuilder();
        StringBuilder withoutSearchKeyHentai = new StringBuilder();
        StringBuilder withoutSearchKeyRealLife = new StringBuilder();

        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getCommandCategoryMap().get(Category.NSFW)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());

            if (CommandManager.commandIsTurnedOnIgnoreAdmin(command, member, getTextChannel().get())) {
                buttonMap.put(i++, command.getTrigger());
                String title = TextManager.getString(getLocale(), command.getCategory(), command.getTrigger() + "_title");

                StringBuilder extras = new StringBuilder(generateCommandIcons(channel, command, false, false, true));
                if (command instanceof PornSearchAbstract) {
                    withSearchKey.append(getString("nsfw_slot", command.getTrigger(), extras.toString(), title)).append("\n");
                } else if (command instanceof PornPredefinedAbstract) {
                    if (command instanceof RealbooruAbstract) {
                        withoutSearchKeyRealLife.append(getString("nsfw_slot", command.getTrigger(), extras.toString(), title)).append("\n");
                    } else {
                        withoutSearchKeyHentai.append(getString("nsfw_slot", command.getTrigger(), extras.toString(), title)).append("\n");
                    }
                }
            }
        }

        if (withSearchKey.length() > 0) {
            withSearchKey.append("\n").append(getString("nsfw_searchkey_on_eg")).append("\n").append(Emojis.ZERO_WIDTH_SPACE.getFormatted());
            eb.addField(getString("nsfw_searchkey_on"), withSearchKey.toString(), false);
        }
        if (withoutSearchKeyHentai.length() > 0) {
            eb.addField(getString("nsfw_searchkey_off_hentai"), withoutSearchKeyHentai.toString(), true);
        }
        if (withoutSearchKeyRealLife.length() > 0) {
            eb.addField(getString("nsfw_searchkey_off_rl"), withoutSearchKeyRealLife.toString(), true);
        }

        addIconDescriptions(channel, eb, false, false, false, true);
    }

    private String generateCommandIcons(TextChannel channel, Command command, boolean includeAlert, boolean includeNsfw, boolean includePatreon) {
        StringBuilder sb = new StringBuilder();

        if (command.isModCommand()) sb.append(CommandIcon.LOCKED.get(channel));
        if (includeAlert && command instanceof OnAlertListener) sb.append(CommandIcon.ALERTS.get(channel));
        if (includeNsfw && command.getCommandProperties().nsfw()) sb.append(CommandIcon.NSFW.get(channel));
        if (includePatreon && command.getCommandProperties().patreonRequired()) {
            sb.append(CommandIcon.PATREON.get(channel));
        }

        return sb.length() == 0 ? "" : "┊" + sb;
    }

    private void addIconDescriptions(TextChannel channel, EmbedBuilder eb, boolean includeLocked, boolean includeAlerts, boolean includeNSFW, boolean includePatreon) {
        StringBuilder sb = new StringBuilder(getString("commandproperties")).append("\n\n");
        if (includeLocked) {
            sb.append(getString("commandproperties_LOCKED", CommandIcon.LOCKED.get(channel))).append("\n");
        }
        if (includeAlerts) {
            sb.append(getString("commandproperties_ALERTS", CommandIcon.ALERTS.get(channel))).append("\n");
        }
        if (includeNSFW) sb.append(getString("commandproperties_NSFW", CommandIcon.NSFW.get(channel))).append("\n");
        if (includePatreon) {
            sb.append(getString("commandproperties_PATREON", CommandIcon.PATREON.get(channel), ExternalLinks.PREMIUM_WEBSITE)).append("\n");
        }

        eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), sb.toString(), false);
    }

    private EmbedBuilder checkMainPage(Member member, TextChannel channel) {
        String banner = Program.publicVersion()
                ? "https://cdn.discordapp.com/attachments/499629904380297226/850825690399899658/help_banner.png"
                : "https://cdn.discordapp.com/attachments/499629904380297226/1106609492256370779/help_banner_custom.png";

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(TextManager.getString(getLocale(), TextManager.COMMANDS, "categories"))
                .setDescription(getString("sp"))
                .setImage(banner);
        EmbedUtil.setFooter(eb, this, getString("navigation"));

        buttonMap.clear();
        buttonMap.put(-1, "quit");

        if (Program.publicVersion()) {
            eb.addField(getString("links_title"), getString(
                    Program.publicVersion() ? "links_content" : "help_links_content_notpublic",
                    ExternalLinks.LAWLIET_WEBSITE,
                    ExternalLinks.SERVER_INVITE_URL,
                    ExternalLinks.BOT_INVITE_URL,
                    ExternalLinks.UPVOTE_URL,
                    ExternalLinks.PREMIUM_WEBSITE,
                    ExternalLinks.FEATURE_REQUESTS_WEBSITE
            ), true);
        }

        setComponents(generateSelectMenu(member, channel, null));
        return eb;
    }

    private SelectMenu generateSelectMenu(Member member, TextChannel channel, Category currentCategory) {
        StringSelectMenu.Builder builder = StringSelectMenu.create("category")
                .setPlaceholder(getString("category_placeholder"));
        for (Category category : Category.values()) {
            if (CommandManager.categoryIsTurnedOnIgnoreAdmin(category, member, channel) &&
                    (!category.isNSFW() || channel.isNSFW())
            ) {
                String label = TextManager.getString(getLocale(), TextManager.COMMANDS, category.getId());
                String value = "cat:" + category.getId();
                builder.addOption(label, value, Emoji.fromUnicode(category.getEmoji()));
                if (category == currentCategory) {
                    builder.setDefaultValues(List.of(value));
                }
            }
        }
        return builder.build();
    }


    private static class CommandIcon {

        public static final CommandIcon LOCKED = new CommandIcon(Emojis.COMMAND_ICON_LOCKED, "¹");
        public static final CommandIcon ALERTS = new CommandIcon(Emojis.COMMAND_ICON_ALERTS, "²");
        public static final CommandIcon NSFW = new CommandIcon(Emojis.COMMAND_ICON_NSFW, "³");
        public static final CommandIcon PATREON = new CommandIcon(Emojis.COMMAND_ICON_PREMIUM, "⁴");

        private final CustomEmoji customEmoji;
        private final String unicodeAlternative;

        public CommandIcon(CustomEmoji customEmoji, String unicodeAlternative) {
            this.customEmoji = customEmoji;
            this.unicodeAlternative = unicodeAlternative;
        }

        public String get(TextChannel channel) {
            if (BotPermissionUtil.can(channel, Permission.MESSAGE_EXT_EMOJI)) {
                return customEmoji.getFormatted();
            } else {
                return unicodeAlternative;
            }
        }

    }

}
