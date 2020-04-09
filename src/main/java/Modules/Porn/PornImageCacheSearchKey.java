package Modules.Porn;

import java.util.ArrayList;

public class PornImageCacheSearchKey {

    private ArrayList<String> imageURLs = new ArrayList<>();

    public boolean contains(String imageURL) {
        return imageURLs.contains(imageURL);
    }

    public synchronized void trim(int maxSize) {
        while (imageURLs.size() > maxSize) imageURLs.remove(0);
    }

    public synchronized void add(String imageURL) {
        imageURLs.remove(imageURL);
        imageURLs.add(imageURL);
    }

}
