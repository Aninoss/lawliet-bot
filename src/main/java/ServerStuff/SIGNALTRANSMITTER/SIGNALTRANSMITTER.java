package ServerStuff.SIGNALTRANSMITTER;

import General.Internet.Internet;
import General.Internet.InternetResponse;
import General.Pair;
import General.SecretManager;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Calendar;

public class SIGNALTRANSMITTER {
    private static SIGNALTRANSMITTER ourInstance = new SIGNALTRANSMITTER();

    private String key;
    private String cookie;
    private static final String VPS_ID = "11810";

    public static SIGNALTRANSMITTER getInstance() {
        return ourInstance;
    }

    private SIGNALTRANSMITTER() {
        try {
            login();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void login() throws IOException {
        String body = "username="+ SecretManager.getString("SIGNALTRANSMITTER.username") +"&password=" + SecretManager.getString("SIGNALTRANSMITTER.password") + "&login=1";
        InternetResponse internetResponse = Internet.getData("https://vps.srv-control.it:4083/index.php?api=json&act=login", body, new Pair<>("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8"));
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
        } catch (IOException e) {
            e.printStackTrace();
        }

        return -1;
    }

    private double getTrafficGB(int tries) throws IOException {
        if (tries <= 0) {
            System.out.println("Giving up");
            return -1;
        }

        JSONObject data = new JSONObject(Internet.getData("https://vps.srv-control.it:4083/" + key + "/index.php?api=json&act=bandwidth&svs=" + VPS_ID + "&show=undefined", getProperties()).getContent().get());
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
            System.out.println("Could not connect to SIGNALTRANSMITTER...");

            login();
            return getTrafficGB(tries - 1);
        }
    }

    private Pair[] getProperties() {
        return new Pair[]{
                new Pair<>("Cookie", cookie),
                new Pair<>("Content-Type", "application/x-www-form-urlencoded")
        };
    }

}
