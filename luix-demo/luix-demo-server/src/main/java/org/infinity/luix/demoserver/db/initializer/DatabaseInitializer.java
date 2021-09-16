package org.infinity.luix.demoserver.db.initializer;

import com.github.cloudyrock.mongock.ChangeLog;
import com.github.cloudyrock.mongock.ChangeSet;
import com.github.cloudyrock.mongock.driver.mongodb.springdata.v3.decorator.impl.MongockTemplate;
import org.infinity.luix.democommon.domain.AdminMenu;
import org.infinity.luix.democommon.domain.App;
import org.infinity.luix.democommon.domain.Authority;

/**
 * Creates the initial database
 */
@ChangeLog(order = "01")
public class DatabaseInitializer {

    private static final String APP_NAME = "rpc-demo-server";

    @ChangeSet(order = "01", author = "Louis", id = "addApps", runAlways = true)
    public void addApps(MongockTemplate mongoTemplate) {
        App app = new App(APP_NAME, true);
        mongoTemplate.save(app);
    }

    @ChangeSet(order = "02", author = "Louis", id = "addAuthorities", runAlways = true)
    public void addAuthorities(MongockTemplate mongoTemplate) {
        mongoTemplate.save(new Authority(Authority.USER, true));
        mongoTemplate.save(new Authority(Authority.ADMIN, true));
        mongoTemplate.save(new Authority(Authority.DEVELOPER, true));
        mongoTemplate.save(new Authority(Authority.ANONYMOUS, true));
    }

    @ChangeSet(order = "04", author = "Louis", id = "addAuthorityAdminMenu", runAlways = true)
    public void addAuthorityAdminMenu(MongockTemplate mongoTemplate) {

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
