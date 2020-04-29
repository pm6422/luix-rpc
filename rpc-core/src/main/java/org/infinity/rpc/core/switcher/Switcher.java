package org.infinity.rpc.core.switcher;

import lombok.Data;

/**
 * Switch tool
 */
@Data
public class Switcher {
    private String  name;
    private boolean on = true;

    public Switcher(String name, boolean on) {
        this.name = name;
        this.on = on;
    }

    /**
     * turn on switcher
     */
    public void switchOn() {
        this.on = true;
    }

    /**
     * turn off switcher
     */
    public void switchOff() {
        this.on = false;
    }
}
