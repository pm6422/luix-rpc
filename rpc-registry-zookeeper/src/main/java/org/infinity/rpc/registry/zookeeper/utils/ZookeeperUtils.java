package org.infinity.rpc.registry.zookeeper.utils;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.registry.zookeeper.ZookeeperStatusNode;

import java.util.Collections;
import java.util.List;

import static org.apache.commons.io.IOUtils.DIR_SEPARATOR_UNIX;


@Slf4j
public abstract class ZookeeperUtils {

    public static final String REGISTRY_NAMESPACE = "/infinity";
    public static final String PROVIDER_DIR       = "/provider";
    public static final String PROVIDER_PATH      = REGISTRY_NAMESPACE + PROVIDER_DIR;
    public static final String REGISTRY_COMMAND   = "/command";

    /**
     * Get the provider address node full path under specified form and status node
     *
     * @param form    zookeeper form node
     * @param path    provider class fully-qualified name
     * @param node    status node
     * @param address provider address
     * @return provider status node full path
     */
    public static String getProviderAddressFilePath(String form, String path, ZookeeperStatusNode node, String address) {
        return getProviderStatusNodePath(form, path, node) + DIR_SEPARATOR_UNIX + address;
    }

    /**
     * Get the provider status node full path under specified form and status node
     *
     * @param form zookeeper form node
     * @param path provider class fully-qualified name
     * @param node status node
     * @return provider status node full path
     */
    public static String getProviderStatusNodePath(String form, String path, ZookeeperStatusNode node) {
        return getProviderPath(form, path) + DIR_SEPARATOR_UNIX + node.getValue();
    }

    /**
     * Get the provider full path under specified form node
     *
     * @param form form
     * @param path provider class fully-qualified name
     * @return provider full path
     */
    public static String getProviderPath(String form, String path) {
        return getFormPath(form) + DIR_SEPARATOR_UNIX + path;
    }

    /**
     * Get all child directory or file names under the parent path
     *
     * @param path zookeeper directory path
     * @return child node names
     */
    public static List<String> getChildrenNames(ZkClient zkClient, String path) {
        return zkClient.exists(path) ? zkClient.getChildren(path) : Collections.emptyList();
    }

    /**
     * Get the command full path
     *
     * @param url url
     * @return command full path
     */
    public static String getCommandPath(Url url) {
        return getFormPath(url.getForm()) + REGISTRY_COMMAND;
    }

    /**
     * Get the full path of provider form node
     *
     * @param form form
     * @return form node full path
     */
    public static String getFormPath(String form) {
        return PROVIDER_PATH + DIR_SEPARATOR_UNIX + form;
    }
}
