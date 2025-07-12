package commands.runnables.informationcategory;

import commands.*;
import commands.listeners.CommandProperties;
import commands.listeners.OnAlertListener;
import commands.runnables.*;
import commands.runnables.interactionscategory.CustomRolePlaySfwCommand;
import commands.runnables.interactionscategory.RolePlayGenderCommand;
import commands.runnables.nsfwinteractionscategory.CustomRolePlayNsfwCommand;
import constants.Emojis;
import constants.ExternalLinks;
import constants.LogStatus;
import core.EmbedFactory;
import core.ListGen;
import core.Program;
import core.TextManager;
import core.modals.ModalMediator;
import core.utils.BotPermissionUtil;
import core.utils.EmbedUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import mysql.hibernate.entity.CustomRolePlayEntity;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.entities.emoji.UnicodeEmoji;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectMenu;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInput;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

@CommandProperties(
        trigger = "help",
        emoji = "❕",
        executableWithoutArgs = true,
        usesExtEmotes = true,
        aliases = {"commands"}
)
public class HelpCommand extends NavigationAbstract {

    public static final String BUTTON_ID_BROWSE = "browse";

    private static final int STATE_NOT_DEFAULT = 1;

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

    @Override
    public boolean controllerButton(ButtonInteractionEvent event, int i, int state) {
        if (event.getComponentId().equals(BUTTON_ID_BROWSE)) {
            TextInput textInput = TextInput.create("text", getString("trigger"), TextInputStyle.SHORT)
                    .setMinLength(1)
                    .setMaxLength(50)
                    .build();

            Modal modal = ModalMediator.createDrawableCommandModal(this, getString("command_button"), e -> {
                        String input = e.getValues().get(0).getAsString().toLowerCase();
                        String prefix = getGuildEntity().getPrefix();
                        if (input.startsWith(prefix.toLowerCase())) {
                            input = input.substring(prefix.length());
                        }

                        String finalInput = input;
                        if (buttonMap.values().stream().anyMatch(str -> str.equalsIgnoreCase(finalInput))) {
                            searchTerm = input;
                        } else {
                            setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                        }
                        return null;
                    })
                    .addActionRow(textInput)
                    .build();

            event.replyModal(modal).queue();
            return false;
        }

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

    @Override
    public boolean controllerStringSelectMenu(StringSelectInteractionEvent event, int i, int state) throws Throwable {
        searchTerm = event.getValues().get(0);
        return true;
    }

    @Override
    public EmbedBuilder draw(Member member, int state) {
        String arg = searchTerm.trim();
        if (arg.startsWith("<") && arg.endsWith(">")) arg = arg.substring(1, arg.length() - 1);

        GuildMessageChannel channel = getGuildMessageChannel().get();
        setActionRows();

        setState(STATE_NOT_DEFAULT);
        EmbedBuilder eb;
        if ((eb = checkCommand(member, channel, arg)) == null) {
            if ((eb = checkCategory(member, channel, arg)) == null) {
                setState(DEFAULT_STATE);
                eb = checkMainPage(member, channel);
                if (!arg.isEmpty()) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), arg));
                }
            }
        }

        String error = getAttachment("error", String.class);
        if (error != null) {
            setLog(LogStatus.FAILURE, error);
            removeAttachment("error");
        }
        return eb;
    }

    private EmbedBuilder checkCommand(Member member, GuildMessageChannel channel, String arg) {
        boolean noArgs = false;
        if (hasAttachment("noargs")) {
            removeAttachment("noargs");
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
                if (command.getCommandProperties().nsfw() && !JDAUtil.channelIsNsfw(channel)) {
                    buttonMap.put(-1, "");
                    return EmbedFactory.getNSFWBlockEmbed(this);
                }
                buttonMap.put(-1, "cat:" + currentCategory.getId());

                StringBuilder usage = new StringBuilder();
                for (String line : TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_usage").split("\n")) {
                    usage.append("- ").append(getPrefix()).append(commandTrigger).append(" ").append(line).append("\n");
                }

                StringBuilder examples = new StringBuilder();
                int exampleNumber = 0;
                for (String line : TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_examples").split("\n")) {
                    line = StringUtil.solveVariablesOfCommandText(line, getGuildMessageChannel().get(), member, getPrefix());
                    examples.append("- ").append(getPrefix()).append(commandTrigger).append(" ").append(line).append("\n");
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

    private EmbedBuilder checkCategory(Member member, GuildMessageChannel channel, String arg) {
        if (arg.startsWith("cat:")) {
            arg = arg.substring(4);
        }

        if (!arg.isEmpty()) {
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

                if (category.isNSFW() && !JDAUtil.channelIsNsfw(channel)) {
                    return EmbedFactory.getNSFWBlockEmbed(this);
                }

                EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                        .setTitle(TextManager.getString(getLocale(), TextManager.COMMANDS, category.getId()));
                EmbedUtil.setFooter(eb, this, getString("navigation"));

                switch (category) {
                    case CONFIGURATION -> categoryConfiguration(member, channel, eb);
                    case INTERACTIONS -> categoryRolePlay(member, eb);
                    case NSFW_INTERACTIONS -> categoryNSFWRolePlay(member, eb);
                    case NSFW -> categoryNSFW(member, channel, eb);
                    case PATREON_ONLY -> categoryPatreon(member, channel, eb);
                    default -> categoryDefault(member, channel, eb, category);
                }

                setActionRows(
                        ActionRow.of(Button.of(ButtonStyle.PRIMARY, BUTTON_ID_BROWSE, getString("command_button"))),
                        ActionRow.of(generateCategoriesSelectMenu(member, channel, category))
                );
                return eb;
            }
        }

        return null;
    }

    private void categoryRolePlay(Member member, EmbedBuilder eb) {
        eb.setDescription(getString("roleplay_interactive_gender"));
        buttonMap.put(0, Command.getCommandProperties(RolePlayGenderCommand.class).trigger());

        AtomicInteger counter = new AtomicInteger(1);
        addRolePlayCommandList(member, eb, Category.INTERACTIONS, command -> !command.isInteractive(), counter);
        eb.addBlankField(false);

        eb.addField(getString("roleplay_interactive_title"), getString("roleplay_interactive_desc"), false);
        addRolePlayCommandList(member, eb, Category.INTERACTIONS, RolePlayAbstract::isInteractive, counter);

        eb.addBlankField(false);
        eb.addField(getString("roleplay_custom_title"), getString("roleplay_custom_desc"), false);
        addCustomRolePlayCommandList(member, eb, CustomRolePlaySfwCommand.class, false);
    }

    private void categoryNSFWRolePlay(Member member, EmbedBuilder eb) {
        eb.setDescription(getString("roleplay_interactive_gender"));
        buttonMap.put(0, Command.getCommandProperties(RolePlayGenderCommand.class).trigger());

        AtomicInteger counter = new AtomicInteger(1);
        eb.addField(getString("roleplay_interactive_title"), getString("roleplay_interactive_desc"), false);
        addRolePlayCommandList(member, eb, Category.NSFW_INTERACTIONS, command -> true, counter);

        eb.addBlankField(false);
        eb.addField(getString("roleplay_custom_title"), getString("roleplay_custom_desc"), false);
        addCustomRolePlayCommandList(member, eb, CustomRolePlayNsfwCommand.class, true);
    }

    private void addRolePlayCommandList(Member member, EmbedBuilder eb, Category category, Function<RolePlayAbstract, Boolean> rolePlayAbstractFilter, AtomicInteger counter) {
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getCommandCategoryMap().get(category)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            if (!(command instanceof RolePlayAbstract)) {
                continue;
            }

            String commandTrigger = command.getTrigger();
            if (rolePlayAbstractFilter.apply((RolePlayAbstract) command) && CommandManager.commandIsEnabledEffectively(getGuildEntity(), command, member, getGuildMessageChannel().get())) {
                buttonMap.put(counter.getAndIncrement(), command.getTrigger());
                stringBuilder
                        .append("- `")
                        .append(command.getCommandProperties().emoji())
                        .append("⠀")
                        .append(getPrefix())
                        .append(commandTrigger)
                        .append("`")
                        .append("\n");

                i++;
                if (i >= 10) {
                    if (!stringBuilder.isEmpty()) {
                        eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), stringBuilder.toString(), true);
                    }
                    stringBuilder = new StringBuilder();
                    i = 0;
                }
            }
        }
        if (!stringBuilder.isEmpty()) {
            eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), stringBuilder.toString(), true);
        }
    }

    private void addCustomRolePlayCommandList(Member member, EmbedBuilder eb, Class<? extends CustomRolePlaySfwCommand> clazz, boolean nsfw) {
        StringBuilder stringBuilder = new StringBuilder();
        int i = 0;
        for (Map.Entry<String, CustomRolePlayEntity> entry : getGuildEntity().getCustomRolePlayCommandsEffectively().entrySet()) {
            if (!CommandManager.commandIsEnabledEffectively(getGuildEntity(), clazz, member, getGuildMessageChannel().get()) ||
                    entry.getValue().getNsfw() != nsfw
            ) {
                continue;
            }

            stringBuilder.append("- `");
            if (entry.getValue().getEmoji() instanceof UnicodeEmoji) {
                stringBuilder.append(entry.getValue().getEmojiFormatted())
                        .append("⠀");
            }
            stringBuilder.append(getPrefix())
                    .append(entry.getKey())
                    .append("`")
                    .append("\n");

            i++;
            if (i >= 10) {
                if (!stringBuilder.isEmpty()) {
                    eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), stringBuilder.toString(), true);
                }
                stringBuilder = new StringBuilder();
                i = 0;
            }
        }
        if (!stringBuilder.isEmpty()) {
            eb.addField(Emojis.ZERO_WIDTH_SPACE.getFormatted(), stringBuilder.toString(), true);
        }
    }

    private void categoryPatreon(Member member, GuildMessageChannel channel, EmbedBuilder eb) {
        boolean includeLocked = false;
        boolean includeAlerts = false;
        boolean includeNSFW = false;

        ArrayList<StringBuilder> predefinedBooruCommandsFields = new ArrayList<>();
        predefinedBooruCommandsFields.add(new StringBuilder());

        int i = 0;
        for (Category category : Category.independentValues()) {
            for (Class<? extends Command> clazz : CommandContainer.getCommandCategoryMap().get(category)) {
                Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
                String commandTrigger = command.getTrigger();
                if (command.getCommandProperties().patreonRequired() &&
                        !commandTrigger.equals(getTrigger()) &&
                        CommandManager.commandIsEnabledEffectively(getGuildEntity(), command, member, getGuildMessageChannel().get()) &&
                        (!command.getCommandProperties().nsfw() || JDAUtil.channelIsNsfw(channel))
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

                    if (command instanceof PornPredefinedAbstract) {
                        String extras = generateCommandIcons(channel, command, false, true, false);
                        String booruLine = getString("nsfw_slot", command.getTrigger(), extras, command.getCommandLanguage().getTitle()) + "\n";
                        StringBuilder lastStringBuilder = predefinedBooruCommandsFields.get(predefinedBooruCommandsFields.size() - 1);
                        if (lastStringBuilder.length() + booruLine.length() <= MessageEmbed.VALUE_MAX_LENGTH) {
                            lastStringBuilder.append(booruLine);
                        } else {
                            predefinedBooruCommandsFields.add(new StringBuilder(booruLine));
                        }
                    } else {
                        eb.addField(
                                title.toString(),
                                TextManager.getString(getLocale(), command.getCategory(), commandTrigger + "_description") + "\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted(),
                                true
                        );
                    }
                }
            }
        }
        for (int j = 0; j < predefinedBooruCommandsFields.size(); j++) {
            eb.addField(j == 0 ? getString("nsfw_premium") : Emojis.ZERO_WIDTH_SPACE.getFormatted(), predefinedBooruCommandsFields.get(j).toString(), false);
        }

        eb.setDescription(getString("premium", ExternalLinks.PREMIUM_WEBSITE) + "\n" + Emojis.ZERO_WIDTH_SPACE.getFormatted());
        addIconDescriptions(channel, eb, includeLocked, includeAlerts, includeNSFW, false);
    }

    private void categoryDefault(Member member, GuildMessageChannel channel, EmbedBuilder eb, Category category) {
        boolean includeLocked = false;
        boolean includeAlerts = false;
        boolean includeNSFW = false;
        boolean includePatreon = false;

        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getCommandCategoryMap().get(category)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());
            String commandTrigger = command.getTrigger();
            if (!commandTrigger.equals(getTrigger()) &&
                    CommandManager.commandIsEnabledEffectively(getGuildEntity(), command, member, getGuildMessageChannel().get())
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

    private void categoryConfiguration(Member member, GuildMessageChannel channel, EmbedBuilder eb) {
        StringBuilder commands = new StringBuilder();

        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getCommandCategoryMap().get(Category.CONFIGURATION)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());

            if (CommandManager.commandIsEnabledEffectively(getGuildEntity(), command, member, getGuildMessageChannel().get())) {
                buttonMap.put(i++, command.getTrigger());
                String title = TextManager.getString(getLocale(), command.getCategory(), command.getTrigger() + "_title");

                String extras = generateCommandIcons(channel, command, false, false, true);
                commands.append(getString("configuration_slot", command.getCommandProperties().emoji(), command.getTrigger(), extras, title)).append("\n\n");
            }
        }

        EmbedUtil.addFieldSplit(eb, null, commands.append(Emojis.ZERO_WIDTH_SPACE.getFormatted()).toString(), true, "\n\n");
        addIconDescriptions(channel, eb, true, false, false, true);
    }

    private void categoryNSFW(Member member, GuildMessageChannel channel, EmbedBuilder eb) {
        eb.setDescription(getString("nsfw"));

        StringBuilder withSearchKey = new StringBuilder();
        StringBuilder other = new StringBuilder();
        StringBuilder withoutSearchKeyHentai = new StringBuilder();
        StringBuilder withoutSearchKeyRealLife = new StringBuilder();

        int i = 0;
        for (Class<? extends Command> clazz : CommandContainer.getCommandCategoryMap().get(Category.NSFW)) {
            Command command = CommandManager.createCommandByClass(clazz, getLocale(), getPrefix());

            if (CommandManager.commandIsEnabledEffectively(getGuildEntity(), command, member, getGuildMessageChannel().get())) {
                buttonMap.put(i++, command.getTrigger());
                String title = TextManager.getString(getLocale(), command.getCategory(), command.getTrigger() + "_title");

                String extras = generateCommandIcons(channel, command, false, false, true);
                if (command instanceof PornSearchAbstract) {
                    if (!withSearchKey.isEmpty()) {
                        withSearchKey.append("\n");
                    }
                    withSearchKey.append(getString("nsfw_slot_ext", command.getTrigger(), extras, title));
                } else if (command instanceof PornPredefinedAbstract) {
                    if (command instanceof RealbooruAbstract) {
                        if (!withoutSearchKeyRealLife.isEmpty()) {
                            withoutSearchKeyRealLife.append("\n");
                        }
                        withoutSearchKeyRealLife.append(getString("nsfw_slot", command.getTrigger(), extras, title));
                    } else {
                        if (!withoutSearchKeyHentai.isEmpty()) {
                            withoutSearchKeyHentai.append("\n");
                        }
                        withoutSearchKeyHentai.append(getString("nsfw_slot", command.getTrigger(), extras, title));
                    }
                } else {
                    if (!other.isEmpty()) {
                        other.append("\n");
                    }
                    other.append(getString("nsfw_slot_ext", command.getTrigger(), extras, title));
                }
            }
        }

        if (!withSearchKey.isEmpty()) {
            eb.addField(getString("nsfw_searchkey_on"), withSearchKey.toString(), false);
        }
        if (!withoutSearchKeyHentai.isEmpty()) {
            EmbedUtil.addFieldSplit(eb, getString("nsfw_searchkey_off_hentai"), withoutSearchKeyHentai.append(Emojis.ZERO_WIDTH_SPACE.getFormatted()).toString(), true);
        }
        if (!withoutSearchKeyRealLife.isEmpty()) {
            EmbedUtil.addFieldSplit(eb, getString("nsfw_searchkey_off_rl"), withoutSearchKeyRealLife.append(Emojis.ZERO_WIDTH_SPACE.getFormatted()).toString(), true);
        }
        if (!other.isEmpty()) {
            eb.addField(getString("nsfw_other"), other.toString(), false);
        }

        addIconDescriptions(channel, eb, false, false, false, true);
    }

    private String generateCommandIcons(GuildMessageChannel channel, Command command, boolean includeAlert, boolean includeNsfw, boolean includePatreon) {
        StringBuilder sb = new StringBuilder();

        if (command.isModCommand()) sb.append(CommandIcon.LOCKED.get(channel));
        if (includeAlert && command instanceof OnAlertListener) sb.append(CommandIcon.ALERTS.get(channel));
        if (includeNsfw && command.getCommandProperties().nsfw()) sb.append(CommandIcon.NSFW.get(channel));
        if (includePatreon && command.getCommandProperties().patreonRequired()) {
            sb.append(CommandIcon.PATREON.get(channel));
        }

        return sb.isEmpty() ? "" : "┊" + sb;
    }

    private void addIconDescriptions(GuildMessageChannel channel, EmbedBuilder eb, boolean includeLocked, boolean includeAlerts, boolean includeNSFW, boolean includePatreon) {
        StringBuilder sb = new StringBuilder();
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

    private EmbedBuilder checkMainPage(Member member, GuildMessageChannel channel) {
        String banner = Program.publicInstance()
                ? "https://cdn.discordapp.com/attachments/499629904380297226/850825690399899658/help_banner.png"
                : "https://cdn.discordapp.com/attachments/499629904380297226/1106609492256370779/help_banner_custom.png";

        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setTitle(TextManager.getString(getLocale(), TextManager.COMMANDS, "categories"))
                .setDescription(getString("sp"))
                .setImage(banner);
        EmbedUtil.setFooter(eb, this, getString("navigation"));

        buttonMap.clear();
        buttonMap.put(-1, "quit");

        if (Program.publicInstance()) {
            eb.addField(getString("links_title"), getString(
                    Program.publicInstance() ? "links_content" : "help_links_content_notpublic",
                    ExternalLinks.LAWLIET_WEBSITE,
                    ExternalLinks.SERVER_INVITE_URL,
                    ExternalLinks.BOT_INVITE_URL,
                    ExternalLinks.UPVOTE_URL,
                    ExternalLinks.PREMIUM_WEBSITE,
                    ExternalLinks.FEATURE_REQUESTS_WEBSITE
            ), true);
        }

        setComponents(generateCategoriesSelectMenu(member, channel, null));
        return eb;
    }

    private SelectMenu generateCategoriesSelectMenu(Member member, GuildMessageChannel channel, Category currentCategory) {
        StringSelectMenu.Builder builder = StringSelectMenu.create("category")
                .setPlaceholder(getString("category_placeholder"));
        for (Category category : Category.values()) {
            if (CommandManager.commandCategoryIsEnabledEffectively(getGuildEntity(), category, member, channel) &&
                    (!category.isNSFW() || JDAUtil.channelIsNsfw(channel))
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

        public String get(GuildMessageChannel channel) {
            if (BotPermissionUtil.can(channel, Permission.MESSAGE_EXT_EMOJI)) {
                return customEmoji.getFormatted();
            } else {
                return unicodeAlternative;
            }
        }

    }

}
