package modules.osu;

import core.internet.InternetCache;
import core.utils.StringUtil;
import org.json.JSONObject;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

public class OsuAccountDownloader {

    public static Optional<OsuAccount> download(String username) throws ExecutionException, InterruptedException {
        String content = InternetCache.getData("https://osu.ppy.sh/users/" + username).get().getContent().get();
        String[] groups = StringUtil.extractGroups(content, "<script id=\"json-user\" type=\"application/json\">", "</script>");
        if (groups.length == 0)
            return Optional.empty();

        JSONObject data = new JSONObject(groups[0]);
        JSONObject stats = data.getJSONObject("statistics");
        JSONObject rank = stats.getJSONObject("rank");
        JSONObject level = stats.getJSONObject("level");

        return Optional.of(
                new OsuAccount(
                        data.getLong("id"),
                        data.getString("username"),
                        (int)Math.round(stats.getDouble("pp")),
                        rank.getLong("global"),
                        rank.getLong("country"),
                        data.getString("avatar_url"),
                        stats.getDouble("hit_accuracy"),
                        level.getInt("current"),
                        level.getInt("progress")
                )
        );
    }

}
