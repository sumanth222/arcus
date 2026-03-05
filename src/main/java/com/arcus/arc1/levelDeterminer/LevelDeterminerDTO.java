package com.arcus.arc1.levelDeterminer;

public class LevelDeterminerDTO {
    private int pushUps;
    private int pullUps;
    private int sitUps;

    public int getPushUps() {
        return pushUps;
    }

    public void setPushUps(int pushUps) {
        this.pushUps = pushUps;
    }

    public int getPullUps() {
        return pullUps;
    }

    public void setPullUps(int pullUps) {
        this.pullUps = pullUps;
    }

    public int getSitUps() {
        return sitUps;
    }

    public void setSitUps(int sitUps) {
        this.sitUps = sitUps;
    }

    public LevelDeterminerDTO(int pushUps, int pullUps, int sitUps) {
        this.pushUps = pushUps;
        this.pullUps = pullUps;
        this.sitUps = sitUps;
    }
}
