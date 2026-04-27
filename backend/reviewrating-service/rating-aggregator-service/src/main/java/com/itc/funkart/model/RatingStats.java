package com.itc.funkart.model;

public class RatingStats {

    private final Double avg;
    private final Long count;

    public RatingStats(Double avg, Long count) {
        this.avg = avg;
        this.count = count;
    }

    public Double getAvg() { return avg; }
    public Long getCount() { return count; }
}

