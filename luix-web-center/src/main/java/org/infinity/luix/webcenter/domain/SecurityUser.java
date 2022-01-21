package org.infinity.luix.webcenter.domain;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.security.core.GrantedAuthority;

import java.util.Collection;

@ToString
@Setter
@Getter
public class SecurityUser extends org.springframework.security.core.userdetails.User {

    private static final long   serialVersionUID = -8021915441738843058L;
    private              String userId;

    public SecurityUser(String userId, String username, String password, Collection<? extends GrantedAuthority> authorities) {
        super(username, password, authorities);
        this.userId = userId;
    }
}
