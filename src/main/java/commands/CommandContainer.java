package commands;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import commands.listeners.OnAlertListener;
import commands.listeners.OnStaticReactionAddListener;
import commands.listeners.OnStaticReactionRemoveListener;
import commands.runnables.aitoyscategory.*;
import commands.runnables.birthdaycategory.BirthdayCommand;
import commands.runnables.birthdaycategory.BirthdayConfigCommand;
import commands.runnables.birthdaycategory.BirthdayListCommand;
import commands.runnables.casinocategory.*;
import commands.runnables.configurationcategory.*;
import commands.runnables.externalcategory.*;
import commands.runnables.fisherycategory.*;
import commands.runnables.fisherysettingscategory.*;
import commands.runnables.gimmickscategory.*;
import commands.runnables.informationcategory.*;
import commands.runnables.interactionscategory.*;
import commands.runnables.invitetrackingcategory.InviteTrackingCommand;
import commands.runnables.invitetrackingcategory.InvitesCommand;
import commands.runnables.invitetrackingcategory.InvitesManageCommand;
import commands.runnables.invitetrackingcategory.InvitesTopCommand;
import commands.runnables.moderationcategory.*;
import commands.runnables.nsfwcategory.*;
import commands.runnables.nsfwinteractionscategory.*;
import commands.runnables.splatoon2category.MapsCommand;
import commands.runnables.splatoon2category.SalmonCommand;
import commands.runnables.splatoon2category.SplatnetCommand;
import commands.runnables.utilitycategory.*;
import constants.Settings;
import core.MainLogger;
import core.Program;
import core.utils.ExceptionUtil;

import java.time.Duration;
import java.util.*;

public class CommandContainer {

    private static final HashMap<String, Class<? extends Command>> commandMap = new HashMap<>();
    private static final HashMap<Category, ArrayList<Class<? extends Command>>> commandCategoryMap = new HashMap<>();
    private static final ArrayList<Class<? extends OnStaticReactionAddListener>> staticReactionAddCommands = new ArrayList<>();
    private static final ArrayList<Class<? extends OnStaticReactionRemoveListener>> staticReactionRemoveCommands = new ArrayList<>();
    private static final ArrayList<Class<? extends OnAlertListener>> trackerCommands = new ArrayList<>();

    private static final HashMap<ListenerKey, Cache<Long, CommandListenerMeta<?>>> listenerMap = new HashMap<>();

    private static int commandStuckCounter = 0;

    static {
        final ArrayList<Class<? extends Command>> commandList = new ArrayList<>();

        //GIMMICKS
        commandList.add(RollCommand.class);
        commandList.add(FortuneCommand.class);
        commandList.add(KiraCommand.class);
        commandList.add(TriggerCommand.class);
        commandList.add(RainbowCommand.class);
        commandList.add(ShipCommand.class);
        commandList.add(QuoteCommand.class);
        commandList.add(SayCommand.class);
        commandList.add(TopicCommand.class);
        commandList.add(EveryoneCommand.class);
        commandList.add(SmashOrPassCommand.class);

        //AI TOYS
        commandList.add(ImitateCommand.class);
        commandList.add(Txt2ImgCommand.class);
        commandList.add(UpscalerCommand.class);
        commandList.add(TranslateCommand.class);

        //CONFIGURATION
        commandList.add(LanguageCommand.class);
        commandList.add(PrefixCommand.class);
        commandList.add(CommandPermissionsCommand.class);
        commandList.add(WhiteListCommand.class);
        commandList.add(CommandManagementCommand.class);
        commandList.add(NSFWFilterCommand.class);
        commandList.add(SuggestionConfigCommand.class);
        commandList.add(SuggestionManageCommand.class);
        commandList.add(TicketCommand.class);
        commandList.add(CustomConfigCommand.class);
        commandList.add(ReminderManageCommand.class);
        commandList.add(AlertsCommand.class);
        commandList.add(ReactionRolesCommand.class);
        commandList.add(WelcomeCommand.class);
        commandList.add(AutoRolesCommand.class);
        commandList.add(StickyRolesCommand.class);
        commandList.add(AutoChannelCommand.class);
        commandList.add(AutoQuoteCommand.class);
        commandList.add(MemberCountDisplayCommand.class);
        commandList.add(TriggerDeleteCommand.class);
        commandList.add(GiveawayCommand.class);
        commandList.add(CommandChannelShortcutsCommand.class);
        commandList.add(RolePlayBlockCommand.class);
        commandList.add(CustomRolePlayCommand.class);

        //UTILITY
        commandList.add(VoteCommand.class);
        commandList.add(MultiVoteCommand.class);
        commandList.add(AssignRoleCommand.class);
        commandList.add(RevokeRoleCommand.class);
        commandList.add(ReminderCommand.class);
        commandList.add(SuggestionCommand.class);
        commandList.add(CustomCommand.class);
        commandList.add(SetNSFWCommand.class);

        //MODERATION
        commandList.add(ModSettingsCommand.class);
        commandList.add(WarnCommand.class);
        commandList.add(KickCommand.class);
        commandList.add(BanCommand.class);
        commandList.add(NewKickCommand.class);
        commandList.add(NewBanCommand.class);
        commandList.add(UnbanCommand.class);
        commandList.add(WarnLogCommand.class);
        commandList.add(WarnRemoveCommand.class);
        commandList.add(MuteCommand.class);
        commandList.add(UnmuteCommand.class);
        commandList.add(JailCommand.class);
        commandList.add(UnjailCommand.class);
        commandList.add(InviteFilterCommand.class);
        commandList.add(WordFilterCommand.class);
        commandList.add(ClearCommand.class);
        commandList.add(FullClearCommand.class);

        //INFORMATION
        commandList.add(HelpCommand.class);
        commandList.add(DashboardCommand.class);
        commandList.add(PremiumCommand.class);
        commandList.add(FAQCommand.class);
        commandList.add(ServerInfoCommand.class);
        commandList.add(ChannelInfoCommand.class);
        commandList.add(UserInfoCommand.class);
        commandList.add(AvatarCommand.class);
        commandList.add(CommandUsagesCommand.class);
        commandList.add(PingCommand.class);
        commandList.add(NewCommand.class);
        commandList.add(StatsCommand.class);
        commandList.add(AddCommand.class);
        commandList.add(UpvoteCommand.class);
        commandList.add(BotLogsCommand.class);

        //FISHERY SETTINGS
        commandList.add(FisheryCommand.class);
        commandList.add(FisheryRolesCommand.class);
        commandList.add(VCTimeCommand.class);
        commandList.add(FisheryManageCommand.class);
        commandList.add(TreasureCommand.class);
        commandList.add(PowerUpCommand.class);
        commandList.add(AutoClaimCommand.class);
        commandList.add(AutoWorkCommand.class);
        commandList.add(AutoSellCommand.class);

        //FISHERY
        commandList.add(AccountCommand.class);
        commandList.add(GearCommand.class);
        commandList.add(CooldownsCommand.class);
        commandList.add(DailyCommand.class);
        commandList.add(WorkCommand.class);
        commandList.add(ClaimCommand.class);
        commandList.add(ExchangeRateCommand.class);
        commandList.add(SellCommand.class);
        commandList.add(BuyCommand.class);
        commandList.add(TopCommand.class);
        commandList.add(GiveCommand.class);
        commandList.add(SurveyCommand.class);
        commandList.add(StocksCommand.class);

        //CASINO
        commandList.add(CasinoStatsCommand.class);
        commandList.add(CoinFlipCommand.class);
        commandList.add(HangmanCommand.class);
        commandList.add(SlotCommand.class);
        commandList.add(BlackjackCommand.class);
        commandList.add(QuizCommand.class);
        commandList.add(AnimeQuizCommand.class);
        commandList.add(TowerCommand.class);
        commandList.add(BingoCommand.class);
        commandList.add(BombCommand.class);
        commandList.add(MatchingCardsCommand.class);

        //INVITE TRACKING
        commandList.add(InviteTrackingCommand.class);
        commandList.add(InvitesCommand.class);
        commandList.add(InvitesTopCommand.class);
        commandList.add(InvitesManageCommand.class);

        //BIRTHDAYS
        commandList.add(BirthdayConfigCommand.class);
        commandList.add(BirthdayCommand.class);
        commandList.add(BirthdayListCommand.class);

        //INTERACTIONS
        commandList.add(AngryCommand.class);
        commandList.add(AwkwardCommand.class);
        commandList.add(BegCommand.class);
        commandList.add(BlushCommand.class);
        commandList.add(BoredCommand.class);
        commandList.add(CryCommand.class);
        commandList.add(DabCommand.class);
        commandList.add(DanceCommand.class);
        commandList.add(DrinkCommand.class);
        commandList.add(FacepalmCommand.class);
        commandList.add(JumpCommand.class);
        commandList.add(LaughCommand.class);
        commandList.add(NervousCommand.class);
        commandList.add(NoCommand.class);
        commandList.add(NomCommand.class);
        commandList.add(NoseBleedCommand.class);
        commandList.add(PoutCommand.class);
        commandList.add(RunCommand.class);
        commandList.add(ShrugCommand.class);
        commandList.add(SipCommand.class);
        commandList.add(SingCommand.class);
        commandList.add(SleepCommand.class);
        commandList.add(SmileCommand.class);
        commandList.add(SmugCommand.class);
        commandList.add(StareCommand.class);
        commandList.add(YawnCommand.class);
        commandList.add(YesCommand.class);

        commandList.add(ArrestCommand.class);
        commandList.add(BakaCommand.class);
        commandList.add(BiteCommand.class);
        commandList.add(BonkCommand.class);
        commandList.add(CuddleCommand.class);
        commandList.add(HighfiveCommand.class);
        commandList.add(HugCommand.class);
        commandList.add(KissCommand.class);
        commandList.add(LapSitCommand.class);
        commandList.add(LickCommand.class);
        commandList.add(LoveCommand.class);
        commandList.add(MarryCommand.class);
        commandList.add(MassageCommand.class);
        commandList.add(MerkelCommand.class);
        commandList.add(PatCommand.class);
        commandList.add(PokeCommand.class);
        commandList.add(PunchCommand.class);
        commandList.add(RewardCommand.class);
        commandList.add(SlapCommand.class);
        commandList.add(SquishCommand.class);
        commandList.add(StealCommand.class);
        commandList.add(ThrowCommand.class);
        commandList.add(TickleCommand.class);
        commandList.add(WaveCommand.class);
        commandList.add(YaoiCuddleCommand.class);
        commandList.add(YaoiHugCommand.class);
        commandList.add(YaoiKissCommand.class);
        commandList.add(YeetCommand.class);
        commandList.add(YuriCuddleCommand.class);
        commandList.add(YuriHugCommand.class);
        commandList.add(YuriKissCommand.class);

        //NSFW INTERACTIONS
        commandList.add(SixtyNineCommand.class);
        commandList.add(AssFuckCommand.class);
        commandList.add(AssGrabCommand.class);
        commandList.add(BlowjobCommand.class);
        commandList.add(BondageCommand.class);
        commandList.add(BoobsGrabCommand.class);
        commandList.add(BoobSuckCommand.class);
        commandList.add(CreampieCommand.class);
        commandList.add(CumCommand.class);
        commandList.add(DickRideCommand.class);
        commandList.add(FaceSitCommand.class);
        commandList.add(FingerCommand.class);
        commandList.add(FootjobCommand.class);
        commandList.add(FuckCommand.class);
        commandList.add(FurryFuckCommand.class);
        commandList.add(HandjobCommand.class);
        commandList.add(LeashCommand.class);
        commandList.add(MasturbateCommand.class);
        commandList.add(PussyEatCommand.class);
        commandList.add(SpankCommand.class);
        commandList.add(StripCommand.class);
        commandList.add(TittyFuckCommand.class);
        commandList.add(YaoiFuckCommand.class);
        commandList.add(YuriFuckCommand.class);
        commandList.add(BathroomFuckCommand.class);

        //EXTERNAL
        commandList.add(RedditCommand.class);
        commandList.add(MemeCommand.class);
        commandList.add(WholesomeCommand.class);
        commandList.add(YouTubeCommand.class);
        commandList.add(TwitchCommand.class);
        commandList.add(OsuCommand.class);
        commandList.add(AnimeNewsCommand.class);
        commandList.add(AnimeReleasesCommand.class);
        commandList.add(MangaUpdatesCommand.class);
        commandList.add(AnilistCommand.class);
        commandList.add(DadJokeCommand.class);
        commandList.add(SafebooruCommand.class);
        commandList.add(SoftYaoiCommand.class);
        commandList.add(SoftYuriCommand.class);
        commandList.add(PixivCommand.class);

        //NSFW
        commandList.add(Txt2HentaiCommand.class);
        commandList.add(Rule34Command.class);
        commandList.add(RealbooruCommand.class);
        commandList.add(E621Command.class);
        commandList.add(DanbooruCommand.class);
        //commandList.add(KonachanCommand.class);
        commandList.add(RealLifePornCommand.class);
        commandList.add(RealLifeThreesomeCommand.class);
        commandList.add(RealLifePovCommand.class);
        commandList.add(RealLifeAnalCommand.class);
        commandList.add(RealLifeAssCommand.class);
        commandList.add(RealLifeBoobsCommand.class);
        commandList.add(RealLifeThighsCommand.class);
        commandList.add(RealLifeCreampieCommand.class);
        commandList.add(RealLifePussy.class);
        commandList.add(RealLifeDick.class);
        commandList.add(RealLifeBlowjobCommand.class);
        commandList.add(RealLifeBDSMCommand.class);
        commandList.add(RealLifeFemaleAbsCommand.class);
        commandList.add(RealLifeCosplayCommand.class);
        commandList.add(FemboyCommand.class);
        commandList.add(RealLifeGayCommand.class);
        commandList.add(RealLifeLesbianCommand.class);
        commandList.add(HentaiCommand.class);
        commandList.add(ThreeDHentaiCommand.class);
        commandList.add(HentaiPovCommand.class);
        commandList.add(HentaiThreesomeCommand.class);
        commandList.add(HentaiAnalCommand.class);
        commandList.add(HentaiCreampieCommand.class);
        commandList.add(HentaiAssCommand.class);
        commandList.add(HentaiBoobsCommand.class);
        commandList.add(HentaiThighsCommand.class);
        commandList.add(HentaiFeetCommand.class);
        commandList.add(HentaiPussy.class);
        commandList.add(HentaiDick.class);
        commandList.add(HentaiBlowjobCommand.class);
        commandList.add(HentaiBDSMCommand.class);
        commandList.add(HentaiFemaleAbsCommand.class);
        commandList.add(AhegaoCommand.class);
        commandList.add(NekoCommand.class);
        commandList.add(FutaCommand.class);
        commandList.add(TrapCommand.class);
        commandList.add(YaoiCommand.class);
        commandList.add(YuriCommand.class);
        commandList.add(BaraCommand.class);
        commandList.add(FurryCommand.class);
        commandList.add(GenshinCommand.class);
        commandList.add(GenshinYaoiCommand.class);

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
        commandList.add(WeaknessTypeCommand.class);
        commandList.add(WeaknessMonCommand.class);
        commandList.add(HeineCommand.class);
        commandList.add(TartagliaNSFWCommand.class);
        commandList.add(DiamondsCommand.class);

        for (Class<? extends Command> clazz : commandList) {
            Command command = CommandManager.createCommandByClass(clazz, Locale.US, "L.");
            if (command.getCommandProperties().onlyPublicVersion() && !Program.publicInstance()) {
                continue;
            }

            addCommand(command.getTrigger(), command);
            for (String str : command.getCommandProperties().aliases()) {
                addCommand(str, command);
            }

            if (command instanceof OnStaticReactionAddListener) {
                staticReactionAddCommands.add(((OnStaticReactionAddListener) command).getClass());
            }
            if (command instanceof OnStaticReactionRemoveListener) {
                staticReactionRemoveCommands.add(((OnStaticReactionRemoveListener) command).getClass());
            }
            if (command.canRunOnGuild(0L, 0L)) {
                if (command instanceof OnAlertListener) {
                    trackerCommands.add(((OnAlertListener) command).getClass());
                }
                addCommandCategoryMap(command);
            }
        }
    }

    private static void addCommandCategoryMap(Command command) {
        ArrayList<Class<? extends Command>> commands = commandCategoryMap.computeIfAbsent(command.getCategory(), e -> new ArrayList<>());
        commands.add(command.getClass());
    }

    private static void addCommand(String trigger, Command command) {
        if (commandMap.containsKey(trigger)) {
            MainLogger.get().error("Duplicate key for \"" + command.getTrigger() + "\"");
        } else {
            commandMap.put(trigger, command.getClass());
        }
    }


    public static HashMap<String, Class<? extends Command>> getCommandMap() {
        return commandMap;
    }

    public static ArrayList<Class<? extends OnStaticReactionAddListener>> getStaticReactionAddCommands() {
        return staticReactionAddCommands;
    }

    public static ArrayList<Class<? extends OnStaticReactionRemoveListener>> getStaticReactionRemoveCommands() {
        return staticReactionRemoveCommands;
    }

    public static ArrayList<Class<? extends OnAlertListener>> getTrackerCommands() {
        return trackerCommands;
    }

    public static HashMap<Category, ArrayList<Class<? extends Command>>> getCommandCategoryMap() {
        return commandCategoryMap;
    }

    public static ArrayList<Class<? extends Command>> getFullCommandList() {
        ArrayList<Class<? extends Command>> fullList = new ArrayList<>();
        getCommandCategoryMap().values()
                .forEach(fullList::addAll);

        return fullList;
    }

    public static synchronized <T> void registerListener(Class<?> clazz, CommandListenerMeta<T> commandListenerMeta) {
        Cache<Long, CommandListenerMeta<?>> cache = listenerMap.computeIfAbsent(
                new ListenerKey(clazz, commandListenerMeta.getCommand().getCommandProperties().enableCacheWipe()),
                e -> {
                    CacheBuilder<Object, Object> cacheBuilder = CacheBuilder.newBuilder();
                    if (commandListenerMeta.getCommand().getCommandProperties().enableCacheWipe()) {
                        cacheBuilder = cacheBuilder.expireAfterWrite(Duration.ofMinutes(Settings.TIME_OUT_MINUTES))
                                .removalListener(event -> {
                                    if (event.getCause() == RemovalCause.EXPIRED) {
                                        ((CommandListenerMeta<?>) event.getValue()).timeOut();
                                    }
                                });
                    }
                    return cacheBuilder.build();
                }
        );

        cache.put(commandListenerMeta.getCommand().getId(), commandListenerMeta);
    }

    public static synchronized void deregisterListeners(Command command) {
        for (Cache<Long, CommandListenerMeta<?>> cache : listenerMap.values()) {
            cache.invalidate(command.getId());
        }
    }

    public static synchronized void deregisterListeners(long messageId) {
        for (Cache<Long, CommandListenerMeta<?>> cache : listenerMap.values()) {
            for (CommandListenerMeta<?> commandListenerMeta : cache.asMap().values()) {
                Command command = commandListenerMeta.getCommand();
                if (messageId == command.getDrawMessageId().orElse(0L)) {
                    cache.invalidate(command.getId());
                    break;
                }
            }
        }
    }

    public static synchronized Collection<CommandListenerMeta<?>> getListeners(Class<?> clazz) {
        ArrayList<CommandListenerMeta<?>> commandListenerList = new ArrayList<>();
        if (listenerMap.containsKey(new ListenerKey(clazz, true))) {
            commandListenerList.addAll(listenerMap.get(new ListenerKey(clazz, true)).asMap().values());
        }
        if (listenerMap.containsKey(new ListenerKey(clazz, false))) {
            commandListenerList.addAll(listenerMap.get(new ListenerKey(clazz, false)).asMap().values());
        }

        return commandListenerList;
    }

    public static synchronized Optional<CommandListenerMeta<?>> getListener(Class<?> clazz, Command command) {
        if (listenerMap.containsKey(new ListenerKey(clazz, true))) {
            Cache<Long, CommandListenerMeta<?>> cache = listenerMap.get(new ListenerKey(clazz, true));
            if (cache.asMap().containsKey(command.getId())) {
                return Optional.ofNullable(cache.getIfPresent(command.getId()));
            }
        }

        if (listenerMap.containsKey(new ListenerKey(clazz, false))) {
            Cache<Long, CommandListenerMeta<?>> cache = listenerMap.get(new ListenerKey(clazz, false));
            if (cache.asMap().containsKey(command.getId())) {
                return Optional.ofNullable(cache.getIfPresent(command.getId()));
            }
        }

        return Optional.empty();
    }

    public static synchronized void cleanUp() {
        listenerMap.values().forEach(Cache::cleanUp);
    }

    public static synchronized void refreshListeners(Command command) {
        for (Cache<Long, CommandListenerMeta<?>> cache : listenerMap.values()) {
            CommandListenerMeta<?> meta = cache.getIfPresent(command.getId());
            if (meta != null) {
                cache.put(command.getId(), meta);
            }
        }
    }

    public static synchronized Collection<ListenerKey> getListenerKeys() {
        return listenerMap.keySet();
    }

    public static synchronized int getListenerSize() {
        return (int) listenerMap.values().stream()
                .mapToLong(Cache::size)
                .sum();
    }

    public static void addCommandTerminationStatus(Command command, Thread commandThread, boolean stuck) {
        if (stuck) {
            Exception e = ExceptionUtil.generateForStack(commandThread);
            MainLogger.get().error("Command \"{}\" stuck (stuck counter: {})", command.getTrigger(), ++commandStuckCounter, e);
            commandThread.interrupt();
        } else {
            commandStuckCounter = Math.max(0, commandStuckCounter - 1);
        }
    }

    public static int getCommandStuckCounter() {
        return commandStuckCounter;
    }


    public static class ListenerKey {

        private final Class<?> clazz;
        private final boolean withCacheWipe;

        public ListenerKey(Class<?> clazz, boolean withCacheWipe) {
            this.clazz = clazz;
            this.withCacheWipe = withCacheWipe;
        }

        public Class<?> getClazz() {
            return clazz;
        }

        public boolean isWithCacheWipe() {
            return withCacheWipe;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ListenerKey that = (ListenerKey) o;
            return withCacheWipe == that.withCacheWipe && Objects.equals(clazz, that.clazz);
        }

        @Override
        public int hashCode() {
            return Objects.hash(clazz, withCacheWipe);
        }

    }

}
