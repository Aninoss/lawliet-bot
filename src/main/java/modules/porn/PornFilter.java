package modules.porn;

import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class PornFilter {

    public static synchronized PornImageMeta filter(String domain, String searchKey, ArrayList<PornImageMeta> pornImages, ArrayList<String> usedResult) throws ExecutionException {
        if (pornImages.size() == 0) return null;

        /* Delete global duplicate images */
        PornImageCacheSearchKey pornImageCacheSearchKey = PornImageCache.getInstance().get(domain, searchKey);
        pornImageCacheSearchKey.trim(pornImages.size() - 1);
        pornImages.removeIf(pornImageMeta -> pornImageCacheSearchKey.contains(pornImageMeta.getImageUrl()));

        /* Delete duplicate images for this command usage */
        pornImages.removeIf(pornImageMeta -> usedResult.contains(pornImageMeta.getImageUrl()));

        long totalWeight = pornImages.stream().mapToLong(PornImageMeta::getWeight).sum();
        long pos = (long) (new Random().nextDouble() * totalWeight);
        for (PornImageMeta pornImageMeta : pornImages) {
            if ((pos -= pornImageMeta.getWeight()) < 0) {
                pornImageCacheSearchKey.add(pornImageMeta.getImageUrl());
                usedResult.add(pornImageMeta.getImageUrl());
                return pornImageMeta;
            }
        }

        return null;
    }

}