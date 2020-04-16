package ServerStuff;

import Core.Internet.Internet;
import Core.Internet.InternetProperty;
import Core.Internet.InternetResponse;
import Core.SecretManager;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Calendar;
import java.util.concurrent.ExecutionException;

public class SIGNALTRANSMITTER {

    private static SIGNALTRANSMITTER ourInstance = new SIGNALTRANSMITTER();

    private String key;
    private String cookie;
    private static final String VPS_ID = "11810";

    public static SIGNALTRANSMITTER getInstance() {
        return ourInstance;
    }

    final static Logger LOGGER = LoggerFactory.getLogger(SIGNALTRANSMITTER.class);

    private SIGNALTRANSMITTER() {
        try {
            login();
        } catch (IOException | ExecutionException | InterruptedException e) {
            LOGGER.error("Could not log into SIGNALTRANSMITTER", e);
        }
    }

    private void login() throws IOException, ExecutionException, InterruptedException {
        String body = "username="+ SecretManager.getString("SIGNALTRANSMITTER.username") +"&password=" + SecretManager.getString("SIGNALTRANSMITTER.password") + "&login=1";
        InternetResponse internetResponse = Internet.getData("https://vps.srv-control.it:4083/index.php?api=json&act=login", body, new InternetProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")).get();
        cookie = internetResponse.getCookies().get().get(0);
        key = new JSONObject(internetResponse.getContent().get()).getString("redirect").split("/")[1];
    }

    private void logout() throws IOException {
        Internet.getData("https://vps.srv-control.it:4083/" + key + "/index.php?api=json&act=logout&api=json", getProperties());
        ourInstance = null;
    }

    public double getTrafficGB() {
        try {
            return getTrafficGB(5);
        } catch (IOException | ExecutionException | InterruptedException e) {
            LOGGER.error("Could not get traffic", e);
        }

        return -1;
    }

    private double getTrafficGB(int tries) throws IOException, ExecutionException, InterruptedException {
        if (tries <= 0) {
            return -1;
        }

        JSONObject data = new JSONObject(Internet.getData("https://vps.srv-control.it:4083/" + key + "/index.php?api=json&act=bandwidth&svs=" + VPS_ID + "&show=undefined", getProperties()).get().getContent().get());
        String act = data.getString("act");
        if (act.equals("bandwidth")) {
            Calendar calendar = Calendar.getInstance();
            String year = String.valueOf(calendar.get(Calendar.YEAR));
            String month = String.format("%02d", calendar.get(Calendar.MONTH) + 1);
            String day = String.format("%02d", calendar.get(Calendar.DAY_OF_MONTH));
            String dateString = year + month + day;

            JSONObject usages = data.getJSONObject("bandwidth").getJSONObject("usage");
            if (usages.has(dateString)) return usages.getDouble(dateString) / 1000;
            else return -1;
        } else {
            LOGGER.error("Could not connect to SIGNALTRANSMITTER");

            login();
            return getTrafficGB(tries - 1);
        }
    }

    private InternetProperty[] getProperties() {
        return new InternetProperty[]{
                new InternetProperty("Cookie", cookie),
                new InternetProperty("Content-Type", "application/x-www-form-urlencoded")
        };
    }

}
