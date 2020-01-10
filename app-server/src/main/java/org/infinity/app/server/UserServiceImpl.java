package org.infinity.app.server;

import org.infinity.app.common.UserService;
import org.infinity.rpc.server.annotation.Provider;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@Provider
public class UserServiceImpl implements UserService {
    @Override
    public int count() {
        return new Random().nextInt(100);
    }
}
