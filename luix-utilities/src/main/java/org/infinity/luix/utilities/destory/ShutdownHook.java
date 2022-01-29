package org.infinity.luix.utilities.destory;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.infinity.luix.utilities.concurrent.ThreadSafe;

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
     * Prevent instantiation of it outside the class
     */
    private ShutdownHook() {
    }

    public static synchronized void add(Destroyable destroyable, int priority) {
        RESOURCES.add(new CleanableObject(destroyable, priority));
        log.info("Added the cleanup method of class [{}] to {}", destroyable.getClass().getSimpleName(), ShutdownHook.class.getSimpleName());
    }

    /**
     * Only global resources are allowed to add to it.
     *
     * @param destroyable cleanable
     */
    public static void add(Destroyable destroyable) {
        add(destroyable, DEFAULT_PRIORITY);
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
                resource.destroyable.destroy();
            } catch (Exception e) {
                log.error("Failed to cleanup " + resource.destroyable.getClass().getSimpleName(), e);
            }
            log.info("Cleaned up the {}", resource.destroyable.getClass().getSimpleName());
        }
        RESOURCES.clear();
    }

    @AllArgsConstructor
    private static class CleanableObject implements Comparable<CleanableObject> {
        private final Destroyable destroyable;
        private final int         priority;

        /**
         * Lower values have higher priority
         *
         * @param o object
         * @return result
         */
        @Override
        public int compareTo(CleanableObject o) {
            return Integer.compare(o.priority, this.priority);
        }
    }
}

