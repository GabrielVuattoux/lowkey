package ch.uzh.ifi.hase.soprafs26.rest.dto;

// The joining user is always the authenticated one, never an id taken from the request
public class EventJoinByCodePostDTO {
    private String inviteCode;

    public String getInviteCode() {
        return inviteCode;
    }

    public void setInviteCode(String inviteCode) {
        this.inviteCode = inviteCode;
    }
}
