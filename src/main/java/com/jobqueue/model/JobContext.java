package com.jobqueue.model;
import java.util.HashMap;
import java.util.Map;

public class JobContext {
    private final Map<String, Object> parameters;

    public JobContext() {
        this.parameters = new HashMap<>();
    }

    public void setParameter(String key, Object value) {
        parameters.put(key, value);
    }

    public Object getParameter(String key) {
        return parameters.get(key);
    }
}
