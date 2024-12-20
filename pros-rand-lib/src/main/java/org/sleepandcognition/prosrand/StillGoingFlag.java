package org.sleepandcognition.prosrand;

public class StillGoingFlag {
    private boolean flag;

    public StillGoingFlag() {
        flag = true;
    }

    public synchronized boolean getFlagValue() {
        return flag;
    }

    public synchronized void clearFlag() {
        flag = false;
    }
}
