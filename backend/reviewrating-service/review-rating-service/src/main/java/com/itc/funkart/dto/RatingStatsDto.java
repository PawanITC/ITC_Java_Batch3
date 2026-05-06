// com/itc/funkart/dto/RatingStatsDto.java
package com.itc.funkart.dto;

public record RatingStatsDto(Double avg, Long count) {

    public Double getAvg() {
        return avg;
    }


    public Long getCount() {
        return count;
    }
}
