package ch.uzh.ifi.hase.soprafs26.rest.dto;

import java.util.Set;

import ch.uzh.ifi.hase.soprafs26.constant.UserStatus;


public class UserGetDTO {

	private Long id;
	private String username;
	private String email;
	private UserStatus status;
	private String bio;
    private Boolean allowPrivateMessages;
	private Set<Long> followingIds;
    private Double averageRating;
    private Integer ratingCount;

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public UserStatus getStatus() {
		return status;
	}

	public void setStatus(UserStatus status) {
		this.status = status;
	}

	public String getBio() {
		return bio;
	}

	public void setBio(String bio) {
		this.bio = bio;
	}
    public Boolean getAllowPrivateMessages() {
        return allowPrivateMessages;
    }

    public void setAllowPrivateMessages(Boolean allowPrivateMessages) {
        this.allowPrivateMessages = allowPrivateMessages;
    }

	public Set<Long> getFollowingIds() {
		return followingIds;
	}

	public void setFollowingIds(Set<Long> followingIds) {
		this.followingIds = followingIds;
	}
}
