package com.example.instgram;

// self-implemented class for async calls (to achieve promise.all)
public class AsyncCallHandler {
    private Boolean vetoFailure;
    private int successfulCount;
    private int taskRegistered;

    public AsyncCallHandler(int task) {
        this.taskRegistered = task;
        this.successfulCount = 0;
        this.vetoFailure = false;
    }

    public void addSuccessfulTask() {
        this.successfulCount += 1;
    }

    public Boolean waitForAllComplete() {
        if (this.taskRegistered == this.successfulCount) {
            return true;
        } else {
            return false;
        }
    }




}
