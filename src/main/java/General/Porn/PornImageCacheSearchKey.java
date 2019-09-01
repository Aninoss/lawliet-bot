package General.Porn;

import java.util.ArrayList;

public class PornImageCacheSearchKey {

    private String searchKey;
    private ArrayList<String> imageURLs;

    public PornImageCacheSearchKey(String searchKey) {
        this.searchKey = searchKey;
        this.imageURLs = new ArrayList<>();
    }

    public boolean contains(String imageURL) {
        return imageURLs.contains(imageURL);
    }

    public void add(String imageURL, int max) {
        if (!contains(imageURL)) imageURLs.add(imageURL);

        while(imageURLs.size() > max) {
            imageURLs.remove(0);
        }
    }

    public String getSearchKey() {
        return searchKey;
    }
}
