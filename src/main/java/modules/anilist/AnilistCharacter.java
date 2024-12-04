package modules.anilist;

public class AnilistCharacter {

    private String name;
    private String characterUrl;
    private String imageUrl;
    private String mediaName;
    private String mediaUrl;
    private String age;
    private String gender;
    private int favourites;

    public AnilistCharacter() {
    }

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

    public void setName(String name) {
        this.name = name;
    }

    public String getCharacterUrl() {
        return characterUrl;
    }

    public void setCharacterUrl(String characterUrl) {
        this.characterUrl = characterUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getMediaName() {
        return mediaName;
    }

    public void setMediaName(String mediaName) {
        this.mediaName = mediaName;
    }

    public String getMediaUrl() {
        return mediaUrl;
    }

    public void setMediaUrl(String mediaUrl) {
        this.mediaUrl = mediaUrl;
    }

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public int getFavourites() {
        return favourites;
    }

    public void setFavourites(int favourites) {
        this.favourites = favourites;
    }

    private String stringSetNullIfEmpty(String string) {
        return string == null || string.isEmpty() ? null : string;
    }

}
