package com.luixtech.rpc.demoserver.config.dbmigrations;

import com.luixtech.rpc.democommon.domain.AdminMenu;
import com.luixtech.rpc.democommon.domain.App;
import com.luixtech.rpc.democommon.domain.Authority;
import lombok.AllArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates the initial database
 */
@Component
@AllArgsConstructor
public class InitialSetupMigration implements ApplicationRunner {

    private static final String        APP_NAME       = "rpc-demo-server";
    private static final String        MENU_PARENT_ID = "0";
    private final        MongoTemplate mongoTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        drop();
        addApps();
        addAuthorities();
        addAuthorityAdminMenu();
    }

    public void drop() {
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
        AdminMenu userAuthority = new AdminMenu("user-authority", "User authority", 1, "user-authority", 100, MENU_PARENT_ID);
        mongoTemplate.save(userAuthority);

        AdminMenu authorityList = new AdminMenu("authority-list", "Authority", 2, "user-authority.authority-list",
                101, userAuthority.getId());
        mongoTemplate.save(authorityList);

        AdminMenu app = new AdminMenu("app", "Application", 1, "app", 200, MENU_PARENT_ID);
        mongoTemplate.save(app);

        AdminMenu appList = new AdminMenu("app-list", "Application list", 2, "app.app-list", 201, app.getId());
        mongoTemplate.save(appList);
    }
}
