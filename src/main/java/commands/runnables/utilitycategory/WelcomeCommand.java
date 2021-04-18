package commands.runnables.utilitycategory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import commands.listeners.CommandProperties;
import commands.runnables.NavigationAbstract;
import constants.LogStatus;
import constants.Response;
import core.EmbedFactory;
import core.LocalFile;
import core.TextManager;
import core.utils.FileUtil;
import core.utils.InternetUtil;
import core.utils.MentionUtil;
import core.utils.StringUtil;
import modules.Welcome;
import modules.graphics.WelcomeGraphics;
import mysql.modules.welcomemessage.DBWelcomeMessage;
import mysql.modules.welcomemessage.WelcomeMessageData;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.IMentionable;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.events.message.guild.react.GenericGuildMessageReactionEvent;

@CommandProperties(
        trigger = "welcome",
        botChannelPermissions = Permission.MESSAGE_EXT_EMOJI,
        userGuildPermissions = { Permission.MANAGE_SERVER },
        emoji = "🙋",
        executableWithoutArgs = true
)
public class WelcomeCommand extends NavigationAbstract {

    private WelcomeMessageData welcomeMessageBean;

    public WelcomeCommand(Locale locale, String prefix) {
        super(locale, prefix);
    }

    @Override
    public boolean onTrigger(GuildMessageReceivedEvent event, String args) {
        welcomeMessageBean = DBWelcomeMessage.getInstance().retrieve(event.getGuild().getIdLong());
        welcomeMessageBean.getWelcomeChannel().ifPresent(this::checkWriteInChannelWithLog);
        welcomeMessageBean.getGoodbyeChannel().ifPresent(this::checkWriteInChannelWithLog);
        registerNavigationListener(11);
        return true;
    }

    @Override
    public Response controllerMessage(GuildMessageReceivedEvent event, String input, int state) throws IOException, ExecutionException, InterruptedException {
        switch (state) {
            case 1:
                if (input.length() > 0) {
                    if (input.length() <= 20) {
                        welcomeMessageBean.setWelcomeTitle(input);
                        setLog(LogStatus.SUCCESS, getString("titleset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, getString("titletoolarge", "20"));
                        return Response.FALSE;
                    }
                }
                return Response.FALSE;

            case 2:
                if (input.length() > 0) {
                    if (input.length() <= 1000) {
                        welcomeMessageBean.setWelcomeText(input);
                        setLog(LogStatus.SUCCESS, getString("descriptionset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, getString("descriptiontoolarge", "1000"));
                        return Response.FALSE;
                    }
                }
                return Response.FALSE;

            case 3:
                List<TextChannel> channelList = MentionUtil.getTextChannels(event.getMessage(), input).getList();
                if (channelList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return Response.FALSE;
                } else {
                    if (checkWriteInChannelWithLog(channelList.get(0))) {
                        welcomeMessageBean.setWelcomeChannelId(channelList.get(0).getIdLong());
                        setLog(LogStatus.SUCCESS, getString("channelset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        return Response.FALSE;
                    }
                }

            case 4:
                List<Message.Attachment> attachmentList = event.getMessage().getAttachments();
                if (attachmentList.size() > 0 && attachmentList.get(0).isImage()) {
                    LocalFile localFile = new LocalFile(LocalFile.Directory.WELCOME_BACKGROUNDS, String.format("%d.png", event.getGuild().getIdLong()));
                    if (FileUtil.downloadImageAttachment(attachmentList.get(0), localFile)) {
                        setLog(LogStatus.SUCCESS, getString("backgroundset"));
                        setState(0);
                        return Response.TRUE;
                    }
                }

                setLog(LogStatus.FAILURE, TextManager.getString(getLocale(), TextManager.GENERAL, "imagenotfound"));
                return Response.FALSE;

            case 6:
                if (input.length() > 0) {
                    if (input.length() <= 1000) {
                        welcomeMessageBean.setGoodbyeText(input);
                        setLog(LogStatus.SUCCESS, getString("goodbyetextset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, getString("goodbyetoolarge", "1000"));
                        return Response.FALSE;
                    }
                }
                return Response.FALSE;

            case 7:
                channelList = MentionUtil.getTextChannels(event.getMessage(), input).getList();
                if (channelList.size() == 0) {
                    setLog(LogStatus.FAILURE, TextManager.getNoResultsString(getLocale(), input));
                    return Response.FALSE;
                } else {
                    if (checkWriteInChannelWithLog(channelList.get(0))) {
                        welcomeMessageBean.setGoodbyeChannelId(channelList.get(0).getIdLong());
                        setLog(LogStatus.SUCCESS, getString("farechannelset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        return Response.FALSE;
                    }
                }

            case 8:
                if (input.length() > 0) {
                    if (input.length() <= 1000) {
                        welcomeMessageBean.setDmText(input);
                        setLog(LogStatus.SUCCESS, getString("dmtextset"));
                        setState(0);
                        return Response.TRUE;
                    } else {
                        setLog(LogStatus.FAILURE, getString("dmtoolarge", "1000"));
                        return Response.FALSE;
                    }
                }
                return Response.FALSE;

            default:
                return null;
        }
    }

    @Override
    public boolean controllerReaction(GenericGuildMessageReactionEvent event, int i, int state) {
        if (state == 0) {
            switch (i) {
                case -1:
                    removeNavigationWithMessage();
                    return false;

                case 0:
                    welcomeMessageBean.toggleWelcomeActive();
                    setLog(LogStatus.SUCCESS, getString("activateset", !welcomeMessageBean.isWelcomeActive()));
                    return true;

                case 1:
                    setState(1);
                    return true;

                case 2:
                    setState(2);
                    return true;

                case 3:
                    setState(3);
                    return true;

                case 4:
                    setState(4);
                    return true;

                case 5:
                    welcomeMessageBean.toggleDmActive();
                    setLog(LogStatus.SUCCESS, getString("dmset", !welcomeMessageBean.isDmActive()));
                    return true;

                case 6:
                    setState(8);
                    return true;

                case 7:
                    welcomeMessageBean.toggleGoodbyeActive();
                    setLog(LogStatus.SUCCESS, getString("goodbyeset", !welcomeMessageBean.isGoodbyeActive()));
                    return true;

                case 8:
                    setState(6);
                    return true;

                case 9:
                    setState(7);
                    return true;

                case 10:
                    setState(5);
                    return true;

                default:
                    return false;
            }
        } else if (i == -1) {
            setState(0);
            return true;
        }
        return false;
    }

    @Override
    public EmbedBuilder draw(int state) throws ExecutionException, InterruptedException, IOException {
        String notSet = TextManager.getString(getLocale(), TextManager.GENERAL, "notset");

        if (state == 0) {
            setOptions(getString("state0_options").split("\n"));
            return EmbedFactory.getEmbedDefault(this, getString("state0_description"))
                    .addBlankField(false)
                    .addField(getString("state0_menabled"), StringUtil.getOnOffForBoolean(getLocale(), welcomeMessageBean.isWelcomeActive()), true)
                    .addField(getString("state0_mtitle"), StringUtil.escapeMarkdown(welcomeMessageBean.getWelcomeTitle()), true)
                    .addField(getString("state0_mdescription"), stressVariables(welcomeMessageBean.getWelcomeText()),
                            true
                    )
                    .addField(getString("state0_mchannel"), welcomeMessageBean.getWelcomeChannel().map(IMentionable::getAsMention).orElse(notSet), true)
                    .addBlankField(false)
                    .addField(getString("state0_mdm"), StringUtil.getOnOffForBoolean(getLocale(), welcomeMessageBean.isDmActive()), true)
                    .addField(getString("state0_mdmText"), stressVariables(welcomeMessageBean.getDmText().isEmpty() ? notSet : welcomeMessageBean.getDmText()),
                            true
                    )
                    .addBlankField(false)
                    .addField(getString("state0_mgoodbye"), StringUtil.getOnOffForBoolean(getLocale(), welcomeMessageBean.isGoodbyeActive()), true)
                    .addField(getString("state0_mgoodbyeText"), stressVariables(welcomeMessageBean.getGoodbyeText()), true)
                    .addField(getString("state0_mfarewellchannel"), welcomeMessageBean.getGoodbyeChannel().map(IMentionable::getAsMention).orElse(notSet), true);
        } else if (state == 5) {
            return getWelcomeMessageTest(getMember().get());
        }
        return EmbedFactory.getEmbedDefault(this, getString("state" + state + "_description"), getString("state" + state + "_title"));
    }

    private String stressVariables(String text) {
        return Welcome.resolveVariables(
                StringUtil.escapeMarkdown(text),
                "`%SERVER`",
                "`%USER_MENTION`",
                "`%USER_NAME`",
                "`%USER_DISCRIMINATED`",
                "`%MEMBERS`"
        );
    }

    public EmbedBuilder getWelcomeMessageTest(Member member) throws ExecutionException, InterruptedException, IOException {
        EmbedBuilder eb = EmbedFactory.getEmbedDefault()
                .setDescription(Welcome.resolveVariables(
                        welcomeMessageBean.getWelcomeText(),
                        StringUtil.escapeMarkdown(member.getGuild().getName()),
                        member.getAsMention(),
                        StringUtil.escapeMarkdown(member.getUser().getName()),
                        StringUtil.escapeMarkdown(member.getUser().getAsTag()),
                        StringUtil.numToString(member.getGuild().getMemberCount())
                ));

        eb.setImage(InternetUtil.getUrlFromInputStream(
                WelcomeGraphics.createImageWelcome(member, welcomeMessageBean.getWelcomeTitle()).get(),
                "png"
        ));

        return eb;
    }

}
