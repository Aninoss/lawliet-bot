package Commands.Casino;

import CommandListeners.*;
import CommandSupporters.Command;
import CommandSupporters.CommandManager;
import Constants.PowerPlantStatus;
import General.EmbedFactory;
import General.RunningCommands.RunningCommandManager;
import General.TextManager;
import General.Tools;
import MySQL.DBBot;
import MySQL.DBServer;
import MySQL.DBUser;
import org.javacord.api.entity.channel.ServerTextChannel;
import org.javacord.api.entity.message.Message;
import org.javacord.api.entity.message.embed.EmbedBuilder;
import org.javacord.api.entity.user.User;
import org.javacord.api.event.message.MessageCreateEvent;
import org.javacord.api.entity.server.Server;
import org.javacord.api.event.message.reaction.ReactionAddEvent;
import org.javacord.api.event.message.reaction.SingleReactionEvent;

class Casino extends Command implements onReactionAddListener {
    long coinsInput;
    User player;
    Server server;
    ServerTextChannel channel;
    double winMultiplicator;
    boolean active, won, useCalculatedMultiplicator;
    final double BONUS_MULTIPLICATOR = 1;
    String compareKey;
    Message message;
    private MessageCreateEvent createEvent;
    private final String RETRY_EMOJI = "\uD83D\uDD01";

    Casino() {
        super();
        privateUse = false;
        nsfw = false;
        withLoadingBar = false;
        executable = true;
    }

    boolean onGameStart(MessageCreateEvent event, String followedString) throws Throwable {
        createEvent = event;
        server = event.getServer().get();
        player = event.getMessage().getUserAuthor().get();
        channel = event.getServerTextChannel().get();
        active = true;
        useCalculatedMultiplicator = true;
        compareKey = trigger;

        PowerPlantStatus status = DBServer.getPowerPlantStatusFromServer(server);
        if (status != PowerPlantStatus.ACTIVE) {
            coinsInput = 0;
            return true;
        }

        long coins = DBUser.getFishingProfile(server, player).getCoins();
        if (followedString.length() == 0) {
            coinsInput = (long) Math.ceil(coins * 0.1);
            DBUser.addFishingValues(locale, server, player, 0, -coinsInput);
            return true;
        }

        long value = Tools.filterNumberFromString(followedString);

        if (followedString.toLowerCase().contains("all")) {
            value = coins;
        }

        if (value != -1) {
            if (value >= 0) {
                if (value <= coins) {
                    coinsInput = value;
                    DBUser.addFishingValues(locale, server, player, 0, -coinsInput);
                    return true;
                } else {
                    event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(locale, TextManager.COMMANDS, "casino_too_large", Tools.numToString(locale, coins)))).get();
                }
            } else {
                event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(locale, TextManager.GENERAL, "too_small", "0"))).get();
            }
        } else {
            event.getChannel().sendMessage(EmbedFactory.getCommandEmbedError(this, TextManager.getString(locale, TextManager.GENERAL, "no_digit"))).get();
        }

        return false;
    }

    public void onGameEnd() throws Throwable {
        won = false;
        active = false;
        DBUser.addFishingValues(locale, server, player, 0, coinsInput);
        removeNavigation();
        removeMessageForwarder();
        removeReactionListener(((onReactionAddListener)this).getReactionMessage());
    }

    void onLose() throws Throwable {
        onGameEnd();
        DBBot.getGameWonMultiplicator(compareKey, false, 1);
        EmbedBuilder eb = DBUser.addFishingValues(locale, server, player, 0, -coinsInput);
        if (coinsInput > 0) channel.sendMessage(eb);
    }

    void onWin() throws Throwable {
        onGameEnd();
        won = true;

        long coinsWon = (long) Math.ceil(coinsInput * winMultiplicator);
        double multiplicator = DBBot.getGameWonMultiplicator(compareKey, true, winMultiplicator);
        if (!useCalculatedMultiplicator) multiplicator = 1;

        EmbedBuilder eb = DBUser.addFishingValues(locale, server, player, 0, (long) Math.ceil(coinsWon * multiplicator * BONUS_MULTIPLICATOR));
        if (coinsInput > 0) channel.sendMessage(eb);
    }

    EmbedBuilder addRetryOption(EmbedBuilder eb) throws Throwable {
        addReactionListener(getReactionMessage());
        message.addReaction(RETRY_EMOJI);
        eb.addField(Tools.getEmptyCharacter(), TextManager.getString(locale, TextManager.COMMANDS, "casino_retry", RETRY_EMOJI));
        deleteOnTimeOut = false;
        return eb;
    }

    public void onReactionAddRetry(SingleReactionEvent event) throws Throwable {
        if (!active && event.getEmoji().isUnicodeEmoji() && event.getEmoji().asUnicodeEmoji().get().equalsIgnoreCase(RETRY_EMOJI)) {
            removeReactionListener(getReactionMessage());

            Command command = CommandManager.createCommandByClass(this.getClass(), locale, prefix);
            command.setReactionUserID(event.getUser().getId());
            command.setWithLoadingBar(false);

            RunningCommandManager.getInstance().remove(event.getUser(), command.getTrigger());

            CommandManager.manage(createEvent, command, String.valueOf(coinsInput));
        }
    }

    @Override
    public void onReactionAdd(SingleReactionEvent event) throws Throwable {
        onReactionAddRetry(event);
    }

    @Override
    public Message getReactionMessage() {
        return message;
    }

    @Override
    public void onReactionTimeOut(Message message) throws Throwable {}
}
