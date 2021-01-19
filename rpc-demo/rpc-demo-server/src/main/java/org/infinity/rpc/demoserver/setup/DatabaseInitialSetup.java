package org.infinity.rpc.demoserver.setup;

import com.github.mongobee.changeset.ChangeLog;
import com.github.mongobee.changeset.ChangeSet;
import org.infinity.rpc.democommon.domain.AdminMenu;
import org.infinity.rpc.democommon.domain.App;
import org.infinity.rpc.democommon.domain.Authority;
import org.springframework.data.mongodb.core.MongoTemplate;

/**
 * Creates the initial database
 */
@ChangeLog(order = "001")
public class DatabaseInitialSetup {

    private static final String APP_NAME = "rpc-demo-server";

    @ChangeSet(order = "01", author = "Louis", id = "addApps", runAlways = true)
    public void addApps(MongoTemplate mongoTemplate) {
        App app = new App(APP_NAME, true);
        mongoTemplate.save(app);
    }

    @ChangeSet(order = "02", author = "Louis", id = "addAuthorities", runAlways = true)
    public void addAuthorities(MongoTemplate mongoTemplate) {
        mongoTemplate.save(new Authority(Authority.USER, true, true));
        mongoTemplate.save(new Authority(Authority.ADMIN, true, true));
        mongoTemplate.save(new Authority(Authority.DEVELOPER, true, true));
        mongoTemplate.save(new Authority(Authority.ANONYMOUS, true, true));
    }

    @ChangeSet(order = "04", author = "Louis", id = "addAuthorityAdminMenu", runAlways = true)
    public void addAuthorityAdminMenu(MongoTemplate mongoTemplate) {

        AdminMenu userAuthority = new AdminMenu("user-authority", "用户权限", 1, "user-authority", 100, null);
        mongoTemplate.save(userAuthority);

        AdminMenu authorityList = new AdminMenu("authority-list", "权限", 2, "user-authority.authority-list",
                101, userAuthority.getId());
        mongoTemplate.save(authorityList);

        AdminMenu app = new AdminMenu("app", "应用系统", 1, "app", 200, null);
        mongoTemplate.save(app);

        AdminMenu appList = new AdminMenu("app-list", "应用", 2, "app.app-list", 201, app.getId());
        mongoTemplate.save(appList);
    }

}
