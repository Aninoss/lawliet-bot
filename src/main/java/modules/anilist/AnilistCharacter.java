package modules.anilist;

public class AnilistCharacter {

    private final String name;
    private final String siteUrl;
    private final String imageUrl;
    private final String mediaName;
    private final String age;
    private final String gender;
    private final int favourites;

    public AnilistCharacter(String name, String siteUrl, String imageUrl, String mediaName, String age, String gender, int favourites) {
        this.name = name;
        this.siteUrl = siteUrl;
        this.imageUrl = imageUrl;
        this.mediaName = mediaName;
        this.age = age;
        this.gender = gender;
        this.favourites = favourites;
    }

    public String getName() {
        return name;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getMediaName() {
        return mediaName;
    }

    public String getAge() {
        return age;
    }

    public String getGender() {
        return gender;
    }

    public int getFavourites() {
        return favourites;
    }

    @Override
    public String toString() {
        return "AnilistCharacter{" +
                "name='" + name + '\'' +
                ", siteUrl='" + siteUrl + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", mediaName='" + mediaName + '\'' +
                ", age='" + age + '\'' +
                ", gender='" + gender + '\'' +
                ", favourites=" + favourites +
                '}';
    }

}
