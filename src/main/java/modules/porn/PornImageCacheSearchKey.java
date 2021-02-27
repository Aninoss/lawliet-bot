package modules.porn;

import java.util.ArrayList;

public class PornImageCacheSearchKey {

    private final ArrayList<String> imageURLs = new ArrayList<>();

    public boolean contains(String imageURL) {
        return imageURLs.contains(imageURL);
    }

    public synchronized void trim(int maxSize) {
        while (imageURLs.size() > Math.min(30, maxSize)) {
            imageURLs.remove(0);
        }
    }

    public synchronized void add(String imageURL) {
        if (!imageURLs.contains(imageURL)) {
            imageURLs.add(imageURL);
            if (imageURLs.size() > 30) {
                imageURLs.remove(0);
            }
        }
    }

}
