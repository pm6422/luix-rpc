package org.infinity.rpc.core.registry;

import lombok.Data;
import org.infinity.rpc.utilities.id.ShortIdWorker;

@Data
public class App {
    // Application ID
    private String id;
    // Application name
    private String name;
    // Application description
    private String description;
    // Responsible team
    private String team;
    // Application owner
    private String owner;
    // Environment variable, e.g. dev, test or prod
    private String env;
}
