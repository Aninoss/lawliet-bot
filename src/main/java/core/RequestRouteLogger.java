package core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

public class RequestRouteLogger {

    private static final HashMap<String, RequestMeta> routeMap = new HashMap<>();

    public static void logRoute(String path, boolean ratelimit) {
        RequestMeta requestMeta = routeMap.computeIfAbsent(path, k -> new RequestMeta(path));
        requestMeta.requests++;
        if (ratelimit) {
            requestMeta.requestsRateLimit++;
        }
        routeMap.put(path, requestMeta);
    }

    public static void clear() {
        routeMap.clear();
    }

    public static List<RequestMeta> getRoutes() {
        return new ArrayList<>(routeMap.values()).stream()
                .sorted((e0, e1) -> Long.compare(e1.requests, e0.requests))
                .collect(Collectors.toList());
    }


    public static class RequestMeta {

        private final String route;
        private long requests = 0;
        private long requestsRateLimit = 0;

        public RequestMeta(String route) {
            this.route = route;
        }

        public String getRoute() {
            return route;
        }

        public long getRequests() {
            return requests;
        }

        public long getRequestsRateLimit() {
            return requestsRateLimit;
        }

    }

}
