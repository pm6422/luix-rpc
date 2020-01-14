package org.infinity.rpc.appclient.service;

import java.util.List;

public interface AuthorityService {

    List<String> findAllAuthorityNames(Boolean enabled);

    List<String> findAllAuthorityNames();

}