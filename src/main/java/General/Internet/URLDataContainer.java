package General.Internet;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;

public class URLDataContainer {
    private ArrayList<URLData> dataPackets;
    private static URLDataContainer instance = new URLDataContainer();

    public static URLDataContainer getInstance() {
        return instance;
    }

    public URLDataContainer() {
        dataPackets = new ArrayList<>();
    }

    public InternetResponse getData(String url) throws IOException, InterruptedException {
        return getData(url, 0);
    }

    public InternetResponse getData(String url, int waitingTime) throws InterruptedException, IOException {
        for (URLData urlData: new ArrayList<>(dataPackets)) {
            if (urlData.getUrl().equalsIgnoreCase(url)) {
                while (urlData.getBlockInstant() == null && Instant.now().isBefore(urlData.getCreateInstant().plusSeconds(30))) {
                    Thread.sleep(1000);
                }
                if (urlData.getBlockInstant() == null || Instant.now().isAfter(urlData.getBlockInstant())) {
                    urlData.setData(Internet.getData(url, waitingTime));
                }
                return urlData.getData();
            }
        }

        URLData newURLData = new URLData(url, Internet.getData(url));
        dataPackets.add(newURLData);
        manageCacheLimit();
        return newURLData.getData();
    }

    public InternetResponse getData(String url, int waitingTime, Instant instant) throws IOException, InterruptedException {
        InternetResponse str = getData(url, waitingTime);
        setInstantForURL(instant, url);
        return str;
    }

    public InternetResponse getData(String url, Instant instant) throws IOException, InterruptedException {
        InternetResponse str = getData(url,0);
        setInstantForURL(instant, url);
        return str;
    }

    public void setInstantForURL(Instant instant, String... urls) {
        for(String url: urls) {
            URLData urlData = findURLData(url);
            if (urlData != null && (urlData.getBlockInstant() == null || Instant.now().isAfter(urlData.getBlockInstant()))) {
                urlData.setBlockInstant(instant);
            }
        }
    }

    private URLData findURLData(String url) {
        for(URLData urlData: new ArrayList<>(dataPackets)) {
            if (urlData.getUrl().equalsIgnoreCase(url)) {
                return urlData;
            }
        }
        return null;
    }

    private void manageCacheLimit() {
        while (dataPackets.size() > 20) {
            dataPackets.remove(0);
        }
    }
}
