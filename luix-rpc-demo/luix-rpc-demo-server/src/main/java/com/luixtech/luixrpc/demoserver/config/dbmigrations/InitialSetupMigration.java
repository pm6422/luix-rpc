package com.luixtech.luixrpc.demoserver.config.dbmigrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import com.luixtech.luixrpc.democommon.domain.AdminMenu;
import com.luixtech.luixrpc.democommon.domain.App;
import com.luixtech.luixrpc.democommon.domain.Authority;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Creates the initial database
 */
@ChangeUnit(id = "InitialSetupMigration", order = "01")
public class InitialSetupMigration {

    private static final String        APP_NAME = "rpc-demo-server";
    private final        MongoTemplate mongoTemplate;

    public InitialSetupMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Execution
    public void execute() {
        addApps();
        addAuthorities();
        addAuthorityAdminMenu();
    }

    @RollbackExecution
    public void rollback() {
        mongoTemplate.getDb().drop();
    }

    public void addApps() {
        App app = new App(APP_NAME, true);
        mongoTemplate.save(app);
    }

    public void addAuthorities() {
        mongoTemplate.save(new Authority(Authority.USER, true));
        mongoTemplate.save(new Authority(Authority.ADMIN, true));
        mongoTemplate.save(new Authority(Authority.DEVELOPER, true));
        mongoTemplate.save(new Authority(Authority.ANONYMOUS, true));
    }

    public void addAuthorityAdminMenu() {
        AdminMenu userAuthority = new AdminMenu("user-authority", "User authority", 1, "user-authority", 100, null);
        mongoTemplate.save(userAuthority);

        AdminMenu authorityList = new AdminMenu("authority-list", "Authority", 2, "user-authority.authority-list",
                101, userAuthority.getId());
        mongoTemplate.save(authorityList);

        AdminMenu app = new AdminMenu("app", "Application", 1, "app", 200, null);
        mongoTemplate.save(app);

        AdminMenu appList = new AdminMenu("app-list", "Application list", 2, "app.app-list", 201, app.getId());
        mongoTemplate.save(appList);
    }
}
