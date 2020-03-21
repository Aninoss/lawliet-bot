package Commands.CasinoCategory;

import CommandListeners.CommandProperties;
import CommandListeners.onReactionAddListener;
import CommandListeners.onRecievedListener;
import Commands.CasinoAbstract;
import Constants.LogStatus;
import General.*;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Random;


@CommandProperties(
        trigger = "coinflip",
        emoji = "\uD83D\uDCB0",
        thumbnail = "http://icons.iconarchive.com/icons/flat-icons.com/flat/128/Coins-icon.png",
        executable = true,
        deleteOnTimeOut = false,
        aliases = {"coin", "cf"}
)
public class CoinFlipCommand extends CasinoAbstract implements onRecievedListener, onReactionAddListener {

    private String log;
    private final String[] EMOJIS = {"\uD83C\uDDED", "\uD83C\uDDF9"};
    private int[] selection = {-1, -1};
    private LogStatus logStatus;

    public CoinFlipCommand() {
        super();
    }

    @Override
    public boolean onReceived(MessageCreateEvent event, String followedString) throws Throwable {
        onlyNumbersAsArg = false;
        if (onGameStart(event, followedString)) {
            useCalculatedMultiplicator = false;
            winMultiplicator = 1;

            String filteredString = Tools.cutSpaces(Tools.filterLettersFromString(followedString.toLowerCase()));
            if (filteredString.contains("h")) selection[0] = 0;
            else if (filteredString.contains("t")) selection[0] = 1;

            message = event.getChannel().sendMessage(getEmbed(event.getServerTextChannel().get(), event.getMessage().getUserAuthor().get())).get();
            if (selection[0] == -1) for (String str : EMOJIS) message.addReaction(str);
            else manageEnd();

            return true;
        }
        return false;
    }

    private String getChoiceString(ServerTextChannel channel, int pos) {
        if (pos == 1 && selection[0] == -1) return Tools.getEmptyCharacter();

        switch (selection[pos]) {
            case 0:
                return EMOJIS[0];
            case 1:
                return EMOJIS[1];
            default:
                return Tools.getLoadingReaction(channel);
        }
    }

    private EmbedBuilder getEmbed() throws IOException {
        return getEmbed(message.getServerTextChannel().get(), player);
    }

    private EmbedBuilder getEmbed(ServerTextChannel channel, User user) throws IOException {
        EmbedBuilder eb = EmbedFactory.getCommandEmbedStandard(this);
        eb.addField(getString("yourbet"), getChoiceString(channel, 0), true);
        eb.addField(getString("yourthrow"), getChoiceString(channel, 1), true);
        eb.addField(Tools.getEmptyCharacter(), getString("template", user.getDisplayName(server), Tools.numToString(coinsInput)));

        if (selection[0] == -1) eb.addField(Tools.getEmptyCharacter(), getString("expl", EMOJIS[0], EMOJIS[1]));

        if (coinsInput != 0) eb.setFooter(TextManager.getString(getLocale(), TextManager.COMMANDS, "casino_footer"));

        if (!active) {
            eb = EmbedFactory.addLog(eb, logStatus, log);
            eb = addRetryOption(eb);
        }

        return eb;
    }

    private void manageEnd() {
        if (selection[0] == -1) return;
        removeReactionListener(getReactionMessage());

        Thread t = new Thread(() -> {
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            selection[1] = new Random().nextInt(2);
            if (selection[0] == selection[1]) {
                try {
                    log = TextManager.getString(getLocale(), TextManager.GENERAL, "won");
                    logStatus = LogStatus.WIN;
                    onWin();
                } catch (IOException | SQLException e) {
                    ExceptionHandler.handleException(e, getLocale(), message.getServerTextChannel().get());
                }
            } else {
                try {
                    log = TextManager.getString(getLocale(), TextManager.GENERAL, "lost");
                    logStatus = LogStatus.LOSE;
                    onLose();
                } catch (IOException | SQLException e) {
                    ExceptionHandler.handleException(e, getLocale(), message.getServerTextChannel().get());
                }
            }

            try {
                message.edit(getEmbed());
            } catch (IOException e) {
                ExceptionHandler.handleException(e, getLocale(), message.getServerTextChannel().get());
            }
        });
        t.setName("coinflip_cpu");
        t.setPriority(1);
        t.start();
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        if (!active) {
            onReactionAddRetry(event);
            return;
        }

        if (event.getEmoji().isUnicodeEmoji()) {
            for(int i = 0; i < 2; i++) {
                if (event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(EMOJIS[i])) {
                    selection[0] = i;
                    message.edit(getEmbed());
                    manageEnd();
                    return;
                }
            }
        }
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {
        if (active) {
            selection[0] = 0;
            message.edit(getEmbed());
            manageEnd();
        }
    }
}
