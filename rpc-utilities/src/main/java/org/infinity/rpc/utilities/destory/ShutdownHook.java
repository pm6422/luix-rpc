package org.infinity.rpc.utilities.destory;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.concurrent.ThreadSafe;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A utility used to clean up the resources
 *
 */
@Slf4j
@ThreadSafe
public class ShutdownHook extends Thread {
    /**
     * Lower values have higher cleanup priority which means to be cleanup earlier
     */
    private static final int                   DEFAULT_PRIORITY = 20;
    /**
     * Eager instance initialized while class load
     */
    private static final ShutdownHook          INSTANCE         = new ShutdownHook();
    /**
     * Resource list to be cleanup
     */
    private static final List<CleanableObject> RESOURCES        = new ArrayList<>();

    /**
     * Prohibit instantiate an instance outside the class
     */
    private ShutdownHook() {
    }

    public static synchronized void add(Cleanable cleanable, int priority) {
        INSTANCE.RESOURCES.add(new CleanableObject(cleanable, priority));
        log.info("Added the cleanup method of class [{}] to {}", cleanable.getClass().getSimpleName(), ShutdownHook.class.getSimpleName());
    }

    /**
     * Only global resources are allowed to add to it.
     *
     * @param cleanable
     */
    public static void add(Cleanable cleanable) {
        add(cleanable, DEFAULT_PRIORITY);
    }

    /**
     * Register the ShutdownHook to system runtime
     */
    public static void register() {
        Runtime.getRuntime().addShutdownHook(INSTANCE);
    }

    public static void runNow(boolean sync) {
        if (sync) {
            INSTANCE.run();
        } else {
            // Thread.start() will call the run() method on some proper occasion
            INSTANCE.start();
        }
    }

    /**
     * This method will be automatically invoked under below occasions:
     * - Program normal exit
     * - System.exit()
     * - Interruption triggered by Ctrl+C
     * - System close
     * - kill pid command
     * - zookeeper connection failed while startup
     */
    @Override
    public void run() {
        cleanup();
    }

    private synchronized void cleanup() {
        // Sort by priority
        Collections.sort(RESOURCES);
        for (CleanableObject resource : RESOURCES) {
            try {
                resource.cleanable.cleanup();
            } catch (Exception e) {
                log.error("Failed to cleanup " + resource.cleanable.getClass().getSimpleName(), e);
            }
            log.info("Cleaned up the {}", resource.cleanable.getClass().getSimpleName());
        }
        RESOURCES.clear();
    }

    @AllArgsConstructor
    private static class CleanableObject implements Comparable<CleanableObject> {
        private Cleanable cleanable;
        private int       priority;

        /**
         * Lower values have higher priority
         *
         * @param o object
         * @return
         */
        @Override
        public int compareTo(CleanableObject o) {
            if (this.priority > o.priority) {
                return -1;
            } else if (this.priority == o.priority) {
                return 0;
            } else {
                return 1;
            }
        }
    }
}

