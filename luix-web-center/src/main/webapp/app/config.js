/**
 * Using state to manage routing and views
 * Each view are defined as state.
 * Initial there are written state for all view in theme.
 *
 */
angular
    .module('smartcloudserviceApp')
    .config(stateConfig)
    .config(paginationConfig)
    .config(pagerConfig)
    .config(httpConfig)
    .config(localStorageConfig)
    .config(compileServiceConfig)
    .run(function ($rootScope, $state) {
        $rootScope.$state = $state;
        $rootScope.now = new Date(); // Set system time
        $rootScope.parseDate = function (dateString) {
            return new Date(dateString);
        }
    });

function stateConfig($stateProvider, $urlRouterProvider, $ocLazyLoadProvider, IdleProvider, KeepaliveProvider, APP_NAME) {

    // Configure Idle settings
    IdleProvider.idle(5); // in seconds
    IdleProvider.timeout(120); // in seconds

    $urlRouterProvider.otherwise('/');

    $ocLazyLoadProvider.config({
        // Set to true if you want to see what and when is dynamically loaded
        debug: false
    });

    $stateProvider
        .state('layout', {
            abstract: true,
            templateUrl: 'app/views/common/layout.html'
        })
        .state('dashboard', {
            parent: 'layout',
            url: '/',
            views: {
                'content@': {
                    templateUrl: 'app/views/common/dashboard.html'
                }
            },
            data: {
                pageTitle: 'Dashboard',
                authorities: ['ROLE_ADMIN', 'ROLE_DEVELOPER', 'ROLE_USER']
            },
            resolve: {
                loadPlugin: function ($ocLazyLoad) {
                    return $ocLazyLoad.load([
                        {
                            serie: true,
                            name: 'angular-flot',
                            files: ['content/js/plugins/flot/jquery.flot.js', 'content/js/plugins/flot/jquery.flot.time.js', 'content/js/plugins/flot/jquery.flot.tooltip.min.js', 'content/js/plugins/flot/jquery.flot.spline.js', 'content/js/plugins/flot/jquery.flot.resize.js', 'content/js/plugins/flot/jquery.flot.pie.js', 'content/js/plugins/flot/curvedLines.js', 'content/js/plugins/flot/angular-flot.js',]
                        },
                        {
                            name: 'angular-peity',
                            files: ['content/js/plugins/peity/jquery.peity.min.js', 'content/js/plugins/peity/angular-peity.js']
                        }
                    ]);
                }
            }
        })
        .state('error', {
            parent: 'layout',
            url: '/error',
            views: {
                'content@': {
                    templateUrl: 'app/views/common/error.html',
                    controller: 'ErrorPageController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Error page',
                authorities: []
            },
            params: {
                errorMessage: ''
            }
        })
        .state('accessdenied', {
            parent: 'layout',
            url: '/accessdenied',
            views: {
                'content@': {
                    templateUrl: 'app/views/common/accessdenied.html'
                }
            },
            data: {
                pageTitle: 'Access denied',
                authorities: []
            }
        })
        .state('login', {
            url: '/login',
            templateUrl: 'app/views/common/login.html',
            controller: 'LoginController',
            controllerAs: 'vm',
            data: {
                pageTitle: 'Sign in',
                specialClass: 'gray-bg'
            }
        })
        .state('register', {
            url: '/register',
            templateUrl: 'app/views/common/register.html',
            controller: 'RegisterController',
            controllerAs: 'vm',
            data: {
                pageTitle: 'Sign up',
                specialClass: 'gray-bg register-background-img'
            }
        })
        .state('activate', {
            url: '/activate?key',
            templateUrl: 'app/views/common/activate.html',
            controller: 'ActivationController',
            controllerAs: 'vm',
            data: {
                pageTitle: 'Activate account',
                specialClass: 'gray-bg activate-background-img'
            }
        })
        .state('forgot-password', {
            url: '/forgot-password',
            templateUrl: 'app/views/common/forgot-password.html',
            controller: 'ForgotPasswordController',
            controllerAs: 'vm',
            data: {
                pageTitle: 'Forgot password',
                specialClass: 'gray-bg forget-password-background-img'
            }
        })
        .state('reset-password', {
            url: '/reset-password?key',
            templateUrl: 'app/views/common/reset-password.html',
            controller: 'ResetPasswordController',
            controllerAs: 'vm',
            data: {
                pageTitle: 'Reset password',
                specialClass: 'gray-bg'
            }
        })
        .state('user', {
            abstract: true,
            parent: 'layout',
            data: {
                authorities: ['ROLE_USER']
            }
        })
        .state('contact-us', {
            parent: 'user',
            url: '/contact-us',
            views: {
                'content@': {
                    templateUrl: 'app/views/user/contact-us/contact-us.html',
                    controller: 'ContactUsController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Contact Us'
            }
        })
        .state('profile', {
            parent: 'user',
            url: '/profile',
            views: {
                'content@': {
                    templateUrl: 'app/views/user/profile/profile.html',
                    controller: 'ProfileController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'User profile'
            }
        })
        .state('password', {
            parent: 'user',
            url: '/password',
            views: {
                'content@': {
                    templateUrl: 'app/views/user/password/password.html',
                    controller: 'PasswordController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Change password'
            }
        })
        .state('rpc', {
            abstract: true,
            parent: 'user',
            data: {
                pageTitle: 'RPC'
            }
        })
        .state('rpc.application-list', {
            url: '/rpc-application-list?page&sort',
            views: {
                'content@': {
                    templateUrl: 'app/views/user/rpc-application/rpc-application-list.html',
                    controller: 'RpcApplicationListController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'RPC applications'
            },
            params: {
                page: {
                    value: '1',
                    squash: true
                },
                sort: {
                    value: 'team,asc',
                    squash: true
                }
            },
            resolve: {
                pagingParams: ['$stateParams', 'PaginationUtils', function ($stateParams, PaginationUtils) {
                    return {
                        page: PaginationUtils.parsePage($stateParams.page),
                        sort: $stateParams.sort,
                        predicate: PaginationUtils.parsePredicate($stateParams.sort),
                        ascending: PaginationUtils.parseAscending($stateParams.sort)
                    };
                }],
                criteria: ['$stateParams', function ($stateParams) {
                    return {};
                }]
            }
        })
        .state('rpc.server-list', {
            url: '/rpc-server-list?page&sort&address',
            views: {
                'content@': {
                    templateUrl: 'app/views/user/rpc-server/rpc-server-list.html',
                    controller: 'RpcServerListController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'RPC servers'
            },
            params: {
                page: {
                    value: '1',
                    squash: true
                },
                sort: {
                    value: 'address,asc',
                    squash: true
                }
            },
            resolve: {
                pagingParams: ['$stateParams', 'PaginationUtils', function ($stateParams, PaginationUtils) {
                    return {
                        page: PaginationUtils.parsePage($stateParams.page),
                        sort: $stateParams.sort,
                        predicate: PaginationUtils.parsePredicate($stateParams.sort),
                        ascending: PaginationUtils.parseAscending($stateParams.sort)
                    };
                }],
                criteria: ['$stateParams', function ($stateParams) {
                    return {
                        address: $stateParams.address
                    };
                }]
            }
        })
        .state('rpc.server-list.view', {
            url: '/view/:id',
            views: {
                'content@': {
                    templateUrl: 'app/views/user/rpc-server/rpc-server-details.html',
                    controller: 'RpcServerDetailsController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'View'
            },
            resolve: {
                entity: ['RpcServerService', '$stateParams', function (RpcServerService, $stateParams) {
                    return RpcServerService.get({extension: $stateParams.id}).$promise;
                }]
            }
        })
        .state('rpc.service-list', {
            url: '/rpc-service-list?page&sort&interfaceName',
            views: {
                'content@': {
                    templateUrl: 'app/views/user/rpc-service/rpc-service-list.html',
                    controller: 'RpcServiceListController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'RPC services'
            },
            params: {
                page: {
                    value: '1',
                    squash: true
                },
                sort: {
                    value: 'interfaceName,asc',
                    squash: true
                }
            },
            resolve: {
                pagingParams: ['$stateParams', 'PaginationUtils', function ($stateParams, PaginationUtils) {
                    return {
                        page: PaginationUtils.parsePage($stateParams.page),
                        sort: $stateParams.sort,
                        predicate: PaginationUtils.parsePredicate($stateParams.sort),
                        ascending: PaginationUtils.parseAscending($stateParams.sort)
                    };
                }],
                criteria: ['$stateParams', function ($stateParams) {
                    return {
                        interfaceName: $stateParams.interfaceName
                    };
                }]
            }
        })
        .state('rpc.provider-list', {
            url: '/rpc-provider-list?page&sort&application&interfaceName&address',
            views: {
                'content@': {
                    templateUrl: 'app/views/user/rpc-provider/rpc-provider-list.html',
                    controller: 'RpcProviderListController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'RPC providers'
            },
            params: {
                page: {
                    value: '1',
                    squash: true
                },
                sort: {
                    value: 'interfaceName,asc',
                    squash: true
                }
            },
            resolve: {
                loadPlugin: function ($ocLazyLoad) {
                    return $ocLazyLoad.load([
                        {
                            name: 'angular-peity',
                            files: ['content/js/plugins/peity/jquery.peity.min.js', 'content/js/plugins/peity/angular-peity.js']
                        }
                    ]);
                },
                pagingParams: ['$stateParams', 'PaginationUtils', function ($stateParams, PaginationUtils) {
                    return {
                        page: PaginationUtils.parsePage($stateParams.page),
                        sort: $stateParams.sort,
                        predicate: PaginationUtils.parsePredicate($stateParams.sort),
                        ascending: PaginationUtils.parseAscending($stateParams.sort)
                    };
                }],
                criteria: ['$stateParams', function ($stateParams) {
                    return {
                        application: $stateParams.application,
                        interfaceName: $stateParams.interfaceName,
                        address: $stateParams.address
                    };
                }]
            }
        })
        .state('rpc.provider-list.view', {
            url: '/view/:id?tab',
            views: {
                'content@': {
                    templateUrl: 'app/views/user/rpc-provider/rpc-provider-details.html',
                    controller: 'RpcProviderDetailsController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'RPC provider details'
            },
            resolve: {
                entity: ['RpcProviderService', '$stateParams', function (RpcProviderService, $stateParams) {
                    return RpcProviderService.get({extension: $stateParams.id}).$promise;
                }]
            }
        })
        .state('rpc.provider-list.view.create-task', {
            url: '/create-task',
            data: {
                pageTitle: 'Create scheduled task',
                mode: 'create'
            },
            onEnter: ['$state', '$uibModal', function ($state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/views/user/rpc-provider/rpc-scheduled-task-dialog.html',
                    controller: 'RpcScheduledTaskDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: {
                            id: null,
                            name: null,
                            argumentsJson: null,
                            cronExpression: null,
                            remark: null,
                            enabled: true
                        }
                    }
                }).result.then(function () {
                    $state.go('^', null, {reload: true});
                }, function () {
                    $state.go('^');
                });
            }]
        })
        .state('rpc.provider-list.view.edit-task', {
            url: '/edit-task/:taskId',
            data: {
                pageTitle: 'Edit scheduled task',
                mode: 'edit'
            },
            onEnter: ['$state', '$stateParams', '$uibModal', function ($state, $stateParams, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/views/user/rpc-provider/rpc-scheduled-task-dialog.html',
                    controller: 'RpcScheduledTaskDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['RpcScheduledTaskService', function (RpcScheduledTaskService) {
                            return RpcScheduledTaskService.get({extension: $stateParams.taskId}).$promise;
                        }]
                    }
                }).result.then(function () {
                    $state.go('^', null, {reload: true});
                }, function () {
                    $state.go('^');
                });
            }]
        })
        .state('rpc.scheduled-task-list', {
            url: '/rpc-scheduled-task-list?page&sort&name&interfaceName',
            views: {
                'content@': {
                    templateUrl: 'app/views/user/rpc-scheduled-task/rpc-scheduled-task-list.html',
                    controller: 'RpcScheduledTaskListController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'RPC scheduled task list'
            },
            params: {
                page: {
                    value: '1',
                    squash: true
                },
                sort: {
                    value: 'modifiedTime,desc',
                    squash: true
                }
            },
            resolve: {
                pagingParams: ['$stateParams', 'PaginationUtils', function ($stateParams, PaginationUtils) {
                    return {
                        page: PaginationUtils.parsePage($stateParams.page),
                        sort: $stateParams.sort,
                        predicate: PaginationUtils.parsePredicate($stateParams.sort),
                        ascending: PaginationUtils.parseAscending($stateParams.sort)
                    };
                }],
                criteria: ['$stateParams', function ($stateParams) {
                    return {
                        name: $stateParams.name,
                        interfaceName: $stateParams.interfaceName,
                        methodName: $stateParams.methodName
                    };
                }]
            }
        })
        .state('rpc.scheduled-task-list.edit', {
            url: '/edit/:id',
            data: {
                pageTitle: 'Edit',
                mode: 'edit'
            },
            onEnter: ['$state', '$stateParams', '$uibModal', function ($state, $stateParams, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/views/user/rpc-provider/rpc-scheduled-task-dialog.html',
                    controller: 'RpcScheduledTaskDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['RpcScheduledTaskService', function (RpcScheduledTaskService) {
                            return RpcScheduledTaskService.get({extension: $stateParams.id}).$promise;
                        }]
                    }
                }).result.then(function (result) {
                    $state.go('^', null, {reload: true});
                }, function () {
                    $state.go('^');
                });
            }]
        })
        .state('rpc.scheduled-task-history-list', {
            url: '/rpc-scheduled-task-history-list?page&sort&name&providerId',
            views: {
                'content@': {
                    templateUrl: 'app/views/user/rpc-scheduled-task-history/rpc-scheduled-task-history-list.html',
                    controller: 'RpcScheduledTaskHistoryListController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Scheduled task histories'
            },
            params: {
                page: {
                    value: '1',
                    squash: true
                },
                sort: {
                    value: 'createdTime,desc',
                    squash: true
                }
            },
            resolve: {
                pagingParams: ['$stateParams', 'PaginationUtils', function ($stateParams, PaginationUtils) {
                    return {
                        page: PaginationUtils.parsePage($stateParams.page),
                        sort: $stateParams.sort,
                        predicate: PaginationUtils.parsePredicate($stateParams.sort),
                        ascending: PaginationUtils.parseAscending($stateParams.sort)
                    };
                }],
                criteria: ['$stateParams', function ($stateParams) {
                    return {
                        name: $stateParams.name,
                        providerId: $stateParams.providerId
                    };
                }]
            }
        })
        .state('rpc.scheduled-task-history-list.view', {
            url: '/view/:id',
            views: {
                'content@': {
                    templateUrl: 'app/views/user/rpc-scheduled-task-history/rpc-scheduled-task-history-details.html',
                    controller: 'RpcScheduledTaskHistoryDetailsController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'View'
            },
            resolve: {
                entity: ['RpcScheduledTaskHistoryService', '$stateParams', function (RpcScheduledTaskHistoryService, $stateParams) {
                    return RpcScheduledTaskHistoryService.get({extension: $stateParams.id}).$promise;
                }]
            }
        })
        .state('rpc.consumer-list', {
            url: '/rpc-consumer-list?page&sort&application&interfaceName&address',
            views: {
                'content@': {
                    templateUrl: 'app/views/user/rpc-consumer/rpc-consumer-list.html',
                    controller: 'RpcConsumerListController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'RPC consumers'
            },
            params: {
                page: {
                    value: '1',
                    squash: true
                },
                sort: {
                    value: 'interfaceName,asc',
                    squash: true
                }
            },
            resolve: {
                loadPlugin: function ($ocLazyLoad) {
                    return $ocLazyLoad.load([
                        {
                            name: 'angular-peity',
                            files: ['content/js/plugins/peity/jquery.peity.min.js', 'content/js/plugins/peity/angular-peity.js']
                        }
                    ]);
                },
                pagingParams: ['$stateParams', 'PaginationUtils', function ($stateParams, PaginationUtils) {
                    return {
                        page: PaginationUtils.parsePage($stateParams.page),
                        sort: $stateParams.sort,
                        predicate: PaginationUtils.parsePredicate($stateParams.sort),
                        ascending: PaginationUtils.parseAscending($stateParams.sort)
                    };
                }],
                criteria: ['$stateParams', function ($stateParams) {
                    return {
                        application: $stateParams.application,
                        interfaceName: $stateParams.interfaceName,
                        address: $stateParams.address
                    };
                }]
            }
        })
        .state('developer', {
            abstract: true,
            parent: 'layout',
            data: {
                authorities: ['ROLE_DEVELOPER']
            }
        })
        .state('api', {
            parent: 'developer',
            url: '/api',
            views: {
                'content@': {
                    templateUrl: 'app/views/developer/api/api.html'
                }
            },
            data: {
                pageTitle: 'API'
            }
        })
        .state('api-docs', {
            parent: 'developer',
            url: '/api-docs',
            views: {
                'content@': {
                    templateUrl: 'app/views/developer/api-docs/api-docs.html'
                }
            },
            data: {
                pageTitle: 'API Docs'
            }
        })
        .state('metrics', {
            parent: 'developer',
            url: '/metrics',
            views: {
                'content@': {
                    templateUrl: 'app/views/developer/metrics/metrics.html',
                    controller: 'MetricsController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Metrics'
            },
            resolve: {
                metrics: ['MetricsService', function (MetricsService) {
                    return MetricsService.getMetrics();
                }]
            }
        })
        .state('health', {
            parent: 'developer',
            url: '/health',
            views: {
                'content@': {
                    templateUrl: 'app/views/developer/health/health.html',
                    controller: 'HealthController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Health'
            }
        })
        .state('configuration', {
            parent: 'developer',
            url: '/configuration',
            views: {
                'content@': {
                    templateUrl: 'app/views/developer/configuration/configuration.html',
                    controller: 'ConfigurationController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Configuration'
            }
        })
        .state('beans', {
            parent: 'developer',
            url: '/beans',
            views: {
                'content@': {
                    templateUrl: 'app/views/developer/beans/beans.html',
                    controller: 'BeansController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Beans'
            }
        })
        .state('mappings', {
            parent: 'developer',
            url: '/mappings',
            views: {
                'content@': {
                    templateUrl: 'app/views/developer/mappings/mappings.html',
                    controller: 'MappingsController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Mappings'
            }
        })
        .state('http-trace', {
            parent: 'developer',
            url: '/http-trace',
            views: {
                'content@': {
                    templateUrl: 'app/views/developer/trace/http-trace.html',
                    controller: 'HttpTraceController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Http Trace'
            }
        })
        .state('audits', {
            parent: 'developer',
            url: '/audits',
            views: {
                'content@': {
                    templateUrl: 'app/views/developer/audits/audits.html',
                    controller: 'AuditsController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Audits'
            }
        })
        .state('tracker', {
            parent: 'developer',
            url: '/tracker',
            views: {
                'content@': {
                    templateUrl: 'app/views/developer/tracker/tracker.html',
                    controller: 'TrackerController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Tracker'
            },
            onEnter: ['TrackerService', function (TrackerService) {
                TrackerService.subscribe();
            }],
            onExit: ['TrackerService', function (TrackerService) {
                TrackerService.unsubscribe();
            }]
        })
        .state('logger', {
            parent: 'developer',
            url: '/logger',
            views: {
                'content@': {
                    templateUrl: 'app/views/developer/logger/logger.html',
                    controller: 'LoggerController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Loggers'
            }
        })
        .state('arthas', {
            parent: 'developer',
            url: '/arthas',
            views: {
                'content@': {
                    templateUrl: 'app/views/developer/arthas/arthas.html',
                    controller: 'ArthasController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Arthas console'
            }
        })
        .state('schedule', {
            parent: 'developer',
            url: '/schedule',
            views: {
                'content@': {
                    templateUrl: 'app/views/developer/schedule/schedule.html',
                    controller: 'ScheduleController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Schedule'
            }
        })
        .state('control', {
            parent: 'developer',
            url: '/control',
            views: {
                'content@': {
                    templateUrl: 'app/views/developer/control/control.html',
                    controller: 'ControlController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Control'
            }
        })
        .state('admin', {
            abstract: true,
            parent: 'layout',
            data: {
                authorities: ['ROLE_ADMIN']
            }
        })
        .state('user-authority', {
            abstract: true,
            parent: 'admin',
            data: {
                pageTitle: '用户权限'
            }
        })
        .state('user-authority.authority-list', {
            url: '/authority-list?page&sort',
            views: {
                'content@': {
                    templateUrl: 'app/views/admin/authority/authority-list.html',
                    controller: 'AuthorityListController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'Authority list'
            },
            params: {
                page: {
                    value: '1',
                    squash: true
                },
                sort: {
                    value: 'name,asc',
                    squash: true
                }
            },
            resolve: {
                pagingParams: ['$stateParams', 'PaginationUtils', function ($stateParams, PaginationUtils) {
                    return {
                        page: PaginationUtils.parsePage($stateParams.page),
                        sort: $stateParams.sort,
                        predicate: PaginationUtils.parsePredicate($stateParams.sort),
                        ascending: PaginationUtils.parseAscending($stateParams.sort)
                    };
                }],
                criteria: ['$stateParams', function ($stateParams) {
                    return {};
                }]
            }
        })
        .state('user-authority.authority-list.create', {
            url: '/create',
            data: {
                pageTitle: 'Create authority',
                mode: 'create'
            },
            onEnter: ['$state', '$uibModal', function ($state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/views/admin/authority/authority-dialog.html',
                    controller: 'AuthorityDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: {
                            name: null,
                            systemLevel: false,
                            enabled: true
                        }
                    }
                }).result.then(function () {
                    $state.go('^', null, {reload: true});
                }, function () {
                    $state.go('^');
                });
            }]
        })
        .state('user-authority.user-list', {
            url: '/user-list?page&sort&login',
            views: {
                'content@': {
                    templateUrl: 'app/views/admin/user/user-list.html',
                    controller: 'UserListController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'User list'
            },
            params: {
                page: {
                    value: '1',
                    squash: true
                },
                sort: {
                    value: 'modifiedTime,desc',
                    squash: true
                }
            },
            resolve: {
                pagingParams: ['$stateParams', 'PaginationUtils', function ($stateParams, PaginationUtils) {
                    return {
                        page: PaginationUtils.parsePage($stateParams.page),
                        sort: $stateParams.sort,
                        predicate: PaginationUtils.parsePredicate($stateParams.sort),
                        ascending: PaginationUtils.parseAscending($stateParams.sort)
                    };
                }],
                criteria: ['$stateParams', function ($stateParams) {
                    return {
                        login: $stateParams.login
                    };
                }]
            }
        })
        .state('user-authority.user-list.create', {
            url: '/create',
            data: {
                pageTitle: 'Create user',
                mode: 'create'
            },
            onEnter: ['$state', '$uibModal', function ($state, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/views/admin/user/user-dialog.html',
                    controller: 'UserDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: {
                            userId: null,
                            userName: null,
                            firstName: null,
                            lastName: null,
                            email: null,
                            enabled: true,
                            activated: true,
                            createdBy: null,
                            createdTime: null,
                            modifiedBy: null,
                            modifiedTime: null,
                            resetTime: null,
                            resetKey: null
                        }
                    }
                }).result.then(function () {
                    $state.go('^', null, {reload: true});
                }, function () {
                    $state.go('^');
                });
            }]
        })
        .state('user-authority.user-list.edit', {
            url: '/edit/:userName',
            data: {
                pageTitle: 'Edit user',
                mode: 'edit'
            },
            onEnter: ['$state', '$stateParams', '$uibModal', function ($state, $stateParams, $uibModal) {
                $uibModal.open({
                    templateUrl: 'app/views/admin/user/user-dialog.html',
                    controller: 'UserDialogController',
                    controllerAs: 'vm',
                    backdrop: 'static',
                    size: 'lg',
                    resolve: {
                        entity: ['UserService', function (UserService) {
                            return UserService.get({userName: $stateParams.userName}).$promise;
                        }]
                    }
                }).result.then(function (result) {
                    $state.go('^', null, {reload: true});
                }, function () {
                    $state.go('^');
                });
            }]
        })
        .state('user-authority.user-list.view', {
            url: '/view/:userName',
            views: {
                'content@': {
                    templateUrl: 'app/views/admin/user/user-details.html',
                    controller: 'UserDetailsController',
                    controllerAs: 'vm'
                }
            },
            data: {
                pageTitle: 'View user'
            },
            resolve: {
                entity: ['UserService', '$stateParams', function (UserService, $stateParams) {
                    return UserService.get({userName: $stateParams.userName}).$promise;
                }]
            }
        })
}
function paginationConfig(uibPaginationConfig, PAGINATION_CONSTANTS) {
    uibPaginationConfig.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    uibPaginationConfig.maxSize = 5;
    uibPaginationConfig.boundaryLinks = true;
    uibPaginationConfig.firstText = '«';
    uibPaginationConfig.previousText = '‹';
    uibPaginationConfig.nextText = '›';
    uibPaginationConfig.lastText = '»';
}
function pagerConfig(uibPagerConfig, PAGINATION_CONSTANTS) {
    uibPagerConfig.itemsPerPage = PAGINATION_CONSTANTS.itemsPerPage;
    uibPagerConfig.previousText = '«';
    uibPagerConfig.nextText = '»';
}
function httpConfig($urlRouterProvider, $httpProvider, httpRequestInterceptorCacheBusterProvider, $urlMatcherFactoryProvider) {
    //Cache everything except rest api requests
    httpRequestInterceptorCacheBusterProvider.setMatchlist([/.*api.*/, /.*protected.*/], true);

    $httpProvider.interceptors.push('alertErrorHandlerInterceptor');
    $httpProvider.interceptors.push('authExpiredInterceptor');
    $httpProvider.interceptors.push('authInterceptor');
    $httpProvider.interceptors.push('alertHandlerInterceptor');

    $urlMatcherFactoryProvider.type('boolean', {
        name: 'boolean',
        decode: function (val) {
            return val === true || val === 'true';
        },
        encode: function (val) {
            return val ? 1 : 0;
        },
        equals: function (a, b) {
            return this.is(a) && a === b;
        },
        is: function (val) {
            return [true, false, 0, 1].indexOf(val) >= 0;
        },
        pattern: /bool|true|0|1/
    });
}
function localStorageConfig($localStorageProvider, $sessionStorageProvider) {
    $localStorageProvider.setKeyPrefix('app-');
    $sessionStorageProvider.setKeyPrefix('app-');
}
function compileServiceConfig($compileProvider, DEBUG_INFO_ENABLED) {
    // disable debug data on prod profile to improve performance
    $compileProvider.debugInfoEnabled(DEBUG_INFO_ENABLED);

    /*
    If you wish to debug an application with this information
    then you should open up a debug console in the browser
    then call this method directly in this console:

    angular.reloadWithDebugInfo();
    */
}