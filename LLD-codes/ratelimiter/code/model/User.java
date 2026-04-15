package model;

public class User {
    private final String userId;
    private final UserTier tier;

    public User(String userId, UserTier tier) {
        this.userId = userId;
        this.tier = tier;
    }

    public UserTier getTier() {
        return tier;
    }

    public String getUserId() {
        return userId;
    }
}
