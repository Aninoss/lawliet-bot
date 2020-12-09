package commands;

import commands.listeners.*;
import commands.runnables.aitoyscategory.ColorCommand;
import commands.runnables.aitoyscategory.ImitateCommand;
import commands.runnables.aitoyscategory.Waifu2xCommand;
import commands.runnables.casinocategory.*;
import commands.runnables.configurationcategory.CommandManagementCommand;
import commands.runnables.configurationcategory.LanguageCommand;
import commands.runnables.configurationcategory.NSFWFilterCommand;
import commands.runnables.configurationcategory.PrefixCommand;
import commands.runnables.emotescategory.*;
import commands.runnables.externalcategory.*;
import commands.runnables.fisherycategory.*;
import commands.runnables.fisherysettingscategory.*;
import commands.runnables.gimmickscategory.*;
import commands.runnables.informationcategory.*;
import commands.runnables.interactionscategory.*;
import commands.runnables.moderationcategory.*;
import commands.runnables.nsfwcategory.*;
import commands.runnables.splatoon2category.MapsCommand;
import commands.runnables.splatoon2category.SalmonCommand;
import commands.runnables.splatoon2category.SplatnetCommand;
import commands.runnables.utilitycategory.*;
import core.NewApiCollection;
import org.javacord.api.DiscordApi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

public class CommandContainer {

    private static final CommandContainer ourInstance = new CommandContainer();
    public static CommandContainer getInstance() {
        return ourInstance;
    }

    final Logger LOGGER = LoggerFactory.getLogger(CommandContainer.class);

    private final HashMap<String, Class<? extends Command>> commandMap = new HashMap<>();
    private final HashMap<String, ArrayList<Class<? extends Command>>> commandCategoryMap = new HashMap<>();
    private final ArrayList<Class<? extends OnReactionAddStaticListener>> staticReactionAddCommands = new ArrayList<>();
    private final ArrayList<Class<? extends OnReactionRemoveStaticListener>> staticReactionRemoveCommands = new ArrayList<>();
    private final ArrayList<Class<? extends OnTrackerRequestListener>> trackerCommands = new ArrayList<>();
    private final ArrayList<Command> commandsReaction = new ArrayList<>();
    private final ArrayList<Command> commandsMessageForward = new ArrayList<>();

    private CommandContainer() {
        final ArrayList<Class<? extends Command>> commandList = new ArrayList<>();

        //GIMMICKS
        commandList.add(RollCommand.class);
        commandList.add(FortuneCommand.class);
        commandList.add(KiraCommand.class);
        commandList.add(TriggerCommand.class);
        commandList.add(RainbowCommand.class);
        commandList.add(ShipCommand.class);
        commandList.add(QuoteCommand.class);
        commandList.add(AvatarCommand.class);
        commandList.add(SayCommand.class);
        commandList.add(VoteCommand.class);
        commandList.add(TopicCommand.class);

        //AI TOYS
        commandList.add(ImitateCommand.class);
        commandList.add(Waifu2xCommand.class);
        commandList.add(ColorCommand.class);

        //CONFIGURATION
        commandList.add(CommandManagementCommand.class);
        commandList.add(LanguageCommand.class);
        commandList.add(PrefixCommand.class);
        commandList.add(NSFWFilterCommand.class);

        //UTILITY
        commandList.add(WhiteListCommand.class);
        commandList.add(AlertsCommand.class);
        commandList.add(ReactionRolesCommand.class);
        commandList.add(WelcomeCommand.class);
        commandList.add(AutoRolesCommand.class);
        commandList.add(AutoChannelCommand.class);
        commandList.add(AutoQuoteCommand.class);
        commandList.add(AssignRoleCommand.class);
        commandList.add(RevokeRoleCommand.class);
        commandList.add(MemberCountDisplayCommand.class);
        commandList.add(TriggerDeleteCommand.class);
        commandList.add(ReminderCommand.class);
        commandList.add(GiveawayCommand.class);
        commandList.add(SuggestionConfigCommand.class);
        commandList.add(SuggestionCommand.class);

        //MODERATION
        commandList.add(ModSettingsCommand.class);
        commandList.add(WarnCommand.class);
        commandList.add(KickCommand.class);
        commandList.add(BanCommand.class);
        commandList.add(UnbanCommand.class);
        commandList.add(WarnLogCommand.class);
        commandList.add(WarnRemoveCommand.class);
        commandList.add(ChannelMuteCommand.class);
        commandList.add(ChannelUnmuteCommand.class);
        commandList.add(InviteFilterCommand.class);
        commandList.add(WordFilterCommand.class);
        commandList.add(ClearCommand.class);
        commandList.add(FullClearCommand.class);

        //INFORMATION
        commandList.add(HelpCommand.class);
        commandList.add(PatreonCommand.class);
        commandList.add(FAQCommand.class);
        commandList.add(ServerInfoCommand.class);
        commandList.add(ChannelInfoCommand.class);
        commandList.add(UserInfoCommand.class);
        commandList.add(CommandUsagesCommand.class);
        commandList.add(PingCommand.class);
        commandList.add(NewCommand.class);
        commandList.add(StatsCommand.class);
        commandList.add(InviteCommand.class);
        commandList.add(UpvoteCommand.class);

        //FISHERY SETTINGS
        commandList.add(FisheryCommand.class);
        commandList.add(FisheryRolesCommand.class);
        commandList.add(VCTimeCommand.class);
        commandList.add(FisheryManageCommand.class);
        commandList.add(TreasureCommand.class);
        commandList.add(AutoClaimCommand.class);

        //FISHERY
        commandList.add(AccountCommand.class);
        commandList.add(GearCommand.class);
        commandList.add(DailyCommand.class);
        commandList.add(ClaimCommand.class);
        commandList.add(ExchangeRateCommand.class);
        commandList.add(ExchangeRateForecastCommand.class);
        commandList.add(SellCommand.class);
        commandList.add(BuyCommand.class);
        commandList.add(TopCommand.class);
        commandList.add(GiveCommand.class);
        commandList.add(SurveyCommand.class);

        //CASINO
        commandList.add(CoinFlipCommand.class);
        commandList.add(HangmanCommand.class);
        commandList.add(SlotCommand.class);
        commandList.add(BlackjackCommand.class);
        commandList.add(QuizCommand.class);
        commandList.add(AnimeQuizCommand.class);
        commandList.add(TowerCommand.class);

        //EMOTES
        commandList.add(AngryCommand.class);
        commandList.add(AwkwardCommand.class);
        commandList.add(BlushCommand.class);
        commandList.add(CryCommand.class);
        commandList.add(DabCommand.class);
        commandList.add(DanceCommand.class);
        commandList.add(FacepalmCommand.class);
        commandList.add(LaughCommand.class);
        commandList.add(NoCommand.class);
        commandList.add(NoseBleedCommand.class);
        commandList.add(RunCommand.class);
        commandList.add(ShrugCommand.class);
        commandList.add(SipCommand.class);
        commandList.add(SleepCommand.class);
        commandList.add(SmileCommand.class);
        commandList.add(SmugCommand.class);
        commandList.add(StareCommand.class);
        commandList.add(YawnCommand.class);
        commandList.add(YesCommand.class);

        //INTERACTIONS
        commandList.add(BakaCommand.class);
        commandList.add(BiteCommand.class);
        commandList.add(BullyCommand.class);
        commandList.add(CuddleCommand.class);
        commandList.add(EveryoneCommand.class);
        commandList.add(FuckCommand.class);
        commandList.add(FurryFuckCommand.class);
        commandList.add(HighfiveCommand.class);
        commandList.add(HugCommand.class);
        commandList.add(KillCommand.class);
        commandList.add(KissCommand.class);
        commandList.add(LickCommand.class);
        commandList.add(LoveCommand.class);
        commandList.add(MarryCommand.class);
        commandList.add(MassageCommand.class);
        commandList.add(MerkelCommand.class);
        commandList.add(NomCommand.class);
        commandList.add(PatCommand.class);
        commandList.add(PokeCommand.class);
        commandList.add(PunchCommand.class);
        commandList.add(RewardCommand.class);
        commandList.add(SlapCommand.class);
        commandList.add(SpankCommand.class);
        commandList.add(StealCommand.class);
        commandList.add(ThrowCommand.class);
        commandList.add(TickleCommand.class);
        commandList.add(WaveCommand.class);
        commandList.add(YaoiCuddleCommand.class);
        commandList.add(YaoiFuckCommand.class);
        commandList.add(YaoiHugCommand.class);
        commandList.add(YaoiKissCommand.class);
        commandList.add(YuriCuddleCommand.class);
        commandList.add(YuriFuckCommand.class);
        commandList.add(YuriHugCommand.class);
        commandList.add(YuriKissCommand.class);

        //EXTERNAL
        commandList.add(RedditCommand.class);
        commandList.add(MemeCommand.class);
        commandList.add(WholesomeCommand.class);
        commandList.add(TwitchCommand.class);
        commandList.add(YouTubeMP3Command.class);
        commandList.add(OsuCommand.class);
        commandList.add(AnimeNewsCommand.class);
        commandList.add(AnimeReleasesCommand.class);
        commandList.add(DadJokeCommand.class);
        commandList.add(SafebooruCommand.class);
        commandList.add(SoftYaoiCommand.class);
        commandList.add(SoftYuriCommand.class);

        //NSFW
        commandList.add(Rule34Command.class);
        commandList.add(GelbooruCommand.class);
        commandList.add(RealbooruCommand.class);
        commandList.add(FurryCommand.class);
        commandList.add(RealLifePornCommand.class);
        commandList.add(RealLifeBoobsCommand.class);
        commandList.add(RealLifeAssCommand.class);
        commandList.add(HentaiCommand.class);
        commandList.add(HentaiBlowjobCommand.class);
        commandList.add(AhegaoCommand.class);
        commandList.add(TrapCommand.class);
        commandList.add(FutaCommand.class);
        commandList.add(NekoCommand.class);
        commandList.add(YaoiCommand.class);
        commandList.add(YuriCommand.class);

        //SPLATOON
        commandList.add(MapsCommand.class);
        commandList.add(SalmonCommand.class);
        commandList.add(SplatnetCommand.class);

        //PRIVATE
        commandList.add(NibbleCommand.class);
        commandList.add(RosesCommand.class);
        commandList.add(WebgateCommand.class);
        commandList.add(CelebrateCommand.class);
        commandList.add(PokemonCommand.class);

        for(Class<? extends Command> clazz: commandList) {
            try {
                Command command = CommandManager.createCommandByClass(clazz, Locale.US, "L.");
                addCommand(command.getTrigger(), command);
                for(String str: command.getAliases()) addCommand(str, command);

                if (command instanceof OnReactionAddStaticListener) staticReactionAddCommands.add(((OnReactionAddStaticListener)command).getClass());
                if (command instanceof OnReactionRemoveStaticListener) staticReactionRemoveCommands.add(((OnReactionRemoveStaticListener)command).getClass());
                if (command instanceof OnTrackerRequestListener) trackerCommands.add(((OnTrackerRequestListener)command).getClass());

                if (command.canRunOnServer(0L, 0L))
                    addCommandCategoryMap(command);
            } catch (IllegalAccessException | InstantiationException | InvocationTargetException e) {
                LOGGER.error("Could not create class", e);
            }
        }

        NewApiCollection.getInstance().addShardDisconnectConsumer(this::clearShard);
    }

    public void clearShard(int shardId) {
        for(Command command: new ArrayList<>(commandsReaction)) {
            if (command != null) {
                DiscordApi api;
                if (command instanceof OnReactionAddListener)
                    api = ((OnReactionAddListener) command).getReactionMessage().getApi();
                else api = command.getNavigationMessage().getApi();
                if (api.getCurrentShard() == shardId) {
                    command.stopCountdown();
                    commandsReaction.remove(command);
                }
            }
        }
        for(Command command: new ArrayList<>(commandsMessageForward)) {
            if (command != null) {
                DiscordApi api;
                if (command instanceof OnForwardedRecievedListener)
                    api = ((OnForwardedRecievedListener) command).getForwardedMessage().getApi();
                else api = command.getNavigationMessage().getApi();
                if (api.getCurrentShard() == shardId) {
                    command.stopCountdown();
                    commandsReaction.remove(command);
                }
            }
        }
    }

    private void addCommandCategoryMap(Command command) {
        ArrayList<Class<? extends Command>> commands = commandCategoryMap.computeIfAbsent(command.getCategory(), e -> new ArrayList<>());
        commands.add(command.getClass());
    }

    private void addCommand(String trigger, Command command) {
        if (commandMap.containsKey(trigger)) LOGGER.error("Dupicate key for \"" + command.getTrigger() + "\"");
        else commandMap.put(trigger, command.getClass());
    }


    public HashMap<String, Class<? extends Command>> getCommandMap() {
        return commandMap;
    }

    public ArrayList<Class<? extends OnReactionAddStaticListener>> getStaticReactionAddCommands() {
        return staticReactionAddCommands;
    }

    public ArrayList<Class<? extends OnReactionRemoveStaticListener>> getStaticReactionRemoveCommands() {
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
        return new ArrayList<>(commandsReaction);
    }

    public ArrayList<Command> getMessageForwardInstances() {
        return new ArrayList<>(commandsMessageForward);
    }

    public void addReactionListener(Command commandParent) {
        if (commandParent != null && !commandsReaction.contains(commandParent))
            commandsReaction.add(commandParent);
    }

    public void addMessageForwardListener(Command commandParent) {
        if (commandParent != null && !commandsMessageForward.contains(commandParent))
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

    public ArrayList<Class<? extends OnTrackerRequestListener>> getTrackerCommands() {
        return trackerCommands;
    }

    public HashMap<String, ArrayList<Class<? extends Command>>> getCommandCategoryMap() {
        return commandCategoryMap;
    }

    public ArrayList<Class<? extends Command>> getFullCommandList() {
        ArrayList<Class<? extends Command>> fullList = new ArrayList<>();
        CommandContainer.getInstance().getCommandCategoryMap().values()
                .forEach(fullList::addAll);

        return fullList;
    }

}
