package com.luixtech.utilities;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.Validate;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class TraceWatch {
    /**
     * Start time of the current task
     */
    private long startMs;

    /**
     * Name of the current task
     */
    private String currentTaskName;

    @Getter
    private final Map<String, List<TaskInfo>> taskMap = new HashMap<>();

    /**
     * 开始时间差类型指标记录，如果需要终止，请调用 {@link #stop()}
     *
     * @param taskName 指标名
     */
    public void start(String taskName) throws IllegalStateException {
        Validate.notNull(currentTaskName, "Can't start TraceWatch: it's already running");
        this.currentTaskName = taskName;
        this.startMs = System.currentTimeMillis();
    }

    /**
     * 终止时间差类型指标记录，调用前请确保已经调用
     */
    public void stop() throws IllegalStateException {
        Validate.notNull(currentTaskName, "Can't stop TraceWatch: it's not running");
        long lastTime = System.currentTimeMillis() - this.startMs;
        TaskInfo info = new TaskInfo(this.currentTaskName, lastTime);
        this.taskMap.computeIfAbsent(this.currentTaskName, e -> new LinkedList<>()).add(info);
        this.currentTaskName = null;
    }

    /**
     * 直接记录指标数据，不局限于时间差类型
     *
     * @param taskName 指标名
     * @param data     指标数据
     */
    public void record(String taskName, Object data) {
        TaskInfo info = new TaskInfo(taskName, data);
        this.taskMap.computeIfAbsent(taskName, e -> new LinkedList<>()).add(info);
    }

    @Getter
    @AllArgsConstructor
    public static final class TaskInfo {
        private final String taskName;
        private final Object data;
    }
}
