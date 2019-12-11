package General.Porn;

import General.Comment;

import java.time.Instant;
import java.util.ArrayList;

public class PornImage {
    private String imageUrl, pageUrl;
    private ArrayList<Comment> comments;
    private int score, nComments;
    private Instant instant;
    private boolean video;

    public PornImage(String imageUrl, String pageUrl, ArrayList<Comment> comments, int score, int nComments, Instant instant, boolean video) {
        this.imageUrl = imageUrl;
        this.pageUrl = pageUrl;
        this.comments = comments;
        this.score = score;
        this.nComments = nComments;
        this.instant = instant;
        this.video = video;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getPageUrl() {
        return pageUrl;
    }

    public ArrayList<Comment> getComments() {
        return comments;
    }

    public int getScore() {
        return score;
    }

    public int getnComments() {
        return nComments;
    }

    public Instant getInstant() {
        return instant;
    }

    public boolean isVideo() {
        return video;
    }
}
