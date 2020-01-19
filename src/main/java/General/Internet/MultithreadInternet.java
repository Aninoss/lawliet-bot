package General.Internet;

import java.io.IOException;

public class MultithreadInternet {

    private InternetResponse[] returnStrings;
    private String[] urlStrings;
    private int downloaded;

    public MultithreadInternet getData(String... urlStrings) {
        this.urlStrings = urlStrings;
        returnStrings = new InternetResponse[urlStrings.length];
        downloaded = 0;
        for(int i=0; i<urlStrings.length; i++) {
            final int j = i;
            Thread t = new Thread(() -> {
                try {
                    returnStrings[j] = Internet.getData(urlStrings[j]);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                downloaded++;
            });
            t.setName("multithread_download");
            t.start();
        }
        return this;
    }

    public InternetResponse[] get() {
        while(downloaded < urlStrings.length);
        return returnStrings;
    }
}
