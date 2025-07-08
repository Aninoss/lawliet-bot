package events.sync.events;

import events.sync.InviteTrackingApiEvent;
import events.sync.SyncServerEvent;
import events.sync.apidata.v1.InviteStats;
import modules.invitetracking.InviteTracking;
import net.dv8tion.jda.api.entities.Guild;
import org.json.JSONObject;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@SyncServerEvent(event = "API_INVITE_TRACKING_STATS_TOP")
public class OnApiInviteTrackingStatsTop extends InviteTrackingApiEvent {

    @Override
    public JSONObject apply(JSONObject requestJson, JSONObject responseJSON, Guild guild) {
        int page = requestJson.getInt("page");
        int size = Math.max(1, Math.min(100, requestJson.getInt("size")));
        String sort = requestJson.getString("sort");

        List<InviteStats> inviteStats = InviteTracking.generateInviteMetricsMap(guild).values().stream()
                .map(this::mapToApiInviteStats)
                .sorted(getComparator(sort))
                .skip((long) page * size)
                .limit(size)
                .collect(Collectors.toList());

        responseJSON.put("objects", writeListAsJson(inviteStats));
        return responseJSON;
    }

    private Comparator<InviteStats> getComparator(String sort) {
        String[] split = sort.split(",");
        Comparator<InviteStats> comparator = switch (split.length > 0 ? split[0] : "") {
            case "onServer" -> Comparator.comparingInt(InviteStats::getOnServer);
            case "retained" -> Comparator.comparingInt(InviteStats::getRetained);
            case "active" -> Comparator.comparingInt(InviteStats::getActive);
            default -> Comparator.comparingLong(InviteStats::getTotal);
        };
        if (split.length < 2 || !split[1].equals("asc")) {
            comparator = comparator.reversed();
        }
        return comparator;
    }

}
