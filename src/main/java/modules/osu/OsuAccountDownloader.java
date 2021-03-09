package modules.osu;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import core.internet.InternetCache;
import core.utils.InternetUtil;
import core.utils.StringUtil;
import org.json.JSONObject;

public class OsuAccountDownloader {

    public static CompletableFuture<Optional<OsuAccount>> download(String username, String gameMode) {
        return InternetCache.getData("https://osu.ppy.sh/users/" + InternetUtil.escapeForURL(username) + "/" + gameMode)
                .thenApply(res -> {
                    Optional<String> contentOpt = res.getContent();
                    if (contentOpt.isEmpty())
                        return Optional.empty();

                    String content = contentOpt.get();
                    String[] groups = StringUtil.extractGroups(content, "<script id=\"json-user\" type=\"application/json\">", "</script>");
                    if (groups.length == 0)
                        return Optional.empty();

                    JSONObject data = new JSONObject(groups[0]);
                    JSONObject stats = data.getJSONObject("statistics");
                    JSONObject country = data.getJSONObject("country");
                    JSONObject rank = stats.getJSONObject("rank");
                    JSONObject level = stats.getJSONObject("level");

                    return Optional.of(
                            new OsuAccount(
                                    data.getLong("id"),
                                    data.getString("username"),
                                    country.getString("code"),
                                    (int)Math.round(stats.getDouble("pp")),
                                    rank.isNull("global") ? null : rank.getLong("global"),
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
