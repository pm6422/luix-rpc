package org.infinity.rpc.appclient.service;

import java.util.List;
import java.util.Set;

public interface AuthorityAdminMenuService {

    Set<String> findAdminMenuIdSetByAuthorityNameIn(List<String> authorityNames);

}