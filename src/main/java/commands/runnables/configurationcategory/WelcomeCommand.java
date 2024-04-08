package commands.runnables.configurationcategory;

import commands.CommandEvent;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import commands.stateprocessor.FileStateProcessor;
import commands.stateprocessor.GuildChannelsStateProcessor;
import commands.stateprocessor.StringStateProcessor;
import constants.LogStatus;
import core.EmbedFactory;
import core.LocalFile;
import core.TextManager;
import core.atomicassets.AtomicGuildMessageChannel;
import core.components.ActionRows;
import core.modals.StringModalBuilder;
import core.utils.FileUtil;
import core.utils.InternetUtil;
import core.utils.JDAUtil;
import core.utils.StringUtil;
import modules.Welcome;
import modules.graphics.WelcomeGraphics;
import mysql.hibernate.entity.BotLogEntity;
import mysql.modules.welcomemessage.DBWelcomeMessage;
import mysql.modules.welcomemessage.WelcomeMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.interactions.components.selections.SelectOption;
import net.dv8tion.jda.api.interactions.components.selections.StringSelectMenu;
import net.dv8tion.jda.api.interactions.components.text.TextInputStyle;
import net.dv8tion.jda.api.interactions.modals.Modal;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

@CommandProperties(
        trigger = "welcome",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = {Permission.MANAGE_SERVER},
        emoji = "🙋",
        usesExtEmotes = true,
        executableWithoutArgs = true
)
public class WelcomeCommand extends NavigationAbstract {

    public static int MAX_WELCOME_TITLE_LENGTH = 20;
    public static int MAX_TEXT_LENGTH = MessageEmbed.VALUE_MAX_LENGTH;

    public static final int STATE_SET_TEXT = 1,
            STATE_SET_CHANNEL = 2,
            STATE_SET_BANNER_BACKGROUND = 3,
            STATE_EXAMPLE = 4;


    private WelcomeMessageData welcomeMessageData;
    private int category = 0;

    public WelcomeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(@NotNull CommandEvent event, @NotNull String args) {
        welcomeMessageData = DBWelcomeMessage.getInstance().retrieve(event.getGuild().getIdLong());
        welcomeMessageData.getWelcomeChannel().ifPresent(this::checkWriteEmbedInChannelAndAttachFilesWithLog);
        welcomeMessageData.getGoodbyeChannel().ifPresent(this::checkWriteEmbedInChannelAndAttachFilesWithLog);

        registerNavigationListener(event.getMember(), List.of(
                new StringStateProcessor(this, STATE_SET_TEXT, DEFAULT_STATE, getString("state0_mdescription"))
                        .setDescription(getString("settext_description"))
                        .setClearButton(false)
                        .setMax(MAX_TEXT_LENGTH)
                        .enableHibernateTransaction()
                        .setSetter(input -> {
                            switch (category) {
                                case 0 -> {
                                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_TEXT, event.getMember(), welcomeMessageData.getWelcomeText(), input);
                                    welcomeMessageData.setWelcomeText(input);
                                }
                                case 1 -> {
                                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_DM_TEXT, event.getMember(), welcomeMessageData.getDmText(), input);
                                    welcomeMessageData.setDmText(input);
                                }
                                case 2 -> {
                                    BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_LEAVE_TEXT, event.getMember(), welcomeMessageData.getGoodbyeText(), input);
                                    welcomeMessageData.setGoodbyeText(input);
                                }
                            }
                        }),
                new GuildChannelsStateProcessor(this, STATE_SET_CHANNEL, DEFAULT_STATE, getString("state0_mchannel"))
                        .setMinMax(1, 1)
                        .setChannelTypes(JDAUtil.GUILD_MESSAGE_CHANNEL_CHANNEL_TYPES)
                        .setCheckPermissions(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_ATTACH_FILES)
                        .enableHibernateTransaction()
                        .setSingleGetter(() -> category == 0 ? welcomeMessageData.getWelcomeChannelId() : welcomeMessageData.getGoodbyeChannelId())
                        .setSingleSetter(channelId -> {
                            if (category == 0) {
                                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_CHANNEL, event.getMember(), welcomeMessageData.getWelcomeChannelId(), channelId);
                                welcomeMessageData.setWelcomeChannelId(channelId);
                            } else {
                                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_LEAVE_CHANNEL, event.getMember(), welcomeMessageData.getGoodbyeChannelId(), channelId);
                                welcomeMessageData.setGoodbyeChannelId(channelId);
                            }
                        }),
                new FileStateProcessor(this, STATE_SET_BANNER_BACKGROUND, DEFAULT_STATE, getString("dashboard_backgroundimage"))
                        .setClearButton(true)
                        .enableHibernateTransaction()
                        .setSetter(attachment -> {
                            LocalFile localFile = new LocalFile(LocalFile.Directory.WELCOME_BACKGROUNDS, String.format("%d.png", event.getGuild().getIdLong()));
                            if (attachment != null) {
                                if (!FileUtil.downloadImageAttachment(attachment, localFile)) {
                                    throw new RuntimeException("File download failed");
                                }
                                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_BANNER_BACKGROUND_SET, event.getMember());
                            } else {
                                localFile.delete();
                                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_BANNER_BACKGROUND_RESET, event.getMember());
                            }
                        })
        ));
        return true;
    }

    @ControllerButton(state = DEFAULT_STATE)
    public boolean onButtonDefault(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            deregisterListenersWithComponentMessage();
            return false;
        }
        return switch (category) {
            case 0 -> onButtonWelcome(event, i);
            case 1 -> onButtonDM(event, i);
            case 2 -> onButtonLeave(event, i);
            default -> false;
        };
    }

    @ControllerButton(state = STATE_EXAMPLE)
    public boolean onButtonExample(ButtonInteractionEvent event, int i) {
        if (i == -1) {
            setState(DEFAULT_STATE);
            return true;
        }
        return false;
    }

    @ControllerStringSelectMenu(state = DEFAULT_STATE)
    public boolean onSelectMenuDefault(StringSelectInteractionEvent event, int i) {
        category = Integer.parseInt(event.getValues().get(0));
        return true;
    }

    @Draw(state = DEFAULT_STATE)
    public EmbedBuilder drawDefault(Member member) {
        String[] options = getString("state0_options_" + category).split("\n");
        ArrayList<Button> buttons = new ArrayList<>();
        for (int i = 0; i < options.length; i++) {
            Button button = Button.of(ButtonStyle.PRIMARY, String.valueOf(i), options[i]);
            buttons.add(button);
        }

        ArrayList<ActionRow> actionRows = new ArrayList<>(ActionRows.of(buttons));
        actionRows.add(generateSelectMenu());
        setActionRows(actionRows);

        GuildMessageChannel channel = getGuildMessageChannel().get();
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");
        return switch (category) {
            case 0 -> EmbedFactory.getEmbedDefault(this, getString("state0_description"), getString("dashboard_join"))
                    .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(channel, getLocale(), welcomeMessageData.isWelcomeActive()), true)
                    .addField(getString("state0_mdescription"), StringUtil.shortenString(stressVariables(welcomeMessageData.getWelcomeText()), 1024), true)
                    .addField(getString("state0_membed"), StringUtil.getOnOffForBoolean(channel, getLocale(), welcomeMessageData.getWelcomeEmbed()), true)
                    .addField(getString("state0_mchannel"), welcomeMessageData.getWelcomeChannel().map(c -> new AtomicGuildMessageChannel(c).getPrefixedNameInField(getLocale())).orElse(notSet), true)
                    .addField(getString("state0_mbanner"), StringUtil.getOnOffForBoolean(channel, getLocale(), welcomeMessageData.getBanner()), true)
                    .addField(getString("state0_mtitle"), StringUtil.escapeMarkdown(welcomeMessageData.getWelcomeTitle()), true);
            case 1 -> EmbedFactory.getEmbedDefault(this, getString("state0_description"), getString("dashboard_dm"))
                    .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(channel, getLocale(), welcomeMessageData.isDmActive()), true)
                    .addField(getString("state0_mdescription"), StringUtil.shortenString(stressVariables(welcomeMessageData.getDmText()), 1024), true)
                    .addField(getString("state0_membed"), StringUtil.getOnOffForBoolean(channel, getLocale(), welcomeMessageData.getDmEmbed()), true);
            case 2 -> EmbedFactory.getEmbedDefault(this, getString("state0_description"), getString("dashboard_leave"))
                    .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(channel, getLocale(), welcomeMessageData.isGoodbyeActive()), true)
                    .addField(getString("state0_mdescription"), StringUtil.shortenString(stressVariables(welcomeMessageData.getGoodbyeText()), 1024), true)
                    .addField(getString("state0_membed"), StringUtil.getOnOffForBoolean(channel, getLocale(), welcomeMessageData.getGoodbyeEmbed()), true)
                    .addField(getString("state0_mchannel"), welcomeMessageData.getGoodbyeChannel().map(c -> new AtomicGuildMessageChannel(c).getPrefixedNameInField(getLocale())).orElse(notSet), true);
            default -> throw new UnsupportedOperationException("Invalid category");
        };
    }

    @Draw(state = STATE_EXAMPLE)
    public EmbedBuilder drawExample(Member member) throws ExecutionException, InterruptedException, IOException {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(Welcome.resolveVariables(
                        welcomeMessageData.getWelcomeText(),
                        StringUtil.escapeMarkdown(member.getGuild().getName()),
                        member.getAsMention(),
                        StringUtil.escapeMarkdown(member.getUser().getName()),
                        StringUtil.escapeMarkdown(member.getUser().getAsTag()),
                        StringUtil.numToString(member.getGuild().getMemberCount()),
                        StringUtil.escapeMarkdown(member.getUser().getEffectiveName())
                ));

        if (welcomeMessageData.getBanner()) {
            eb.setImage(InternetUtil.getUrlFromInputStream(
                    WelcomeGraphics.createImageWelcome(member, welcomeMessageData.getWelcomeTitle()).get(),
                    "png"
            ));
        }

        return eb;
    }

    private boolean onButtonWelcome(ButtonInteractionEvent event, int i) {
        switch (i) {
            case 0 -> {
                welcomeMessageData.toggleWelcomeActive();

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_ACTIVE, event.getMember(), null, welcomeMessageData.isWelcomeActive());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("activeset", welcomeMessageData.isWelcomeActive(), getString("dashboard_join")));
                return true;
            }
            case 1 -> {
                setState(STATE_SET_TEXT);
                return true;
            }
            case 2 -> {
                welcomeMessageData.toggleWelcomeEmbed();

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_EMBEDS, event.getMember(), null, welcomeMessageData.getWelcomeEmbed());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("embedset", welcomeMessageData.getWelcomeEmbed()));
                return true;
            }
            case 3 -> {
                setState(STATE_SET_CHANNEL);
                return true;
            }
            case 4 -> {
                welcomeMessageData.toggleBanner();

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_BANNERS, event.getMember(), null, welcomeMessageData.getBanner());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("bannerset", welcomeMessageData.getBanner()));
                return true;
            }
            case 5 -> {
                Modal modal = new StringModalBuilder(this, getString("state0_mtitle"), TextInputStyle.SHORT)
                        .setMinMaxLength(1, MAX_WELCOME_TITLE_LENGTH)
                        .setLogEvent(BotLogEntity.Event.WELCOME_BANNER_TITLE)
                        .setGetter(() -> welcomeMessageData.getWelcomeTitle())
                        .setSetter(s -> welcomeMessageData.setWelcomeTitle(s))
                        .build();

                event.replyModal(modal).queue();
                return false;
            }
            case 6 -> {
                setState(STATE_SET_BANNER_BACKGROUND);
                return true;
            }
            case 7 -> {
                setState(STATE_EXAMPLE);
                return true;
            }
        }
        return false;
    }

    private boolean onButtonDM(ButtonInteractionEvent event, int i) {
        switch (i) {
            case 0 -> {
                welcomeMessageData.toggleDmActive();

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_DM_ACTIVE, event.getMember(), null, welcomeMessageData.isDmActive());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("activeset", welcomeMessageData.isDmActive(), getString("dashboard_dm")));
                return true;
            }
            case 1 -> {
                setState(STATE_SET_TEXT);
                return true;
            }
            case 2 -> {
                welcomeMessageData.toggleDmEmbed();

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_DM_EMBEDS, event.getMember(), null, welcomeMessageData.getDmEmbed());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("embedset", welcomeMessageData.getDmEmbed()));
                return true;
            }
        }
        return false;
    }

    private boolean onButtonLeave(ButtonInteractionEvent event, int i) {
        switch (i) {
            case 0 -> {
                welcomeMessageData.toggleGoodbyeActive();

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_LEAVE_ACTIVE, event.getMember(), null, welcomeMessageData.isGoodbyeActive());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("activeset", welcomeMessageData.isGoodbyeActive(), getString("dashboard_leave")));
                return true;
            }
            case 1 -> {
                setState(STATE_SET_TEXT);
                return true;
            }
            case 2 -> {
                welcomeMessageData.toggleGoodbyeEmbed();

                getEntityManager().getTransaction().begin();
                BotLogEntity.log(getEntityManager(), BotLogEntity.Event.WELCOME_LEAVE_EMBEDS, event.getMember(), null, welcomeMessageData.getGoodbyeEmbed());
                getEntityManager().getTransaction().commit();

                setLog(LogStatus.SUCCESS, getString("embedset", welcomeMessageData.getGoodbyeEmbed()));
                return true;
            }
            case 3 -> {
                setState(STATE_SET_CHANNEL);
                return true;
            }
        }
        return false;
    }

    private String stressVariables(String text) {
        return Welcome.resolveVariables(
                StringUtil.escapeMarkdown(text),
                "`%SERVER`",
                "`%USER_MENTION`",
                "`%USERNAME`",
                "`%USER_DISCRIMINATED`",
                "`%MEMBERS`",
                "`%DISPLAY_NAME`"
        );
    }

    private ActionRow generateSelectMenu() {
        String[] selectMenuOptions = getString("state0_selectmenu").split("\n");
        StringSelectMenu.Builder selectMenuBuilder = StringSelectMenu.create("category")
                .setRequiredRange(1, 1);
        for (int i = 0; i < selectMenuOptions.length; i++) {
            selectMenuBuilder.addOption(selectMenuOptions[i], String.valueOf(i));
        }
        selectMenuBuilder.setDefaultOptions(SelectOption.of(selectMenuOptions[category], String.valueOf(category)));
        return ActionRow.of(selectMenuBuilder.build());
    }

}
