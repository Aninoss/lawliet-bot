package General.Porn;

import java.util.ArrayList;

public class PornImageCache {

    private static PornImageCache ourInstance = new PornImageCache();
    private ArrayList<PornImageCacheSearchKey> searchKeys;
    private final int MAX = 20;

    public static PornImageCache getInstance() {
        return ourInstance;
    }

    private PornImageCache() {
        searchKeys = new ArrayList<>();
    }

    public boolean contains(String searchKeyString, String imageURL) {
        return find(searchKeyString).contains(imageURL);
    }

    public void add(String searchKeyString, String imageURL, int max) {
        find(searchKeyString).add(imageURL, max);
    }

    public void clear(String searchKeyString) {
        searchKeys.removeIf(searchKey -> searchKey.getSearchKey().equals(searchKeyString));
    }

    private PornImageCacheSearchKey find(String searchKeyString) {
        for(PornImageCacheSearchKey searchKey: searchKeys) {
            if (searchKey.getSearchKey().equals(searchKeyString)) return searchKey;
        }

        PornImageCacheSearchKey searchKey = new PornImageCacheSearchKey(searchKeyString);
        searchKeys.add(searchKey);
        while(searchKeys.size() > MAX) {
            searchKeys.remove(0);
        }

        return searchKey;
    }

}