/**
 * Controllers
 */
angular
    .module('smartcloudserviceApp')
    .controller('MainController', MainController)
    .controller('LeftSidebarController', LeftSidebarController)
    .controller('ErrorPageController', ErrorPageController)
    .controller('NavbarController', NavbarController)
    .controller('FooterController', FooterController)
    .controller('MetricsController', MetricsController)
    .controller('MetricsDialogController', MetricsDialogController)
    .controller('HealthController', HealthController)
    .controller('HealthDialogController', HealthDialogController)
    .controller('ConfigurationController', ConfigurationController)
    .controller('BeansController', BeansController)
    .controller('BeanDialogController', BeanDialogController)
    .controller('MappingsController', MappingsController)
    .controller('HttpTraceController', HttpTraceController)
    .controller('HttpSessionController', HttpSessionController)
    .controller('LoggerController', LoggerController)
    .controller('TimingTaskListController', TimingTaskListController)
    .controller('TimingTaskDialogController', TimingTaskDialogController)
    .controller('TimingTaskHistoryListController', TimingTaskHistoryListController)
    .controller('TimingTaskHistoryDetailsController', TimingTaskHistoryDetailsController)
    .controller('ScheduleController', ScheduleController)
    .controller('ControlController', ControlController)
    .controller('AppListController', AppListController)
    .controller('AppDialogController', AppDialogController)
    .controller('AppDetailsController', AppDetailsController)
    .controller('AuthorityListController', AuthorityListController)
    .controller('AuthorityDialogController', AuthorityDialogController);

/**
 * MainController - controller
 * Contains several global data used in different view
 *
 */
function MainController($http, $rootScope, $scope, $state, AdminMenuService, AlertUtils, APP_NAME, COMPANY_NAME) {
    var main = this;
    main.account = null;
    main.isAuthenticated = null;
    main.links = [];
    main.selectedLink = null;
    main.selectLink = selectLink;
    $rootScope.companyName = COMPANY_NAME;

    function selectLink($item, $model, $label, $event) {
        $state.go(main.selectedLink.url);
    }
}

/**
 * LeftSidebarController
 */
function LeftSidebarController($scope, $state, $element, $timeout, APP_NAME, AdminMenuService) {
    var vm = this;

    vm.init = init;
    vm.groups = [];

    vm.init();

    function init() {
        AdminMenuService.query({}, function (response) {
            if (response) {
                vm.groups = response;
                // Call the metsiMenu plugin and plug it to sidebar navigation
                $timeout(function () {
                    $element.metisMenu();
                });
            }
        }, function (errorResponse) {
        });
    }
}

/**
 * ErrorPageController
 */
function ErrorPageController($state, $stateParams, $scope, JSONFormatterConfig) {
    var vm = this;

    vm.errorMessage = $stateParams.errorMessage;
}

/**
 * NavbarController
 */
function NavbarController($rootScope, $scope, $translate, $state, ProfileService) {
    var vm = this;

    vm.isNavbarCollapsed = true;
    vm.changeLanguage = changeLanguage;

    ProfileService.getProfileInfo().then(function (response) {
        vm.inProduction = response.inProduction;
        vm.swaggerEnabled = response.swaggerDisabled;
    });

    vm.toggleNavbar = toggleNavbar;
    vm.collapseNavbar = collapseNavbar;
    vm.$state = $state;

    $rootScope.isNavbarLoaded = true;

    function changeLanguage(langKey) {
        $translate.use(langKey);
        $scope.language = langKey;
    }

    function toggleNavbar() {
        vm.isNavbarCollapsed = !vm.isNavbarCollapsed;
    }

    function collapseNavbar() {
        vm.isNavbarCollapsed = true;
    }
}

/**
 * FooterController
 */
function FooterController($http) {
    var vm = this;
}

/**
 * MetricsController
 *
 */
function MetricsController($state, $scope, $uibModal, MetricsService, metrics) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.cachesStats = {};
    vm.metrics = metrics;
    vm.refresh = refresh;
    vm.refreshThreadDumpData = refreshThreadDumpData;
    vm.servicesStats = {};
    vm.updatingMetrics = false;
    /**
     * Options for Doughnut chart
     */
    vm.doughnutOptions = {
        segmentShowStroke: true,
        segmentStrokeColor: '#fff',
        segmentStrokeWidth: 2,
        percentageInnerCutout: 45, // This is 0 for Pie charts
        animationSteps: 100,
        animationEasing: 'easeOutBounce',
        animateRotate: true,
        animateScale: false
    };

    vm.totalMemory = [
        {
            value: vm.metrics.gauges['jvm.memory.total.used'].value / 1000000,
            color: '#a3e1d4',
            highlight: '#4424bc',
            label: '已使用'
        },
        {
            value: (vm.metrics.gauges['jvm.memory.total.max'].value - vm.metrics.gauges['jvm.memory.total.used'].value) / 1000000,
            color: '#dedede',
            highlight: '#4424bc',
            label: '未使用'
        }
    ];

    vm.heapMemory = [
        {
            value: vm.metrics.gauges['jvm.memory.heap.used'].value / 1000000,
            color: '#a3e1d4',
            highlight: '#4424bc',
            label: '已使用'
        },
        {
            value: (vm.metrics.gauges['jvm.memory.heap.max'].value - vm.metrics.gauges['jvm.memory.heap.used'].value) / 1000000,
            color: '#dedede',
            highlight: '#4424bc',
            label: '未使用'
        }
    ];

    vm.edenSpaceMemory = [
        {
            value: vm.metrics.gauges['jvm.memory.pools.PS-Eden-Space.used'].value / 1000000,
            color: '#a3e1d4',
            highlight: '#4424bc',
            label: '已使用'
        },
        {
            value: (vm.metrics.gauges['jvm.memory.pools.PS-Eden-Space.max'].value - vm.metrics.gauges['jvm.memory.pools.PS-Eden-Space.used'].value) / 1000000,
            color: '#dedede',
            highlight: '#4424bc',
            label: '未使用'
        }
    ];

    vm.survivorSpaceMemory = [
        {
            value: vm.metrics.gauges['jvm.memory.pools.PS-Survivor-Space.used'].value / 1000000,
            color: '#a3e1d4',
            highlight: '#4424bc',
            label: '已使用'
        },
        {
            value: (vm.metrics.gauges['jvm.memory.pools.PS-Survivor-Space.max'].value - vm.metrics.gauges['jvm.memory.pools.PS-Survivor-Space.used'].value) / 1000000,
            color: '#dedede',
            highlight: '#4424bc',
            label: '未使用'
        }
    ];

    vm.oldSpaceMemory = [
        {
            value: vm.metrics.gauges['jvm.memory.pools.PS-Old-Gen.used'].value / 1000000,
            color: '#a3e1d4',
            highlight: '#4424bc',
            label: '已使用'
        },
        {
            value: (vm.metrics.gauges['jvm.memory.pools.PS-Old-Gen.max'].value - vm.metrics.gauges['jvm.memory.pools.PS-Old-Gen.used'].value) / 1000000,
            color: '#dedede',
            highlight: '#4424bc',
            label: '未使用'
        }
    ];

    vm.nonHeapMemory = [
        {
            value: vm.metrics.gauges['jvm.memory.non-heap.used'].value / 1000000,
            color: '#a3e1d4',
            highlight: '#4424bc',
            label: '已使用'
        },
        {
            value: (vm.metrics.gauges['jvm.memory.non-heap.committed'].value - vm.metrics.gauges['jvm.memory.non-heap.used'].value) / 1000000,
            color: '#dedede',
            highlight: '#4424bc',
            label: '未使用'
        }
    ];

    vm.runnableThreads = [
        {
            value: vm.metrics.gauges['jvm.threads.runnable.count'].value,
            color: '#a3e1d4',
            highlight: '#4424bc',
            label: 'Runnable'
        },
        {
            value: (vm.metrics.gauges['jvm.threads.count'].value - vm.metrics.gauges['jvm.threads.runnable.count'].value),
            color: '#dedede',
            highlight: '#4424bc',
            label: 'Others'
        }
    ];

    vm.timedWaitingThreads = [
        {
            value: vm.metrics.gauges['jvm.threads.timed_waiting.count'].value,
            color: '#a3e1d4',
            highlight: '#4424bc',
            label: 'Timed waiting'
        },
        {
            value: (vm.metrics.gauges['jvm.threads.count'].value - vm.metrics.gauges['jvm.threads.timed_waiting.count'].value),
            color: '#dedede',
            highlight: '#4424bc',
            label: 'Others'
        }
    ];

    vm.waitingThreads = [
        {
            value: vm.metrics.gauges['jvm.threads.waiting.count'].value,
            color: '#a3e1d4',
            highlight: '#4424bc',
            label: 'Waiting'
        },
        {
            value: (vm.metrics.gauges['jvm.threads.count'].value - vm.metrics.gauges['jvm.threads.waiting.count'].value),
            color: '#dedede',
            highlight: '#4424bc',
            label: 'Others'
        }
    ];

    vm.blockedThreads = [
        {
            value: vm.metrics.gauges['jvm.threads.blocked.count'].value,
            color: '#a3e1d4',
            highlight: '#4424bc',
            label: 'Blocked'
        },
        {
            value: (vm.metrics.gauges['jvm.threads.count'].value - vm.metrics.gauges['jvm.threads.blocked.count'].value),
            color: '#dedede',
            highlight: '#4424bc',
            label: 'Others'
        }
    ];

    $scope.$watch('vm.metrics', function (newValue) {
        vm.servicesStats = {};
        vm.cachesStats = {};
        angular.forEach(newValue.timers, function (value, key) {
            if (key.indexOf('controller.') !== -1) {
                var controllerStartIndex = key.indexOf('controller.');
                var offset = 'controller.'.length;
                vm.servicesStats[key.substr(controllerStartIndex + offset, key.length)] = value;
            }
            if (key.indexOf('service.impl.') !== -1) {
                var controllerStartIndex = key.indexOf('service.impl.');
                var offset = 'service.impl.'.length;
                vm.servicesStats[key.substr(controllerStartIndex + offset, key.length)] = value;
            }
            if (key.indexOf('net.sf.ehcache.Cache') !== -1) {
                // remove gets or puts
                var index = key.lastIndexOf('.');
                var newKey = key.substr(0, index);

                // Keep the name of the domain
                index = newKey.lastIndexOf('.');
                vm.cachesStats[newKey] = {
                    'name': newKey.substr(index + 1),
                    'value': value
                };
            }
        });
    });

    function refresh() {
        vm.updatingMetrics = true;
        MetricsService.getMetrics().then(function (promise) {
            vm.metrics = promise;
            vm.updatingMetrics = false;
        }, function (promise) {
            vm.metrics = promise.data;
            vm.updatingMetrics = false;
        });
    }

    function refreshThreadDumpData() {
        MetricsService.threadDump().then(function (data) {
            $uibModal.open({
                templateUrl: 'app/views/developer/metrics/metrics.modal.html',
                controller: 'MetricsDialogController',
                controllerAs: 'vm',
                size: 'lg',
                resolve: {
                    threadDump: function () {
                        return data.threads;
                    }
                }
            });
        });
    }
}

/**
 * MetricsDialogController
 *
 */
function MetricsDialogController($uibModalInstance, threadDump) {
    var vm = this;

    vm.cancel = cancel;
    vm.getLabelClass = getLabelClass;
    vm.threadDump = threadDump;
    vm.threadDumpAll = 0;
    vm.threadDumpBlocked = 0;
    vm.threadDumpRunnable = 0;
    vm.threadDumpTimedWaiting = 0;
    vm.threadDumpWaiting = 0;

    angular.forEach(threadDump, function (value) {
        if (value.threadState === 'RUNNABLE') {
            vm.threadDumpRunnable += 1;
        } else if (value.threadState === 'WAITING') {
            vm.threadDumpWaiting += 1;
        } else if (value.threadState === 'TIMED_WAITING') {
            vm.threadDumpTimedWaiting += 1;
        } else if (value.threadState === 'BLOCKED') {
            vm.threadDumpBlocked += 1;
        }
    });

    vm.threadDumpAll = vm.threadDumpRunnable + vm.threadDumpWaiting +
        vm.threadDumpTimedWaiting + vm.threadDumpBlocked;

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }

    function getLabelClass(threadState) {
        if (threadState === 'RUNNABLE') {
            return 'label-success';
        } else if (threadState === 'WAITING') {
            return 'label-info';
        } else if (threadState === 'TIMED_WAITING') {
            return 'label-warning';
        } else if (threadState === 'BLOCKED') {
            return 'label-danger';
        }
    }
}

function HealthController($state, HealthService, $uibModal) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.updatingHealth = true;
    vm.getLabelClass = getLabelClass;
    vm.refresh = refresh;
    vm.showHealth = showHealth;
    vm.baseName = HealthService.getBaseName;
    vm.subSystemName = HealthService.getSubSystemName;

    vm.refresh();

    function getLabelClass(statusState) {
        if (statusState === 'UP') {
            return 'label-primary';
        } else {
            return 'label-danger';
        }
    }

    function refresh() {
        vm.updatingHealth = true;
        HealthService.checkHealth().then(function (response) {
            vm.healthData = HealthService.transformHealthData(response);
            vm.updatingHealth = false;
        }, function (response) {
            vm.healthData = HealthService.transformHealthData(response.data);
            vm.updatingHealth = false;
        });
    }

    function showHealth(health) {
        $uibModal.open({
            templateUrl: 'app/views/developer/health/health.dialog.html',
            controller: 'HealthDialogController',
            controllerAs: 'vm',
            size: 'lg',
            resolve: {
                currentHealth: function () {
                    return health;
                },
                baseName: function () {
                    return vm.baseName;
                },
                subSystemName: function () {
                    return vm.subSystemName;
                }
            }
        });
    }
}

function HealthDialogController($uibModalInstance, currentHealth, baseName, subSystemName) {
    var vm = this;

    vm.cancel = cancel;
    vm.currentHealth = currentHealth;
    vm.baseName = baseName;
    vm.subSystemName = subSystemName;

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }
}

function ConfigurationController($state, ConfigurationService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.allConfiguration = null;
    vm.configuration = null;
    vm.configKeys = [];

    ConfigurationService.get().then(function (configuration) {
        vm.configuration = configuration;

        for (var config in configuration) {
            if (config.properties !== undefined) {
                vm.configKeys.push(Object.keys(config.properties));
            }
        }
    });
    ConfigurationService.getEnv().then(function (configuration) {
        vm.allConfiguration = configuration;
    });
}

function BeansController($state, $http, $uibModal, APP_NAME) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.items = null;
    vm.refresh = refresh;
    vm.showBean = showBean;
    vm.refresh();

    function refresh() {
        $http.get('management/beans').then(function (response) {
            vm.items = [];
            angular.forEach(response.data['contexts'][APP_NAME]['beans'], function (val, key) {
                vm.items.push({bean: key, type: val.type, scope: val.scope, dependencies: val.dependencies});
            });
        });
    }

    function showBean(name) {
        $uibModal.open({
            templateUrl: 'app/views/developer/beans/bean.dialog.html',
            controller: 'BeanDialogController',
            controllerAs: 'vm',
            size: 'lg',
            resolve: {
                name: function () {
                    return name;
                },
                beanDetails: function () {
                    return $http.get('api/systems/bean', {
                        params: {
                            'name': name
                        }
                    }).then(function (response) {
                        return response.data;
                    });
                }
            }
        });
    }
}

function BeanDialogController($uibModalInstance, name, beanDetails) {
    var vm = this;

    vm.cancel = cancel;
    vm.name = name;
    vm.beanDetails = beanDetails;

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }
}

function MappingsController($state, $http, APP_NAME) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.items = [];
    vm.refresh = refresh;
    vm.refresh();

    function refresh() {
        $http.get('management/mappings').then(function (response) {
            var mappings = response.data['contexts'][APP_NAME]['mappings'];

            for (var key in mappings) {
                if (key === 'dispatcherServlets') {
                    angular.forEach(mappings[key]['dispatcherServlet'], function (v, k) {
                        vm.items.push({url: v.predicate, handler: v.handler});
                    });
                } else if (key === 'servletFilters') {
                    angular.forEach(mappings[key], function (v, k) {
                        vm.items.push({url: v.urlPatternMappings, handler: v.className});
                    });
                } else if (key === 'servlets') {
                    angular.forEach(mappings[key], function (v, k) {
                        vm.items.push({url: v.mappings, handler: v.className});
                    });
                }
            }
        });
    }
}

function HttpTraceController($state, $http) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.refresh = refresh;
    vm.refresh();

    function refresh() {
        $http.get('management/httptrace').then(function (response) {
            vm.items = response.data.traces;
        });
    }
}

/**
 * HttpSessionController
 */
function HttpSessionController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, HttpSessionService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;

    vm.loadAll();

    function loadAll() {
        HttpSessionService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort(),
            principal: vm.criteria.principal
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'principal') {
            // default sort column
            result.push('principal,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            principal: vm.criteria.principal
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function del(id) {
        AlertUtils.createDeleteConfirmation('数据有可能被其他数据所引用，删除之后可能出现一些问题，您确定删除吗?', function (isConfirm) {
            if (isConfirm) {
                HttpSessionService.del({id: id},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }
}

/**
 * LoggerController
 */
function LoggerController($state, LoggerService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.changeLevel = changeLevel;
    vm.query = query;

    vm.query();

    function query() {
        LoggerService.query({}, function (data) {
            vm.loggers = [];
            angular.forEach(data.loggers, function (val, key) {
                vm.loggers.push({name: key, level: val.effectiveLevel});
            });
        });
    }

    function changeLevel(name, level) {
        // The first argument is the path variable, the second one is request body
        LoggerService.changeLevel({name: name}, {configuredLevel: level}, function () {
            vm.query();
        });
    }
}

/**
 * TimingTaskListController
 */
function TimingTaskListController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, TimingTaskService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.setEnabled = setEnabled;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;
    vm.goToHistory = goToHistory;

    vm.loadAll();

    function loadAll() {
        TimingTaskService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort(),
            name: vm.criteria.name,
            beanName: vm.criteria.beanName,
            methodName: vm.criteria.methodName
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'name') {
            // default sort column
            result.push('name,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            name: vm.criteria.name,
            beanName: vm.criteria.beanName,
            methodName: vm.criteria.methodName
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function setEnabled(entity, enabled) {
        entity.enabled = enabled;
        TimingTaskService.update(entity,
            function () {
                vm.loadAll();
            },
            function () {
                entity.enabled = !enabled;
            });
    }

    function del(id) {
        AlertUtils.createDeleteConfirmation('The data may be referenced by other data, and there may be some problems after deletion, are you sure to delete?', function (isConfirm) {
            if (isConfirm) {
                TimingTaskService.del({id: id},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }

    function goToHistory(name) {
        $state.go('timing-task-history-list', {'name': name});
    }
}

/**
 * TimingTaskDialogController
 */
function TimingTaskDialogController($state, $stateParams, $uibModalInstance, TimingTaskService, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.mode = $state.current.data.mode;
    vm.beanNames = TimingTaskService.queryBeans({id: 'beans'});
    vm.timeUnits = TimingTaskService.queryTimeUnits({id: 'time-units'});
    vm.entity = entity;
    vm.isSaving = false;
    vm.save = save;
    vm.cancel = cancel;

    function save() {
        vm.isSaving = true;
        if (vm.mode == 'edit') {
            TimingTaskService.update(vm.entity, onSaveSuccess, onSaveError);
        } else {
            TimingTaskService.create(vm.entity, onSaveSuccess, onSaveError);
        }
    }

    function onSaveSuccess(result) {
        vm.isSaving = false;
        $uibModalInstance.close(result);
    }

    function onSaveError(result) {
        vm.isSaving = false;
    }

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }
}

/**
 * TimingTaskHistoryListController
 */
function TimingTaskHistoryListController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, TimingTaskHistoryService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;

    vm.loadAll();

    function loadAll() {
        TimingTaskHistoryService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort(),
            name: vm.criteria.name
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'name') {
            // default sort column
            result.push('name,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc'),
            name: vm.criteria.name
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }
}

/**
 * TimingTaskHistoryDetailsController
 */
function TimingTaskHistoryDetailsController($state, $stateParams, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.grandfatherPageTitle = $state.$current.parent.parent.data.pageTitle;
    vm.entity = entity;
}

/**
 * ScheduleController
 */
function ScheduleController($state, $http) {
    var vm = this;
    vm.data = {};

    vm.pageTitle = $state.current.data.pageTitle;

    $http.get('management/scheduledtasks').then(function (response) {
        vm.data = response.data;
    });
}

/**
 * ControlController
 */
function ControlController($state, $http, AlertUtils) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.items = null;
    vm.shutdown = shutdown;

    function shutdown() {
        $http.post('management/shutdown').then(function (response) {
                AlertUtils.success('Shutdown successfully', {});
            },
            function (response) {
                AlertUtils.error('Shutdown failed', {});
            });
    }
}

/**
 * AppListController
 */
function AppListController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, AppService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.setEnabled = setEnabled;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;

    vm.loadAll();

    function loadAll() {
        AppService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort()
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'name') {
            // default sort column
            result.push('name,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function setEnabled(entity, enabled) {
        entity.enabled = enabled;
        AppService.update(entity,
            function () {
                vm.loadAll();
            },
            function () {
                entity.enabled = !enabled;
            });
    }

    function del(name) {
        AlertUtils.createDeleteConfirmation('The data may be referenced by other data, and there may be some problems after deletion, are you sure to delete?', function (isConfirm) {
            if (isConfirm) {
                AppService.del({name: name},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }
}

/**
 * AppDialogController
 */
function AppDialogController($state, $stateParams, $uibModalInstance, AppService, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.mode = $state.current.data.mode;
    vm.entity = entity;
    vm.isSaving = false;
    vm.save = save;
    vm.cancel = cancel;

    function save() {
        vm.isSaving = true;
        if (vm.mode == 'edit') {
            AppService.update(vm.entity, onSaveSuccess, onSaveError);
        } else {
            AppService.save(vm.entity, onSaveSuccess, onSaveError);
        }
    }

    function onSaveSuccess(result) {
        vm.isSaving = false;
        $uibModalInstance.close(result);
    }

    function onSaveError(result) {
        vm.isSaving = false;
    }

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }
}

/**
 * AppDetailsController
 */
function AppDetailsController($state, $stateParams, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.grandfatherPageTitle = $state.$current.parent.parent.data.pageTitle;
    vm.entity = entity;
}

/**
 * AuthorityListController
 */
function AuthorityListController($state, AlertUtils, ParseLinksUtils, PAGINATION_CONSTANTS, pagingParams, criteria, AuthorityService) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.parentPageTitle = $state.$current.parent.data.pageTitle;
    vm.links = null;
    vm.loadAll = loadAll;
    vm.loadPage = loadPage;
    vm.checkPressEnter = checkPressEnter;
    vm.page = 1;
    vm.setEnabled = setEnabled;
    vm.totalItems = null;
    vm.entities = [];
    vm.predicate = pagingParams.predicate;
    vm.reverse = pagingParams.ascending;
    vm.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    vm.transition = transition;
    vm.criteria = criteria;
    vm.del = del;

    vm.loadAll();

    function loadAll() {
        AuthorityService.query({
            page: pagingParams.page - 1,
            size: vm.itemsPerPage,
            sort: sort()
        }, function (result, headers) {
            vm.links = ParseLinksUtils.parse(headers('link'));
            vm.totalItems = headers('X-Total-Count');
            vm.page = pagingParams.page;
            vm.entities = result;
        });
    }

    function sort() {
        var result = [vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')];
        if (vm.predicate !== 'name') {
            // default sort column
            result.push('name,asc');
        }
        return result;
    }

    function loadPage(page) {
        vm.page = page;
        vm.transition();
    }

    function transition() {
        $state.transitionTo($state.$current, {
            page: vm.page,
            sort: vm.predicate + ',' + (vm.reverse ? 'asc' : 'desc')
        });
    }

    function checkPressEnter($event) {
        //按下enter键重新查询数据
        if ($event.keyCode == 13) {
            vm.transition();
        }
    }

    function setEnabled(entity, enabled) {
        entity.enabled = enabled;
        AuthorityService.update(entity,
            function () {
                vm.loadAll();
            },
            function () {
                entity.enabled = !enabled;
            });
    }

    function del(name) {
        AlertUtils.createDeleteConfirmation('The data may be referenced by other data, and there may be some problems after deletion, are you sure to delete?', function (isConfirm) {
            if (isConfirm) {
                AuthorityService.del({name: name},
                    function () {
                        vm.loadAll();
                    },
                    function () {
                    });
            }
        });
    }
}

/**
 * AuthorityDialogController
 */
function AuthorityDialogController($state, $stateParams, $uibModalInstance, AuthorityService, entity) {
    var vm = this;

    vm.pageTitle = $state.current.data.pageTitle;
    vm.mode = $state.current.data.mode;
    vm.entity = entity;
    vm.isSaving = false;
    vm.save = save;
    vm.cancel = cancel;

    function save() {
        vm.isSaving = true;
        if (vm.mode == 'edit') {
            AuthorityService.update(vm.entity, onSaveSuccess, onSaveError);
        } else {
            AuthorityService.save(vm.entity, onSaveSuccess, onSaveError);
        }
    }

    function onSaveSuccess(result) {
        vm.isSaving = false;
        $uibModalInstance.close(result);
    }

    function onSaveError(result) {
        vm.isSaving = false;
    }

    function cancel() {
        $uibModalInstance.dismiss('cancel');
    }
}
