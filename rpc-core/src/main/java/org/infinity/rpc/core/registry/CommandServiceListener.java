/*
 *  Copyright 2009-2016 Weibo, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.infinity.rpc.core.registry;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.registry.listener.CommandListener;
import org.infinity.rpc.core.registry.listener.NotifyListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;
import org.infinity.rpc.core.switcher.DefaultSwitcherService;
import org.infinity.rpc.core.switcher.SwitcherService;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;
import org.infinity.rpc.utilities.network.NetworkIpUtils;
import org.infinity.rpc.utilities.spi.ServiceInstanceLoader;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * command service manager for a url
 */
@Slf4j
public class CommandServiceListener implements CommandListener, ServiceListener {

    public static final String  MOTAN_COMMAND_SWITCHER = "feature.motanrpc.command.enable";
    private static      Pattern IP_PATTERN             = Pattern.compile("^!?[0-9.]*\\*?$");

    static {
        DefaultSwitcherService.getInstance().initSwitcher(MOTAN_COMMAND_SWITCHER, true);
    }

    /**
     * Client url
     */
    private          Url                             clientUrl;
    /**
     * Registry
     */
    private          CommandFailbackAbstractRegistry registry;
    /**
     *
     */
    private          Set<NotifyListener>             notifyListeners          = new ConcurrentHashSet<>();
    /**
     * Provider address urls map
     */
    private          Map<String, List<Url>>          groupAddressUrlsMapCache = new ConcurrentHashMap<>();
    /**
     *
     */
    private volatile RpcCommand                      rpcCommandCache;
    /**
     *
     */
    private          String                          rpcCommandStrCache       = "";

    public CommandServiceListener(Url clientUrl, CommandFailbackAbstractRegistry registry) {
        this.clientUrl = clientUrl;
        this.registry = registry;
        log.info("Created command service manager for url [{}]", clientUrl.toFullStr());
    }

    /**
     * Add notify listener to container
     *
     * @param notifyListener notify listener to be added
     */
    public void addNotifyListener(NotifyListener notifyListener) {
        notifyListeners.add(notifyListener);
    }

    /**
     * Remove notify listener from container
     *
     * @param notifyListener notify listener to be removed
     */
    public void removeNotifyListener(NotifyListener notifyListener) {
        notifyListeners.remove(notifyListener);
    }

    public Set<NotifyListener> getNotifyListeners() {
        return notifyListeners;
    }

    /**
     * @param clientUrl           client url
     * @param registryUrl         registry url
     * @param providerAddressUrls address urls
     */
    @Override
    public void onSubscribe(Url clientUrl, Url registryUrl, List<Url> providerAddressUrls) {
        if (registry == null) {
            throw new RuntimeException("Registry must be instantiated before use!");
        }

        String group = clientUrl.getParameter(Url.PARAM_GROUP);
        groupAddressUrlsMapCache.put(group, providerAddressUrls);

        List<Url> result = new ArrayList<>();
        if (rpcCommandCache != null) {
            Map<String, Integer> weights = new HashMap<>();
            result = discoverServiceWithCommand(this.clientUrl, weights, rpcCommandCache);
        } else {
            log.info("Discovering the provider urls based on group param of url when RPC command is null");
            result.addAll(discoverByUrlParamGroup(this.clientUrl));
        }

        for (NotifyListener notifyListener : notifyListeners) {
            notifyListener.onSubscribe(registry.getRegistryUrl(), result);
        }
    }

    @Override
    public void onSubscribe(Url serviceUrl, String commandString) {
        log.info("CommandServiceManager notify command. service:" + serviceUrl.toSimpleString() + ", command:" + commandString);

        if (!ServiceInstanceLoader.getServiceLoader(SwitcherService.class).load(DefaultSwitcherService.SERVICE_NAME).isOn(MOTAN_COMMAND_SWITCHER) || commandString == null) {
            log.info("command reset empty since swither is close.");
            commandString = "";
        }

        List<Url> finalResult = new ArrayList<Url>();
        Url urlCopy = serviceUrl.copy();

        if (!StringUtils.equals(commandString, rpcCommandStrCache)) {
            rpcCommandStrCache = commandString;
            rpcCommandCache = RpcCommandUtils.convertToCommand(rpcCommandStrCache);
            Map<String, Integer> weights = new HashMap<String, Integer>();

            if (rpcCommandCache != null && rpcCommandCache.getClientCommandList() != null && !rpcCommandCache.getClientCommandList().isEmpty()) {
                rpcCommandCache.sort();
                finalResult = discoverServiceWithCommand(clientUrl, weights, rpcCommandCache);
            } else {
                // 如果是指令有异常时，应当按没有指令处理，防止错误指令导致服务异常
                if (StringUtils.isNotBlank(commandString)) {
                    log.warn("command parse fail, ignored! command:" + commandString);
                    commandString = "";
                }
                // 没有命令时，只返回这个manager实际group对应的结果
                finalResult.addAll(discoverByUrlParamGroup(clientUrl));

            }

            // 指令变化时，删除不再有效的缓存，取消订阅不再有效的group
            Set<String> groupKeys = groupAddressUrlsMapCache.keySet();
            for (String gk : groupKeys) {
                if (!weights.containsKey(gk)) {
                    groupAddressUrlsMapCache.remove(gk);
                    Url urlTemp = urlCopy.copy();
                    urlTemp.addParameter(Url.PARAM_GROUP, gk);
                    registry.unsubscribeServiceListener(urlTemp, this);
                }
            }
            // 当指令从有改到无时，或者没有流量切换指令时，会触发取消订阅所有的group，需要重新订阅本组的service
            if ("".equals(commandString) || weights.isEmpty()) {
                log.info("reSub service" + clientUrl.toSimpleString());
                registry.subscribeServiceListener(clientUrl, this);
            }
        } else {
            log.info("command not change. url:" + serviceUrl.toSimpleString());
            // 指令没有变化，什么也不做
            return;
        }

        for (NotifyListener notifyListener : notifyListeners) {
            notifyListener.onSubscribe(registry.getRegistryUrl(), finalResult);
        }
    }

    public List<Url> discoverServiceWithCommand(Url serviceUrl, Map<String, Integer> weights, RpcCommand rpcCommand) {
        String localIP = NetworkIpUtils.INTRANET_IP;
        return this.discoverServiceWithCommand(serviceUrl, weights, rpcCommand, localIP);
    }

    public List<Url> discoverServiceWithCommand(Url serviceUrl, Map<String, Integer> weights, RpcCommand rpcCommand, String localIP) {
        if (rpcCommand == null || CollectionUtils.isEmpty(rpcCommand.getClientCommandList())) {
            return discoverByUrlParamGroup(serviceUrl);
        }

        List<Url> mergedResult = new LinkedList<Url>();
        String path = serviceUrl.getPath();

        List<RpcCommand.ClientCommand> clientCommandList = rpcCommand.getClientCommandList();
        boolean hit = false;
        for (RpcCommand.ClientCommand command : clientCommandList) {
            mergedResult = new LinkedList<Url>();
            // 判断当前url是否符合过滤条件
            boolean match = RpcCommandUtils.match(command.getPattern(), path);
            if (match) {
                hit = true;
                if (CollectionUtils.isNotEmpty(command.getMergeGroups())) {
                    // 计算出所有要合并的分组及权重
                    try {
                        buildWeightsMap(weights, command);
                    } catch (Exception e) {
                        log.warn("build weights map fail!" + e.getMessage());
                        continue;
                    }
                    // 根据计算结果，分别发现各个group的service，合并结果
                    mergedResult.addAll(mergeResult(serviceUrl, weights));
                } else {
                    mergedResult.addAll(discoverByUrlParamGroup(serviceUrl));
                }

                log.info("mergedResult: size-" + mergedResult.size() + " --- " + mergedResult.toString());

                if (CollectionUtils.isNotEmpty(command.getRouteRules())) {
                    log.info("router: " + command.getRouteRules().toString());

                    for (String routeRule : command.getRouteRules()) {
                        String[] fromTo = routeRule.replaceAll("\\s+", "").split("to");

                        if (fromTo.length != 2) {
                            routeRuleConfigError();
                            continue;
                        }
                        String from = fromTo[0];
                        String to = fromTo[1];
                        if (from.length() < 1 || to.length() < 1 || !IP_PATTERN.matcher(from).find() || !IP_PATTERN.matcher(to).find()) {
                            routeRuleConfigError();
                            continue;
                        }
                        boolean oppositeFrom = from.startsWith("!");
                        boolean oppositeTo = to.startsWith("!");
                        if (oppositeFrom) {
                            from = from.substring(1);
                        }
                        if (oppositeTo) {
                            to = to.substring(1);
                        }
                        int idx = from.indexOf('*');
                        boolean matchFrom;
                        if (idx != -1) {
                            matchFrom = localIP.startsWith(from.substring(0, idx));
                        } else {
                            matchFrom = localIP.equals(from);
                        }

                        // 开头有!，取反
                        if (oppositeFrom) {
                            matchFrom = !matchFrom;
                        }
                        log.info("matchFrom: " + matchFrom + ", localip:" + localIP + ", from:" + from);
                        if (matchFrom) {
                            boolean matchTo;
                            Iterator<Url> iterator = mergedResult.iterator();
                            while (iterator.hasNext()) {
                                Url url = iterator.next();
                                if (url.getProtocol().equalsIgnoreCase("rule")) {
                                    continue;
                                }
                                idx = to.indexOf('*');
                                if (idx != -1) {
                                    matchTo = url.getHost().startsWith(to.substring(0, idx));
                                } else {
                                    matchTo = url.getHost().equals(to);
                                }
                                if (oppositeTo) {
                                    matchTo = !matchTo;
                                }
                                if (!matchTo) {
                                    iterator.remove();
                                    log.info("router To not match. url remove : " + url.toSimpleString());
                                }
                            }
                        }
                    }
                }
                // 只取第一个匹配的 TODO 考虑是否能满足绝大多数场景需求
                break;
            }
        }

        List<Url> finalResult = new ArrayList<Url>();
        if (!hit) {
            finalResult = discoverByUrlParamGroup(serviceUrl);
        } else {
            finalResult.addAll(mergedResult);
        }
        return finalResult;
    }

    private void buildWeightsMap(Map<String, Integer> weights, RpcCommand.ClientCommand command) {
        for (String rule : command.getMergeGroups()) {
            String[] gw = rule.split(":");
            int weight = 1;
            if (gw.length > 1) {
                try {
                    weight = Integer.parseInt(gw[1]);
                } catch (NumberFormatException e) {
                    weightConfigError();
                }
                if (weight < 0 || weight > 100) {
                    weightConfigError();
                }
            }
            weights.put(gw[0], weight);
        }
    }

    private List<Url> mergeResult(Url url, Map<String, Integer> weights) {
        List<Url> finalResult = new ArrayList<Url>();

        if (weights.size() > 1) {
            // 将所有group及权重拼接成一个rule的URL，并作为第一个元素添加到最终结果中
            Url ruleUrl = Url.of("rule", url.getHost(), url.getPort(), url.getPath());
            StringBuilder weightsBuilder = new StringBuilder(64);
            for (Map.Entry<String, Integer> entry : weights.entrySet()) {
                weightsBuilder.append(entry.getKey()).append(':').append(entry.getValue()).append(',');
            }
            ruleUrl.addParameter(UrlParam.weights.getName(), weightsBuilder.deleteCharAt(weightsBuilder.length() - 1).toString());
            finalResult.add(ruleUrl);
        }

        for (String key : weights.keySet()) {
            if (groupAddressUrlsMapCache.containsKey(key)) {
                finalResult.addAll(groupAddressUrlsMapCache.get(key));
            } else {
                Url urlTemp = url.copy();
                urlTemp.addParameter(Url.PARAM_GROUP, key);
                finalResult.addAll(discoverByUrlParamGroup(urlTemp));
                registry.subscribeServiceListener(urlTemp, this);
            }
        }
        return finalResult;
    }

    /**
     * Discover providers address list based on group param of url
     *
     * @param clientUrl client url
     * @return address urls
     */
    private List<Url> discoverByUrlParamGroup(Url clientUrl) {
        String group = clientUrl.getParameter(Url.PARAM_GROUP);
        List<Url> addressUrls = groupAddressUrlsMapCache.get(group);
        if (addressUrls == null) {
            addressUrls = registry.discoverActiveProviders(clientUrl);
            groupAddressUrlsMapCache.put(group, addressUrls);
        }
        log.info("Discovered url by param group of url [{}]", clientUrl);
        return addressUrls;
    }

    public void setRpcCommandCache(String command) {
        rpcCommandStrCache = command;
        rpcCommandCache = RpcCommandUtils.convertToCommand(rpcCommandStrCache);
        log.info("CommandServiceManager set commandcache. commandstring:" + rpcCommandStrCache + ", comandcache "
                + (rpcCommandCache == null ? "is null." : "is not null."));
    }

    private void weightConfigError() {
        throw new RuntimeException("权重比只能是[0,100]间的整数");
    }

    private void routeRuleConfigError() {
        log.warn("路由规则配置不合法");
    }

}
