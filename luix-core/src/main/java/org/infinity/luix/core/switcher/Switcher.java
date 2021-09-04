package org.infinity.luix.core.switcher;

import lombok.Data;

/**
 * Switch
 */
@Data
public class Switcher {
    private String  name;
    private boolean on = true;

    public static Switcher of(String name, boolean on) {
        Switcher switcher = new Switcher();
        switcher.setName(name);
        switcher.setOn(on);
        return switcher;
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
