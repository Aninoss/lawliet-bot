package CommandSupporters;

import CommandListeners.*;
import Commands.BotManagement.*;
import Commands.Casino.*;
import Commands.Emotes.*;
import Commands.External.*;
import Commands.General.*;
import Commands.BotOwner.*;
import Commands.Interactions.*;
import Commands.Moderation.*;
import Commands.NSFW.*;
import Commands.PowerPlant.*;
import Commands.ServerManagement.*;
import Commands.Splatoon2.*;

import java.lang.reflect.InvocationTargetException;
import java.time.Instant;
import java.util.*;

public class CommandContainer {
    private static CommandContainer ourInstance = new CommandContainer();
    public static CommandContainer getInstance() {
        return ourInstance;
    }

    private HashMap<String,Class> commands;
    private ArrayList<onReactionAddStatic> staticReactionAddCommands;
    private ArrayList<onReactionRemoveStatic> staticReactionRemoveCommands;
    private ArrayList<onTrackerRequestListener> trackerCommands;
    private ArrayList<Command> commandsReaction;
    private ArrayList<Command> commandsMessageForward;
    private ArrayList<Class> commandList;
    private Instant lastCommandUsage;

    private CommandContainer() {
        updateLastCommandUsage();
        commands = new HashMap<>();
        staticReactionAddCommands = new ArrayList<>();
        staticReactionRemoveCommands = new ArrayList<>();
        trackerCommands = new ArrayList<>();
        commandsReaction = new ArrayList<>();
        commandsMessageForward = new ArrayList<>();
        commandList = new ArrayList<>();

        //GENERAL
        commandList.add(CoinFlipCommand.class);
        commandList.add(RollCommand.class);
        commandList.add(FortuneCommand.class);
        commandList.add(KiraCommand.class);
        commandList.add(TriggerCommand.class);
        commandList.add(RainbowCommand.class);
        commandList.add(ShipCommand.class);
        commandList.add(QuoteCommand.class);
        commandList.add(AvatarCommand.class);

        //BOT MANAGEMENT
        commandList.add(HelpCommand.class);
        commandList.add(LanguageCommand.class);
        commandList.add(WhiteListCommand.class);
        commandList.add(PrefixCommand.class);
        commandList.add(StatsCommand.class);
        commandList.add(TipsCommand.class);
        commandList.add(NewCommand.class);
        commandList.add(TrackerCommand.class);
        commandList.add(PingCommand.class);
        commandList.add(ReportCommand.class);
        commandList.add(InviteCommand.class);
        commandList.add(UpvoteCommand.class);
        commandList.add(DonateCommand.class);

        //SERVER MANAGEMENT
        commandList.add(ReactionRolesCommand.class);
        commandList.add(WelcomeCommand.class);
        commandList.add(AutoRolesCommand.class);
        commandList.add(AutoChannelCommand.class);
        commandList.add(VoteCommand.class);

        //MODERATION
        commandList.add(ModSettingsCommand.class);
        commandList.add(WarnCommand.class);
        commandList.add(KickCommand.class);
        commandList.add(BanCommand.class);
        commandList.add(SelfPromotionBlockCommand.class);
        commandList.add(BannedWordsCommand.class);
        commandList.add(AutoKickCommand.class);
        commandList.add(ClearCommand.class);

        //BOT OWNER
        commandList.add(EmojisCommand.class);
        commandList.add(SendCommand.class);

        //POWER PLANT
        commandList.add(PowerPlantSetupCommand.class);
        commandList.add(SellCommand.class);
        commandList.add(BuyCommand.class);
        commandList.add(DailyCommand.class);
        commandList.add(ClaimCommand.class);
        commandList.add(AccountCommand.class);
        commandList.add(TopCommand.class);
        commandList.add(GiveCommand.class);
        commandList.add(SurveyCommand.class);

        //CASINO
        commandList.add(HangmanCommand.class);
        commandList.add(SlotCommand.class);
        commandList.add(BlackjackCommand.class);
        commandList.add(QuizCommand.class);
        commandList.add(AnimeQuizCommand.class);

        //EMOTES
        commandList.add(DabCommand.class);
        commandList.add(AwkwardCommand.class);
        commandList.add(YesCommand.class);
        commandList.add(NoCommand.class);
        commandList.add(CryCommand.class);
        commandList.add(DanceCommand.class);

        //INTERACTIONS
        commandList.add(MerkelCommand.class);
        commandList.add(KillCommand.class);
        commandList.add(PunchCommand.class);
        commandList.add(SlapCommand.class);
        commandList.add(BakaCommand.class);
        commandList.add(StealCommand.class);
        commandList.add(ThrowCommand.class);
        commandList.add(BullyCommand.class);
        commandList.add(NotWorkCommand.class);
        commandList.add(EveryoneCommand.class);
        commandList.add(BiteCommand.class);
        commandList.add(NomCommand.class);
        commandList.add(PokeCommand.class);
        commandList.add(FishCommand.class);
        commandList.add(TickleCommand.class);
        commandList.add(WaveCommand.class);
        commandList.add(HighfiveCommand.class);
        commandList.add(PatCommand.class);
        commandList.add(RewardCommand.class);
        commandList.add(HugCommand.class);
        commandList.add(CuddleCommand.class);
        commandList.add(MassageCommand.class);
        commandList.add(LickCommand.class);
        commandList.add(LoveCommand.class);
        commandList.add(KissCommand.class);
        commandList.add(YaoiKissCommand.class);
        commandList.add(YuriKissCommand.class);
        commandList.add(MarryCommand.class);
        commandList.add(FuckCommand.class);
        commandList.add(YaoiFuckCommand.class);
        commandList.add(YuriFuckCommand.class);
        commandList.add(YiffCommand.class);

        //EXTERNAL
        commandList.add(RedditCommand.class);
        commandList.add(MemeCommand.class);
        commandList.add(WholesomeCommand.class);
        commandList.add(DadJokeCommand.class);
        commandList.add(AnimeNewsCommand.class);
        commandList.add(IncreaseResolutionCommand.class);
        commandList.add(SafebooruCommand.class);
        commandList.add(SoftYaoiCommand.class);
        commandList.add(SoftYuriCommand.class);

        //NSFW
        commandList.add(Rule34Command.class);
        commandList.add(GelbooruCommand.class);
        commandList.add(RealbooruCommand.class);
        commandList.add(RealLifePornCommand.class);
        commandList.add(HentaiCommand.class);
        commandList.add(YaoiCommand.class);
        commandList.add(YuriCommand.class);
        commandList.add(GimmeHentaiCommand.class);

        //SPLATOON
        commandList.add(MapsCommand.class);
        commandList.add(SalmonCommand.class);
        commandList.add(SplatnetCommand.class);

        for(Class clazz: commandList) {
            try {
                Command command = CommandManager.createCommandByClass(clazz);
                commands.put(command.getTrigger(), clazz);
                for(String str: command.getAliases()) commands.put(str, clazz);
                if (command instanceof onReactionAddStatic) staticReactionAddCommands.add((onReactionAddStatic)command);
                if (command instanceof onReactionRemoveStatic) staticReactionRemoveCommands.add((onReactionRemoveStatic)command);
                if (command instanceof onTrackerRequestListener) trackerCommands.add((onTrackerRequestListener)command);
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        }
    }

    public HashMap<String,Class> getCommands() {
        return commands;
    }

    public ArrayList<onReactionAddStatic> getStaticReactionAddCommands() {
        return staticReactionAddCommands;
    }

    public ArrayList<onReactionRemoveStatic> getStaticReactionRemoveCommands() {
        return staticReactionRemoveCommands;
    }

    public int getActivitiesSize() {
        ArrayList<Command> commandList = new ArrayList<>();

        for(Command command: getReactionInstances()) {
            if (!commandList.contains(command)) commandList.add(command);
        }
        for(Command command: getMessageForwardInstances()) {
            if (!commandList.contains(command)) commandList.add(command);
        }

        return commandList.size();
    }

    public ArrayList<Command> getReactionInstances() {
        return (ArrayList<Command>) commandsReaction.clone();
    }

    public ArrayList<Command> getMessageForwardInstances() {
        return (ArrayList<Command>) commandsMessageForward.clone();
    }

    public void addReactionListener(Command commandParent) {
        if (!commandsReaction.contains(commandParent))
            commandsReaction.add(commandParent);
    }

    public void addMessageForwardListener(Command commandParent) {
        if (!commandsMessageForward.contains(commandParent))
            commandsMessageForward.add(commandParent);
    }

    public void removeReactionListener(Command commandParent) {
        commandsReaction.remove(commandParent);
    }

    public void removeForwarder(Command commandParent) {
        commandsMessageForward.remove(commandParent);
    }

    public boolean reactionListenerContains(Command commandParent) {
        return commandsReaction.contains(commandParent);
    }

    public boolean forwarderContains(Command commandParent) {
        return commandsMessageForward.contains(commandParent);
    }

    public ArrayList<onTrackerRequestListener> getTrackerCommands() {
        return trackerCommands;
    }

    public ArrayList<Class> getCommandList() {
        return commandList;
    }

    public Instant getLastCommandUsage() {
        return lastCommandUsage;
    }

    public void updateLastCommandUsage() {
        lastCommandUsage = Instant.now();
    }
}
