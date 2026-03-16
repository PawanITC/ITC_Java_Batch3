package com.itc.funkart.dto;

public class ReviewRequest {
    private int rating;
    private String comment;

    public ReviewRequest() {
    }

    public ReviewRequest(int rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }


    // getters/setters

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}