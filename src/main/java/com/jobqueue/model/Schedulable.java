package com.jobqueue.model;

import java.time.LocalDateTime;

public interface Schedulable {
    void schedule(LocalDateTime time);
    boolean cancel();
}
