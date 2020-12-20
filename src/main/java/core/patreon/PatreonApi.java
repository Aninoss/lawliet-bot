package core.patreon;

import core.Bot;
import core.CustomThread;
import core.DiscordApiManager;
import core.SecretManager;
import core.internet.HttpProperty;
import core.internet.HttpRequest;
import mysql.modules.patreon.DBPatreon;
import mysql.modules.patreon.PatreonBean;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.concurrent.ExecutionException;

public class PatreonApi {

    private final static Logger LOGGER = LoggerFactory.getLogger(PatreonApi.class);
    
    private static final PatreonApi ourInstance = new PatreonApi();
    public static PatreonApi getInstance() { return ourInstance; }
    private PatreonApi() { }

    private final HashMap<String, Integer> TIER_MAP  = new HashMap<>() {{
        put("6044874", 1);
        put("4928466", 2);
        put("5074151", 3);
        put("5074320", 4);
        put("5080986", 5);
        put("5080991", 6);
    }};

    private HashMap<Long, Integer> userTiers = new HashMap<>();
    private Instant nextReset = Instant.now();

    public synchronized int getUserTier(long userId) {
        /* fetch update if due */
        if (Instant.now().isAfter(nextReset))
            new CustomThread(this::update, "patreon_update", 1).start();

        /* return 6 if user is owner */
        if (userId == DiscordApiManager.getInstance().getOwnerId())
            return 6;

        /* check patreon api */
        if (userTiers.containsKey(userId))
            return userTiers.get(userId);

        /* check sql database */
        HashMap<Long, PatreonBean> sqlPatreon = DBPatreon.getInstance().getBean();
        if (sqlPatreon.containsKey(userId)) {
            PatreonBean p = sqlPatreon.get(userId);
            if (p.isValid())
                return p.getTier();
        }

        return 0;
    }

    public void update() {
        if (Bot.isProductionMode()) {
            LOGGER.info("Updating Patreon tiers");
            nextReset = Instant.now().plus(10, ChronoUnit.MINUTES);
            try {
                HashMap<Long, Integer> userTiers = new HashMap<>();
                fetchFromUrl("https://www.patreon.com/api/oauth2/v2/campaigns/3334056/members?include=user,currently_entitled_tiers&fields%5Bmember%5D=full_name,patron_status&fields%5Buser%5D=social_connections&page%5Bsize%5D=9999", userTiers);
                this.userTiers = userTiers;
                LOGGER.info("Patreon update completed with {} users", userTiers.size());
            } catch (ExecutionException | InterruptedException e) {
                LOGGER.error("Could not fetch patreon data", e);
            }
        }
    }

    public HashMap<Long, Integer> getUserTiersMap() {
        HashMap<Long, Integer> userTiersMap = new HashMap<>(userTiers);
        HashMap<Long, PatreonBean> sqlMap = DBPatreon.getInstance().getBean();

        sqlMap.keySet().forEach(userId -> {
            PatreonBean p = sqlMap.get(userId);
            if (p.isValid())
                userTiersMap.put(userId, p.getTier());
        });
        return userTiersMap;
    }

    private void fetchFromUrl(String url, HashMap<Long, Integer> userTiers) throws ExecutionException, InterruptedException {
        HttpProperty property = new HttpProperty("Authorization", "Bearer " + SecretManager.getString("patreon.accesstoken"));
        String data = HttpRequest.getData(url, property).get().getContent().get();
        JSONObject rootJson = new JSONObject(data);

        HashMap<String, Integer> patreonTiers = getPatreonTiers(rootJson.getJSONArray("data"));
        addUserTiersToMap(userTiers, rootJson.getJSONArray("included"), patreonTiers);

        if (rootJson.has("links")) {
            JSONObject linksJson = rootJson.getJSONObject("links");
            if (linksJson.has("next") && !linksJson.isNull("next")) {
                fetchFromUrl(linksJson.getString("next"), userTiers);
            }
        }
    }

    private void addUserTiersToMap(HashMap<Long, Integer> userTiers, JSONArray includedJson, HashMap<String, Integer> patreonTiers) {
        for (int i = 0; i < includedJson.length(); i++) {
            JSONObject slotJson = includedJson.getJSONObject(i);
            if (slotJson.getString("type").equals("user")) {
                JSONObject attributesJson = slotJson.getJSONObject("attributes");
                if (attributesJson.has("social_connections")) {
                    JSONObject socialConnectionsJson = attributesJson.getJSONObject("social_connections");
                    String id = slotJson.getString("id");
                    if (patreonTiers.containsKey(id) && !socialConnectionsJson.isNull("discord")) {
                        long discordUserId = Long.parseLong(socialConnectionsJson.getJSONObject("discord").getString("user_id"));
                        userTiers.put(discordUserId, patreonTiers.get(id));
                    }
                }
            }
        }
    }

    private HashMap<String, Integer> getPatreonTiers(JSONArray dataJson) {
        HashMap<String, Integer> patreonTiers = new HashMap<>();
        for (int i = 0; i < dataJson.length(); i++) {
            JSONObject slotJson = dataJson.getJSONObject(i);
            JSONObject attributesJson = slotJson.getJSONObject("attributes");
            if (!attributesJson.isNull("patron_status") && attributesJson.getString("patron_status").equals("active_patron")) {
                JSONObject relationshipsJson = slotJson.getJSONObject("relationships");
                String id = relationshipsJson.getJSONObject("user").getJSONObject("data").getString("id");
                String tierId = relationshipsJson.getJSONObject("currently_entitled_tiers").getJSONArray("data").getJSONObject(0).getString("id");
                patreonTiers.put(id, TIER_MAP.get(tierId));
            }
        }
        return patreonTiers;
    }

}
