package com.booking.backend.domain.game.repository;

public interface PlayCountEntry {
    String getMemberName();
    String getTeamName();
    Long getPlayCount();
}