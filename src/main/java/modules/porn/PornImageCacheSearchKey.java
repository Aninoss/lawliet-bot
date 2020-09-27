package modules.porn;

import java.util.ArrayList;

public class PornImageCacheSearchKey {

    private final ArrayList<String> imageURLs = new ArrayList<>();

    public boolean contains(String imageURL) {
        return imageURLs.contains(imageURL);
    }

    public synchronized void trim(int maxSize) {
        while (imageURLs.size() > maxSize) imageURLs.remove(0);
    }

    public synchronized void add(String imageURL) {
        if (!imageURLs.contains(imageURL))
            imageURLs.add(imageURL);
    }

}
