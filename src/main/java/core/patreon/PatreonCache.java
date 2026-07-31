package core.patreon;

import constants.AssetIds;
import constants.ExternalLinks;
import core.Program;
import core.ShardManager;
import core.cache.SingleCache;
import core.utils.ComponentsUtil;
import core.utils.JDAUtil;
import events.sync.SendEvent;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.components.container.ContainerChildComponent;
import net.dv8tion.jda.api.components.separator.Separator;
import net.dv8tion.jda.api.components.textdisplay.TextDisplay;
import net.dv8tion.jda.api.components.tree.MessageComponentTree;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutionException;

public class PatreonCache extends SingleCache<PatreonData> {

    private static final PatreonCache ourInstance = new PatreonCache();

    public static PatreonCache getInstance() {
        return ourInstance;
    }

    private PatreonCache() {
    }

    public boolean hasPremium(long userId, boolean requiresOld) {
        if (userId == AssetIds.OWNER_USER_ID) {
            return true;
        }

        PatreonData patreonData = getAsync();
        if (patreonData == null ||
                (!patreonData.getOldUserList().contains(userId) && requiresOld)
        ) {
            return false;
        }

        return patreonData.getUserTierMap().containsKey(userId);
    }

    public boolean isUnlocked(long guildId) {
        PatreonData patreonData = getAsync();
        return !Program.publicInstance() || (patreonData != null && patreonData.getGuildList().contains(guildId));
    }

    public void requestUpdate() {
        SendEvent.sendEmpty("PATREON_FETCH");
    }

    @Override
    protected PatreonData fetchValue() {
        if (Program.productionMode()) {
            try {
                return SendEvent.sendRequestPatreon().get();
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        } else {
            return new PatreonData(new HashMap<>(), new HashSet<>(), new HashSet<>());
        }
    }

    @Override
    protected int getRefreshRateMinutes() {
        return 5;
    }

    public static PatreonData patreonDataFromJson(JSONObject responseJson) {
        HashMap<Long, Integer> userTierMap = new HashMap<>();
        HashSet<Long> unlockedGuilds = new HashSet<>();
        HashSet<Long> oldUsers = new HashSet<>();

        JSONArray usersArray = responseJson.getJSONArray("users");
        for (int i = 0; i < usersArray.length(); i++) {
            JSONObject userJson = usersArray.getJSONObject(i);
            long userId = userJson.getLong("user_id");
            int tier = userJson.getInt("tier");
            if (tier > 0) {
                userTierMap.put(userId, tier);
            }
        }

        JSONArray guildsArray = responseJson.getJSONArray("guilds");
        for (int i = 0; i < guildsArray.length(); i++) {
            unlockedGuilds.add(guildsArray.getLong(i));
        }

        JSONArray oldUsersArray = responseJson.getJSONArray("old_users");
        for (int i = 0; i < oldUsersArray.length(); i++) {
            oldUsers.add(oldUsersArray.getLong(i));
        }

        return new PatreonData(userTierMap, unlockedGuilds, oldUsers);
    }

    public static void sendJoinDm(long userId, PatreonTier patreonTier) {
        ArrayList<ContainerChildComponent> components = new ArrayList<>();
        components.add(TextDisplay.of("Thank you very much for your support! ❤️"));
        if (patreonTier.getUnlocksServers()) {
            components.add(TextDisplay.of("ℹ\uFE0F You need to specify the server you want to unlock in order to run Premium commands:\n> Go to the [Lawliet Premium page](%s) → log into your Discord account → go to the \"Unlock Servers\" section at the bottom of the page.".formatted(ExternalLinks.PREMIUM_WEBSITE)));
        }
        components.add(TextDisplay.of("Your Premium subscription includes an exclusive role on the Lawliet Discord server. If you would like to leave the Lawliet server, please do so via the [Connected Apps settings in your Patreon account](%s) to ensure that you will not be added to the server again after your next payment.".formatted(ExternalLinks.PATREON_DISCORD_SETTINGS)));

        components.add(Separator.createInvisible(Separator.Spacing.SMALL));

        ArrayList<Button> buttons = new ArrayList<>();
        if (patreonTier.getUnlocksServers()) {
            buttons.add(Button.of(ButtonStyle.LINK, ExternalLinks.PREMIUM_COMMANDS_WEBSITE, "Premium Commands"));
        }
        buttons.add(Button.of(ButtonStyle.LINK, ExternalLinks.DEVELOPMENT_VOTES_URL, "Monthly Development Votes"));
        buttons.add(Button.of(ButtonStyle.LINK, ExternalLinks.FEATURE_REQUESTS_WEBSITE, "Feature Requests (More Boosts)"));
        buttons.add(Button.of(ButtonStyle.LINK, ExternalLinks.BETA_SERVER_INVITE, "Join Private Development Discord Server"));
        components.add(ActionRow.of(buttons));

        MessageComponentTree commandComponentTree = ComponentsUtil.createCommandComponentTree("Lawliet Premium - " + patreonTier.getName(), components, ComponentsUtil.DEFAULT_CONTAINER_COLOR);
        JDAUtil.openPrivateChannel(ShardManager.getAnyJDA().get(), userId)
                .flatMap(messageChannel -> messageChannel.sendMessageComponents(commandComponentTree).useComponentsV2())
                .queue();
    }

}
