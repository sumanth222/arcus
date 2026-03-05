package com.arcus.arc1.levelDeterminer;

import com.arcus.arc1.constants.ArcConstants;

public class LevelDeterminer {

    public String determineLevel(LevelDeterminerDTO ldd) {

        int score = 0;

        // Pushups
        if (ldd.getPushUps() < 5) {
            score += 0;
        } else if (ldd.getPushUps() <= 20) {
            score += 1;
        } else if (ldd.getPushUps() <= 40) {
            score += 2;
        } else {
            score += 3;
        }

        // Pullups
        if (ldd.getPullUps() < 5) {
            score += 0;
        } else if (ldd.getPullUps() <= 10) {
            score += 1;
        } else if (ldd.getPullUps() <= 20) {
            score += 2;
        } else {
            score += 3;
        }

        // Situps
        if (ldd.getSitUps() < 10) {
            score += 0;
        } else if (ldd.getSitUps() <= 25) {
            score += 1;
        } else if (ldd.getSitUps() <= 40) {
            score += 2;
        } else {
            score += 3;
        }

        // Total possible score = 9

        if (score <= 2) {
            return ArcConstants.LEVEL_BEGINNER;
        } else if (score <= 5) {
            return ArcConstants.LEVEL_MEDIUM;
        } else if (score <= 7) {
            return ArcConstants.LEVEL_ADVANCED;
        } else {
            return ArcConstants.LEVEL_EXPERT;
        }
    }
}
