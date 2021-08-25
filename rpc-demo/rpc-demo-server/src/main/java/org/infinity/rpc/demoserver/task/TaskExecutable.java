package org.infinity.rpc.demoserver.task;

import java.util.Map;

/**
 * Timing task used to execute job
 */
public interface TaskExecutable {
    String METHOD_NAME = "executeTask";

    /**
     * Execute task
     *
     * @param arguments method arguments map
     */
    void executeTask(Map<?, ?> arguments);
}
