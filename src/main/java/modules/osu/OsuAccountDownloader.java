package modules.osu;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import core.internet.HttpCache;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import modules.OsuGame;
import org.json.JSONObject;

public class OsuAccountDownloader {

    public static CompletableFuture<Optional<OsuAccount>> download(String username, OsuGame gameMode) {
        return HttpCache.get("https://osu.ppy.sh/users/" + InternetUtil.escapeForURL(username) + "/" + gameMode.getId(), Duration.ofMinutes(5))
                .thenApply(res -> {
                    String content = res.getBody();
                    if (content == null) {
                        return Optional.empty();
                    }

                    String[] groups = StringUtil.extractGroups(content, "data-initial-data=\"", "\"");
                    if (groups.length == 0) {
                        return Optional.empty();
                    }

                    String json = StringUtil.decryptString(groups[0]);
                    JSONObject data = new JSONObject(json).getJSONObject("user");
                    JSONObject stats = data.getJSONObject("statistics");
                    JSONObject country = data.getJSONObject("country");
                    JSONObject rank = stats.getJSONObject("rank");
                    JSONObject level = stats.getJSONObject("level");

                    return Optional.of(
                            new OsuAccount(
                                    data.getLong("id"),
                                    data.getString("username"),
                                    country.getString("code"),
                                    (int) Math.round(stats.getDouble("pp")),
                                    stats.isNull("global_rank") ? null : stats.getLong("global_rank"),
                                    rank.isNull("country") ? null : rank.getLong("country"),
                                    data.getString("avatar_url"),
                                    stats.getDouble("hit_accuracy"),
                                    level.getInt("current"),
                                    level.getInt("progress")
                            )
                    );
                });
    }

}
