package mysql.hibernate.entity.user;

public enum RolePlayGender {

    MALE("m"), FEMALE("f"), ANY("a");


    private final String id;

    RolePlayGender(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

}
