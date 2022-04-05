package org.infinity.luix.webcenter.config.dbmigrations;

import io.mongock.api.annotations.ChangeUnit;
import io.mongock.api.annotations.Execution;
import io.mongock.api.annotations.RollbackExecution;
import org.infinity.luix.webcenter.domain.Authority;
import org.infinity.luix.webcenter.domain.User;
import org.infinity.luix.webcenter.domain.UserAuthority;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Creates the initial database
 */
@ChangeUnit(id = "InitialSetupMigration", order = "01")
public class InitialSetupMigration {

    private static final String        APP_NAME = "rpc-web-center";
    private final        MongoTemplate mongoTemplate;

    public InitialSetupMigration(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Execution
    public void execute() {
        addAuthorities();
        addUserAndAuthorities();
    }

    @RollbackExecution
    public void rollback() {
        mongoTemplate.getDb().drop();
    }

    public void addAuthorities() {
        mongoTemplate.save(new Authority(Authority.USER, true));
        mongoTemplate.save(new Authority(Authority.ADMIN, true));
        mongoTemplate.save(new Authority(Authority.DEVELOPER, true));
        mongoTemplate.save(new Authority(Authority.ANONYMOUS, true));
    }

    public void addUserAndAuthorities() {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        // Creates 'user' user and corresponding authorities
        User userRoleUser = new User();
        userRoleUser.setUserName("user");
        userRoleUser.setFirstName("User");
        userRoleUser.setLastName("User");
        userRoleUser.setEmail("user@localhost");
        userRoleUser.setMobileNo("15000899479");
        // Raw password: user
        userRoleUser.setPasswordHash(passwordEncoder.encode("user"));
        userRoleUser.setActivated(true);
        userRoleUser.setActivationKey(null);
        userRoleUser.setResetKey(null);
        userRoleUser.setResetTime(null);
        userRoleUser.setEnabled(true);
        mongoTemplate.save(userRoleUser);

        mongoTemplate.save(new UserAuthority(userRoleUser.getId(), Authority.USER));

        // Creates 'admin' user and corresponding authorities
        User adminRoleUser = new User();
        adminRoleUser.setUserName("admin");
        adminRoleUser.setFirstName("Admin");
        adminRoleUser.setLastName("Admin");
        adminRoleUser.setEmail("admin@localhost");
        adminRoleUser.setMobileNo("15000899477");
        // Raw password: admin
        adminRoleUser.setPasswordHash(passwordEncoder.encode("admin"));
        adminRoleUser.setActivated(true);
        adminRoleUser.setActivationKey(null);
        adminRoleUser.setResetKey(null);
        adminRoleUser.setResetTime(null);
        adminRoleUser.setEnabled(true);
        mongoTemplate.save(adminRoleUser);

        mongoTemplate.save(new UserAuthority(adminRoleUser.getId(), Authority.USER));
        mongoTemplate.save(new UserAuthority(adminRoleUser.getId(), Authority.ADMIN));

        // Creates 'system' user and corresponding authorities
        User adminRoleSystemUser = new User();
        adminRoleSystemUser.setUserName("system");
        adminRoleSystemUser.setFirstName("System");
        adminRoleSystemUser.setLastName("System");
        adminRoleSystemUser.setEmail("system@localhost");
        adminRoleSystemUser.setMobileNo("15000899422");
        // Raw password: system
        adminRoleSystemUser.setPasswordHash(passwordEncoder.encode("system"));
        adminRoleSystemUser.setActivated(true);
        adminRoleSystemUser.setActivationKey(null);
        adminRoleSystemUser.setResetKey(null);
        adminRoleSystemUser.setResetTime(null);
        adminRoleSystemUser.setEnabled(true);
        mongoTemplate.save(adminRoleSystemUser);

        mongoTemplate.save(new UserAuthority(adminRoleSystemUser.getId(), Authority.USER));
        mongoTemplate.save(new UserAuthority(adminRoleSystemUser.getId(), Authority.ADMIN));

        // Creates 'louis' user and corresponding authorities
        User developerRoleUser = new User();
        developerRoleUser.setUserName("louis");
        developerRoleUser.setFirstName("Louis");
        developerRoleUser.setLastName("Lau");
        developerRoleUser.setEmail("louis@luixtech.com");
        developerRoleUser.setMobileNo("15000899488");
        // Raw password: louis
        developerRoleUser.setPasswordHash(passwordEncoder.encode("louis"));
        developerRoleUser.setActivated(true);
        developerRoleUser.setActivationKey(null);
        developerRoleUser.setResetKey(null);
        developerRoleUser.setResetTime(null);
        developerRoleUser.setEnabled(true);
        mongoTemplate.save(developerRoleUser);

        mongoTemplate.save(new UserAuthority(developerRoleUser.getId(), Authority.USER));
        mongoTemplate.save(new UserAuthority(developerRoleUser.getId(), Authority.ADMIN));
        mongoTemplate.save(new UserAuthority(developerRoleUser.getId(), Authority.DEVELOPER));
    }
}
