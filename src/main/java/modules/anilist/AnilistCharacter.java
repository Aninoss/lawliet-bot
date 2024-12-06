package modules.anilist;

public class AnilistCharacter {

    private final String name;
    private final String characterUrl;
    private final String imageUrl;
    private final String mediaName;
    private final String mediaUrl;
    private final String age;
    private final String gender;
    private final int favourites;

    public AnilistCharacter(String name, String characterUrl, String imageUrl, String mediaName, String mediaUrl, String age, String gender, int favourites) {
        this.name = name;
        this.characterUrl = characterUrl;
        this.imageUrl = stringSetNullIfEmpty(imageUrl);
        this.mediaName = stringSetNullIfEmpty(mediaName);
        this.mediaUrl = stringSetNullIfEmpty(mediaUrl);
        this.age = stringSetNullIfEmpty(age);
        this.gender = stringSetNullIfEmpty(gender);
        this.favourites = favourites;
    }

    public String getName() {
        return name;
    }

    public String getSiteUrl() {
        return characterUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getMediaName() {
        return mediaName;
    }

    public String getMediaUrl() {
        return mediaUrl;
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

    private String stringSetNullIfEmpty(String string) {
        return string == null || string.isEmpty() ? null : string;
    }

}
