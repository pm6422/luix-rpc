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
import org.infinity.rpc.core.registry.listener.ClientListener;
import org.infinity.rpc.core.registry.listener.CommandListener;
import org.infinity.rpc.core.registry.listener.ServiceListener;
import org.infinity.rpc.core.subscribe.RpcCommand;
import org.infinity.rpc.core.subscribe.RpcCommandUtils;
import org.infinity.rpc.core.switcher.impl.SwitcherHolder;
import org.infinity.rpc.core.url.Url;
import org.infinity.rpc.utilities.annotation.EventReceiver;
import org.infinity.rpc.utilities.collection.ConcurrentHashSet;
import org.infinity.rpc.utilities.concurrent.NotThreadSafe;
import org.infinity.rpc.utilities.network.AddressUtils;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

import static org.infinity.rpc.core.constant.ServiceConstants.FORM;

/**
 * todo: CommandServiceManager
 * Command and service listener for a client.
 * One command service listener for one consumer or provider interface class.
 * Refer to {@link CommandFailbackAbstractRegistry#doSubscribe(Url, ClientListener)}
 */
@Slf4j
@NotThreadSafe
public class CommandServiceListener implements ServiceListener, CommandListener {

    public static final  String  COMMAND_SWITCHER = "feature.motanrpc.command.enable";
    private static final Pattern IP_PATTERN       = Pattern.compile("^!?[0-9.]*\\*?$");

    static {
        SwitcherHolder.getInstance().initSwitcher(COMMAND_SWITCHER, true);
    }

    /**
     * Client url
     */
    private final    Url                             consumerUrl;
    /**
     * Registry
     */
    private final    CommandFailbackAbstractRegistry registry;
    /**
     *
     */
    private final    Set<ClientListener>             clientListeners           = new ConcurrentHashSet<>();
    /**
     * Active provider urls per form map
     */
    private final    Map<String, List<Url>>          activeProviderUrlsPerForm = new ConcurrentHashMap<>();
    /**
     *
     */
    private volatile RpcCommand                      rpcCommandCache;
    /**
     *
     */
    private          String                          rpcCommandStrCache        = "";

    public CommandServiceListener(Url consumerUrl, CommandFailbackAbstractRegistry registry) {
        this.consumerUrl = consumerUrl;
        this.registry = registry;
        log.info("Created command service listener for url [{}]", consumerUrl.toFullStr());
    }

    /**
     * Add notify listener to container
     *
     * @param clientListener notify listener to be added
     */
    public void addNotifyListener(ClientListener clientListener) {
        clientListeners.add(clientListener);
    }

    /**
     * Remove notify listener from container
     *
     * @param clientListener notify listener to be removed
     */
    public void removeNotifyListener(ClientListener clientListener) {
        clientListeners.remove(clientListener);
    }

    public Set<ClientListener> getClientListeners() {
        return clientListeners;
    }

    /**
     * Service listener event
     *
     * @param consumerUrl  consumer url
     * @param registryUrl  registry url
     * @param providerUrls provider urls
     */
    @EventReceiver("providersChangeEvent")
    @Override
    public void onNotify(Url consumerUrl, Url registryUrl, List<Url> providerUrls) {
        String group = consumerUrl.getForm();
        activeProviderUrlsPerForm.put(group, providerUrls);

        List<Url> providerUrlList;
        if (rpcCommandCache != null) {
            Map<String, Integer> weights = new HashMap<>();
            providerUrlList = discoverServiceWithCommand(this.consumerUrl, weights, rpcCommandCache);
        } else {
            log.info("Discovering the active provider urls based on group param of url when RPC command is null");
            providerUrlList = new ArrayList<>(discoverActiveProvidersByGroup(this.consumerUrl));
        }

        for (ClientListener clientListener : clientListeners) {
            clientListener.onNotify(registry.getRegistryUrl(), providerUrlList);
            log.debug("Invoked event: {}", clientListener);
        }
    }

    /**
     * Command listener event
     *
     * @param consumerUrl   consumer url
     * @param commandString command string
     */
    @Override
    @EventReceiver("providerDataChangeEvent")
    public void onNotify(Url consumerUrl, String commandString) {
        log.info("CommandServiceManager notify command. service:" + consumerUrl.toSimpleString() + ", command:" + commandString);

        if (!SwitcherHolder.getInstance().isOn(COMMAND_SWITCHER) || commandString == null) {
            log.info("command reset empty since switcher is close.");
            commandString = "";
        }

        List<Url> result;
        Url urlCopy = consumerUrl.copy();

        if (!StringUtils.equals(commandString, rpcCommandStrCache)) {
            rpcCommandStrCache = commandString;
            rpcCommandCache = RpcCommandUtils.convertToCommand(rpcCommandStrCache);
            Map<String, Integer> weights = new HashMap<>();

            if (rpcCommandCache != null && rpcCommandCache.getClientCommandList() != null && !rpcCommandCache.getClientCommandList().isEmpty()) {
                rpcCommandCache.sort();
                result = discoverServiceWithCommand(this.consumerUrl, weights, rpcCommandCache);
            } else {
                // 如果是指令有异常时，应当按没有指令处理，防止错误指令导致服务异常
                if (StringUtils.isNotBlank(commandString)) {
                    log.warn("command parse fail, ignored! command:" + commandString);
                    commandString = "";
                }
                // 没有命令时，只返回这个manager实际group对应的结果
                result = new ArrayList<>(discoverActiveProvidersByGroup(this.consumerUrl));

            }

            // 指令变化时，删除不再有效的缓存，取消订阅不再有效的group
            Set<String> groupKeys = activeProviderUrlsPerForm.keySet();
            for (String gk : groupKeys) {
                if (!weights.containsKey(gk)) {
                    activeProviderUrlsPerForm.remove(gk);
                    Url urlTemp = urlCopy.copy();
                    urlTemp.addOption(FORM, gk);
                    registry.unsubscribeServiceListener(urlTemp, this);
                }
            }
            // 当指令从有改到无时，或者没有流量切换指令时，会触发取消订阅所有的group，需要重新订阅本组的service
            if ("".equals(commandString) || weights.isEmpty()) {
                log.info("reSub service" + this.consumerUrl.toSimpleString());
                registry.subscribeServiceListener(this.consumerUrl, this);
            }
        } else {
            log.info("command not change. url:" + consumerUrl.toSimpleString());
            // 指令没有变化，什么也不做
            return;
        }

        for (ClientListener clientListener : clientListeners) {
            clientListener.onNotify(registry.getRegistryUrl(), result);
        }
    }

    public List<Url> discoverServiceWithCommand(Url providerUrl, Map<String, Integer> weights, RpcCommand rpcCommand) {
        return this.discoverServiceWithCommand(providerUrl, weights, rpcCommand, AddressUtils.getLocalIp());
    }

    public List<Url> discoverServiceWithCommand(Url providerUrl, Map<String, Integer> weights,
                                                RpcCommand rpcCommand, String localIp) {
        if (rpcCommand == null || CollectionUtils.isEmpty(rpcCommand.getClientCommandList())) {
            return discoverActiveProvidersByGroup(providerUrl);
        }

        List<Url> mergedResult = new LinkedList<>();
        String path = providerUrl.getPath();

        List<RpcCommand.ClientCommand> clientCommandList = rpcCommand.getClientCommandList();
        boolean hit = false;
        for (RpcCommand.ClientCommand command : clientCommandList) {
            mergedResult = new LinkedList<>();
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
                    mergedResult.addAll(mergeResult(providerUrl, weights));
                } else {
                    mergedResult.addAll(discoverActiveProvidersByGroup(providerUrl));
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
                            matchFrom = localIp.startsWith(from.substring(0, idx));
                        } else {
                            matchFrom = localIp.equals(from);
                        }

                        // 开头有!，取反
                        if (oppositeFrom) {
                            matchFrom = !matchFrom;
                        }
                        log.info("matchFrom: " + matchFrom + ", localIp:" + localIp + ", from:" + from);
                        if (matchFrom) {
                            boolean matchTo;
                            Iterator<Url> iterator = mergedResult.iterator();
                            while (iterator.hasNext()) {
                                Url url = iterator.next();
                                if ("rule".equalsIgnoreCase(url.getProtocol())) {
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

        List<Url> finalResult;
        if (!hit) {
            finalResult = discoverActiveProvidersByGroup(providerUrl);
        } else {
            finalResult = new ArrayList<>(mergedResult);
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

    private List<Url> mergeResult(Url providerUrl, Map<String, Integer> weights) {
        List<Url> finalResult = new ArrayList<>();

        if (weights.size() > 1) {
            // 将所有group及权重拼接成一个rule的URL，并作为第一个元素添加到最终结果中
            Url ruleUrl = Url.of("rule", providerUrl.getHost(), providerUrl.getPort(), providerUrl.getPath());
            StringBuilder weightsBuilder = new StringBuilder(64);
            for (Map.Entry<String, Integer> entry : weights.entrySet()) {
                weightsBuilder.append(entry.getKey()).append(':').append(entry.getValue()).append(',');
            }
            ruleUrl.addOption(Url.PARAM_WEIGHT, weightsBuilder.deleteCharAt(weightsBuilder.length() - 1).toString());
            finalResult.add(ruleUrl);
        }

        for (String key : weights.keySet()) {
            if (activeProviderUrlsPerForm.containsKey(key)) {
                finalResult.addAll(activeProviderUrlsPerForm.get(key));
            } else {
                Url urlTemp = providerUrl.copy();
                urlTemp.addOption(FORM, key);
                finalResult.addAll(discoverActiveProvidersByGroup(urlTemp));
                registry.subscribeServiceListener(urlTemp, this);
            }
        }
        return finalResult;
    }

    /**
     * Discover providers urls based on group param of url
     *
     * @param consumerUrl consumer url
     * @return active provider urls
     */
    private List<Url> discoverActiveProvidersByGroup(Url consumerUrl) {
        String group = consumerUrl.getForm();
        List<Url> providerUrls = activeProviderUrlsPerForm.get(group);
        if (providerUrls == null) {
            providerUrls = registry.discoverActiveProviders(consumerUrl);
            activeProviderUrlsPerForm.put(group, providerUrls);
        }
        log.info("Discovered url by param group of url [{}]", consumerUrl);
        return providerUrls;
    }

    public void setRpcCommandCache(String command) {
        rpcCommandStrCache = command;
        rpcCommandCache = RpcCommandUtils.convertToCommand(rpcCommandStrCache);
        log.info("CommandServiceManager set command cache. command string:" + rpcCommandStrCache + ", command cache "
                + (rpcCommandCache == null ? "is null." : "is not null."));
    }

    private void weightConfigError() {
        throw new RuntimeException("权重比只能是[0,100]间的整数");
    }

    private void routeRuleConfigError() {
        log.warn("路由规则配置不合法");
    }

    @Override
    public String toString() {
        return CommandServiceListener.class.getSimpleName().concat(":").concat(consumerUrl.getPath());
    }
}
