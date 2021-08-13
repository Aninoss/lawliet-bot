package modules.porn;

import java.io.IOException;

public class TooManyTagsException extends IOException {

    private final int maxTags;

    public TooManyTagsException(int maxTags) {
        this.maxTags = maxTags;
    }

    public int getMaxTags() {
        return maxTags;
    }

}
