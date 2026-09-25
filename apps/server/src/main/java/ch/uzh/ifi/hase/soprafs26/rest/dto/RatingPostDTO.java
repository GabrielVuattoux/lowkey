package ch.uzh.ifi.hase.soprafs26.rest.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class RatingPostDTO {
    @NotNull
    @Min(1)
    @Max(5)
    private Integer score;

    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
}