package ch.uzh.ifi.hase.soprafs26.rest.dto;

public class EventJoinByCodePostDTO {
    private String inviteCode;
    private Long userId;

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }
}
