package org.infinity.luix.registry.zookeeper.utils;

import lombok.extern.slf4j.Slf4j;
import org.I0Itec.zkclient.ZkClient;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.luix.core.url.Url;
import org.infinity.luix.registry.zookeeper.StatusDir;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.apache.commons.io.IOUtils.DIR_SEPARATOR_UNIX;

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
     * @param url       url
     * @param statusDir status directory
     * @return full path of provider address file
     */
    public static String getProviderFilePath(Url url, StatusDir statusDir) {
        if (StringUtils.isEmpty(url.getForm())) {
            return String.format(PROVIDER_FILE_PATH, url.getPath(), statusDir.getValue(), url.getAddress());
        }
        return String.format(PROVIDER_FILE_PATH, url.getPath(), statusDir.getValue(), url.getAddress() + ":" + url.getForm());
    }

    /**
     * Read urls from data of address files
     *
     * @param zkClient  zk client
     * @param path      provider class fully-qualified name, e.g. org.infinity.app.common.service.AppService
     * @param statusDir status directory
     * @return provider urls
     */
    public static List<Url> readUrls(ZkClient zkClient, String path, StatusDir statusDir) {
        String statusDirPath = getStatusDirPath(path, statusDir);
        List<String> addrFiles = getChildrenNames(zkClient, statusDirPath);
        return CollectionUtils.isEmpty(addrFiles) ? Collections.emptyList() : readUrls(zkClient, statusDirPath, addrFiles);
    }

    /**
     * Read urls from data of address files
     *
     * @param zkClient      zk client
     * @param statusDirPath status directory path, e.g. /infinity/default/org.infinity.app.common.service.AppService/active
     * @param fileNames     address file names list, e.g. 172.25.11.111:16010,172.25.11.222:16010
     * @return provider urls
     */
    public static List<Url> readUrls(ZkClient zkClient, String statusDirPath, List<String> fileNames) {
        if (CollectionUtils.isEmpty(fileNames)) {
            return Collections.emptyList();
        }
        List<Url> urls = new ArrayList<>();
        for (String fileName : fileNames) {
            String fullPath = null;
            try {
                fullPath = statusDirPath + DIR_SEPARATOR_UNIX + fileName;
                String fileData = zkClient.readData(fullPath, true);
                if (StringUtils.isNotBlank(fileData)) {
                    urls.add(Url.valueOf(fileData));
                }
            } catch (Exception e) {
                log.warn(MessageFormat.format("Failed to read the file or read illegal file data for the path [{0}]", fullPath), e);
            }
        }
        return urls;
    }

    /**
     * Get the full path of provider status directory
     *
     * @param path      provider class fully-qualified name, e.g. org.infinity.app.common.service.AppService
     * @param statusDir status directory
     * @return full path of provider status directory
     */
    public static String getStatusDirPath(String path, StatusDir statusDir) {
        return String.format(PROVIDER_STATUS_DIR_PATH, path, statusDir.getValue());
    }


    /**
     * Get all child directory or file names under the specified parent path
     *
     * @param zkClient   zk client
     * @param parentPath parent directory path
     * @return names of child node
     */
    public static List<String> getChildrenNames(ZkClient zkClient, String parentPath) {
        return zkClient.exists(parentPath) ? zkClient.getChildren(parentPath) : Collections.emptyList();
    }
}
