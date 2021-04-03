package org.infinity.rpc.registry.zookeeper.utils;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.registry.zookeeper.StatusDir;

import java.util.Collections;
import java.util.List;

@Slf4j
public abstract class ZookeeperUtils {

    public static final String NAMESPACE                = "/infinity";
    public static final String DIR_PROVIDER             = "/provider";
    public static final String DIR_COMMAND              = "/command";
    public static final String FULL_PATH_PROVIDER       = NAMESPACE + DIR_PROVIDER;
    public static final String FULL_PATH_COMMAND        = NAMESPACE + DIR_COMMAND;
    public static final String PROVIDER_STATUS_DIR_PATH = "/infinity/provider/%s/%s";
    public static final String PROVIDER_FILE_PATH       = "/infinity/provider/%s/%s/%s";

    /**
     * Get the full path of provider address file
     *
     * @param path      provider class fully-qualified name
     * @param statusDir status directory
     * @param address   Server IP address of provider
     * @param form      provider form
     * @return full path of provider address file
     */
    public static String getProviderFilePath(String path, StatusDir statusDir, String address, String form) {
        if (StringUtils.isEmpty(form)) {
            return String.format(PROVIDER_FILE_PATH, path, statusDir.getValue(), address);
        }
        return String.format(PROVIDER_FILE_PATH, path, statusDir.getValue(), address + ":" + form);
    }

    /**
     * Get the full path of provider status directory
     *
     * @param path      provider class fully-qualified name
     * @param statusDir status directory
     * @return full path of provider status directory
     */
    public static String getStatusDirPath(String path, StatusDir statusDir) {
        return String.format(PROVIDER_STATUS_DIR_PATH, path, statusDir.getValue());
    }


    /**
     * Get all child directory or file names under the specified parent path
     *
     * @param parentPath parent directory path
     * @return names of child node
     */
    public static List<String> getChildrenNames(ZkClient zkClient, String parentPath) {
        return zkClient.exists(parentPath) ? zkClient.getChildren(parentPath) : Collections.emptyList();
    }
}
