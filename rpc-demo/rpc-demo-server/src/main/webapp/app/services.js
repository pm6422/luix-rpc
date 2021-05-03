/**
 * Services
 */
angular
    .module('smartcloudserviceApp')
    .factory('StateHandler', StateHandler)
    .factory('Base64Utils', Base64Utils)
    .factory('ParseLinksUtils', ParseLinksUtils)
    .factory('PaginationUtils', PaginationUtils)
    .factory('AlertUtils', AlertUtils)
    .factory('DateUtils', DateUtils)
    .factory('DataUtils', DataUtils)
    .factory('ProfileService', ProfileService)
    .factory('MetricsService', MetricsService)
    .factory('HealthService', HealthService)
    .factory('ConfigurationService', ConfigurationService)
    .factory('LoggerService', LoggerService)
    .factory('TimingTaskService', TimingTaskService)
    .factory('TimingTaskHistoryService', TimingTaskHistoryService)
    .factory('AdminMenuService', AdminMenuService)
    .factory('AuthorityService', AuthorityService)
    .factory('AppService', AppService);

/**
 * StateHandler
 */
function StateHandler($rootScope, $state, $sessionStorage, $window, AlertUtils, APP_NAME, VERSION) {
    return {
        initialize: initialize
    };

    function initialize() {
        $rootScope.APP_NAME = APP_NAME;
        $rootScope.VERSION = VERSION;

        var stateChangeStart = $rootScope.$on('$stateChangeStart', function (event, toState, toStateParams, fromState) {
            $rootScope.toState = toState;
            $rootScope.toStateParams = toStateParams;
            $rootScope.fromState = fromState;

            // Redirect to a state with an external URL (http://stackoverflow.com/a/30221248/1098564)
            if (toState.external) {
                event.preventDefault();
                $window.open(toState.url, '_self');
            }
        });

        var stateChangeSuccess = $rootScope.$on('$stateChangeSuccess', function (event, toState, toParams, fromState, fromParams) {
            var titleKey = APP_NAME;

            // Set the page title key to the one configured in state or use default one
            if (toState.data.pageTitle) {
                titleKey = toState.data.pageTitle;
            }
            $window.document.title = titleKey;
        });

        var cleanHttpErrorListener = $rootScope.$on('smartcloudserviceApp.httpError', function (event, httpResponse) {
            var i;
            event.stopPropagation();
            switch (httpResponse.status) {
                // connection refused, server not reachable
                case 0:
                    AlertUtils.error('Server not reachable');
                    break;
                case 400:
                    if (httpResponse.data && httpResponse.data.errorFields) {
                        for (i = 0; i < httpResponse.data.errorFields.length; i++) {
                            var fieldError = httpResponse.data.errorFields[i];
                            AlertUtils.error(fieldError.message);
                        }
                    } else if (httpResponse.data && httpResponse.data.message) {
                        AlertUtils.error(httpResponse.data.message);
                    } else {
                        AlertUtils.error(httpResponse.data);
                    }
                    break;
                case 404:
                    AlertUtils.error('Not found');
                    break;
                default:
                    if (httpResponse.data && httpResponse.data.message) {
                        AlertUtils.error(httpResponse.data.message);
                    } else {
//	                    $state.go('error', {errorMessage: angular.toJson(httpResponse) });
                        $state.go('error', {errorMessage: httpResponse});
                    }
            }
        });

        $rootScope.$on('$destroy', function () {
            if (angular.isDefined(stateChangeStart) && stateChangeStart !== null) {
                stateChangeStart();
            }
            if (angular.isDefined(stateChangeSuccess) && stateChangeSuccess !== null) {
                stateChangeSuccess();
            }
            if (angular.isDefined(cleanHttpErrorListener) && cleanHttpErrorListener !== null) {
                cleanHttpErrorListener();
            }
        });
    }
}

/**
 * Base64Utils
 */
function Base64Utils() {
    var keyStr = 'ABCDEFGHIJKLMNOP' +
        'QRSTUVWXYZabcdef' +
        'ghijklmnopqrstuv' +
        'wxyz0123456789+/' +
        '=';

    return {
        decode: decode,
        encode: encode
    };

    function encode(input) {
        var output = '',
            chr1, chr2, chr3 = '',
            enc1, enc2, enc3, enc4 = '',
            i = 0;

        while (i < input.length) {
            chr1 = input.charCodeAt(i++);
            chr2 = input.charCodeAt(i++);
            chr3 = input.charCodeAt(i++);

            enc1 = chr1 >> 2;
            enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
            enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
            enc4 = chr3 & 63;

            if (isNaN(chr2)) {
                enc3 = enc4 = 64;
            } else if (isNaN(chr3)) {
                enc4 = 64;
            }

            output = output +
                keyStr.charAt(enc1) +
                keyStr.charAt(enc2) +
                keyStr.charAt(enc3) +
                keyStr.charAt(enc4);
            chr1 = chr2 = chr3 = '';
            enc1 = enc2 = enc3 = enc4 = '';
        }

        return output;
    }

    function decode(input) {
        var output = '',
            chr1, chr2, chr3 = '',
            enc1, enc2, enc3, enc4 = '',
            i = 0;

        // remove all characters that are not A-Z, a-z, 0-9, +, /, or =
        input = input.replace(/[^A-Za-z0-9\+\/\=]/g, '');

        while (i < input.length) {
            enc1 = keyStr.indexOf(input.charAt(i++));
            enc2 = keyStr.indexOf(input.charAt(i++));
            enc3 = keyStr.indexOf(input.charAt(i++));
            enc4 = keyStr.indexOf(input.charAt(i++));

            chr1 = (enc1 << 2) | (enc2 >> 4);
            chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
            chr3 = ((enc3 & 3) << 6) | enc4;

            output = output + String.fromCharCode(chr1);

            if (enc3 !== 64) {
                output = output + String.fromCharCode(chr2);
            }
            if (enc4 !== 64) {
                output = output + String.fromCharCode(chr3);
            }

            chr1 = chr2 = chr3 = '';
            enc1 = enc2 = enc3 = enc4 = '';
        }

        return output;
    }
}

/**
 * ParseLinksUtils
 */
function ParseLinksUtils() {
    return {
        parse: parse
    };

    function parse(header) {
        if (header.length === 0) {
            throw new Error('input must not be of zero length');
        }

        // Split parts by comma
        var parts = header.split(',');
        var links = {};
        // Parse each part into a named link
        angular.forEach(parts, function (p) {
            var section = p.split(';');
            if (section.length !== 2) {
                throw new Error('section could not be split on ";"');
            }
            var url = section[0].replace(/<(.*)>/, '$1').trim();
            var queryString = {};
            url.replace(
                new RegExp('([^?=&]+)(=([^&]*))?', 'g'),
                function ($0, $1, $2, $3) {
                    queryString[$1] = $3;
                }
            );
            var page = queryString.page;
            if (angular.isString(page)) {
                page = parseInt(page);
            }
            var name = section[1].replace(/rel="(.*)"/, '$1').trim();
            links[name] = page;
        });

        return links;
    }
}

/**
 * PaginationUtils
 */
function PaginationUtils() {
    return {
        parseAscending: parseAscending,
        parsePage: parsePage,
        parsePredicate: parsePredicate
    };

    function parseAscending(sort) {
        var sortArray = sort.split(',');
        if (sortArray.length > 1) {
            return sort.split(',').slice(-1)[0] === 'asc';
        } else {
            // default to true if no sort defined
            return true;
        }
    }

    // query params are strings, and need to be parsed
    function parsePage(page) {
        return parseInt(page);
    }

    // sort can be in the format `id,asc` or `id`
    function parsePredicate(sort) {
        var sortArray = sort.split(',');
        if (sortArray.length > 1) {
            sortArray.pop();
        }
        return sortArray.join(',');
    }
}

/**
 * AlertUtils
 */
function AlertUtils(SweetAlert, toaster, APP_NAME) {
    return {
        success: success,
        error: error,
        warning: warning,
        createDeleteConfirmation: createDeleteConfirmation,
        createResetPasswordConfirmation: createResetPasswordConfirmation
    };

    function success(msg, params, position) {
        toaster.success(APP_NAME, msg);
    }

    function error(msg, params, position) {
        toaster.error(APP_NAME, msg);
    }

    function warning(msg, params, position) {
        toaster.warning(APP_NAME, msg);
    }

    function createDeleteConfirmation(alerText, confirmDelete) {
        SweetAlert.swal({
            title: '确定删除?',
            text: alerText ? alerText : '',
            type: 'warning',
            animation: 'slide-from-top',
            showCancelButton: true,
            confirmButtonColor: '#ec4758',
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            closeOnConfirm: true,
            closeOnCancel: true
        }, confirmDelete);
    }

    function createResetPasswordConfirmation(alerText, confirmReset) {
        SweetAlert.swal({
            title: '确定重置密码?',
            text: alerText ? alerText : '',
            type: 'warning',
            animation: 'slide-from-top',
            showCancelButton: true,
            confirmButtonColor: '#ec4758',
            confirmButtonText: '确定',
            cancelButtonText: '取消',
            closeOnConfirm: true,
            closeOnCancel: true
        }, confirmReset);
    }
}

/**
 * DateUtils
 */
function DateUtils($filter) {
    return {
        convertDateTimeFromServer: convertDateTimeFromServer,
        convertLocalDateFromServer: convertLocalDateFromServer,
        convertLocalDateToServer: convertLocalDateToServer,
        dateformat: dateformat
    };

    function convertDateTimeFromServer(date) {
        if (date) {
            return new Date(date);
        } else {
            return null;
        }
    }

    function convertLocalDateFromServer(date) {
        if (date) {
            var dateString = date.split('-');
            return new Date(dateString[0], dateString[1] - 1, dateString[2]);
        }
        return null;
    }

    function convertLocalDateToServer(date) {
        if (date) {
            return $filter('date')(date, 'yyyy-MM-dd');
        } else {
            return null;
        }
    }

    function dateformat() {
        return 'yyyy-MM-dd';
    }
}

/**
 * DataUtils
 */
function DataUtils($window) {
    return {
        abbreviate: abbreviate,
        byteSize: byteSize,
        openFile: openFile,
        toBase64: toBase64
    };

    function abbreviate(text) {
        if (!angular.isString(text)) {
            return '';
        }
        if (text.length < 30) {
            return text;
        }
        return text ? (text.substring(0, 15) + '...' + text.slice(-10)) : '';
    }

    function byteSize(base64String) {
        if (!angular.isString(base64String)) {
            return '';
        }

        function endsWith(suffix, str) {
            return str.indexOf(suffix, str.length - suffix.length) !== -1;
        }

        function paddingSize(base64String) {
            if (endsWith('==', base64String)) {
                return 2;
            }
            if (endsWith('=', base64String)) {
                return 1;
            }
            return 0;
        }

        function size(base64String) {
            return base64String.length / 4 * 3 - paddingSize(base64String);
        }

        function formatAsBytes(size) {
            return size.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ') + ' bytes';
        }

        return formatAsBytes(size(base64String));
    }

    function openFile(type, data) {
        $window.open('data:' + type + ';base64,' + data, '_blank', 'height=300,width=400');
    }

    function toBase64(file, cb) {
        var fileReader = new FileReader();
        fileReader.readAsDataURL(file);
        fileReader.onload = function (e) {
            var base64Data = e.target.result.substr(e.target.result.indexOf('base64,') + 'base64,'.length);
            cb(base64Data);
        };
    }
}

/**
 * ProfileService
 */
function ProfileService($q, $http, $localStorage) {
    var dataPromise;

    return {
        getProfileInfo: getProfileInfo
    };

    function getProfileInfo() {
        if (angular.isUndefined(dataPromise)) {
            dataPromise = $http.get('open-api/system/profile-info').then(function (result) {
                if (result.data.activeProfiles) {
                    return result.data;
                }
            });
        }
        return dataPromise;
    }
}

/**
 * MetricsService
 */
function MetricsService($rootScope, $http) {
    return {
        getMetrics: getMetrics,
        threadDump: threadDump
    };

    function getMetrics() {
        return $http.get('api/metric/metrics').then(function (response) {
            return response.data;
        });
    }

    function threadDump() {
        return $http.get('management/threaddump').then(function (response) {
            return response.data;
        });
    }
}

/**
 * HealthService
 */
function HealthService($rootScope, $http) {
    var separator = '.';
    return {
        checkHealth: checkHealth,
        transformHealthData: transformHealthData,
        getBaseName: getBaseName,
        getSubSystemName: getSubSystemName
    };

    function checkHealth() {
        return $http.get('management/health').then(function (response) {
            return response.data;
        });
    }

    function transformHealthData(data) {
        var response = [];
        flattenHealthData(response, null, data.components);
        return response;
    }

    function getBaseName(name) {
        if (name) {
            var split = name.split('.');
            return split[0];
        }
    }

    function getSubSystemName(name) {
        if (name) {
            var split = name.split('.');
            split.splice(0, 1);
            var remainder = split.join('.');
            return remainder ? ' - ' + remainder : '';
        }
    }

    /* private methods */
    function flattenHealthData(result, path, data) {
        angular.forEach(data, function (value, key) {
            if (isHealthObject(value)) {
                if (hasSubSystem(value)) {
                    addHealthObject(result, false, value, getModuleName(path, key));
                    flattenHealthData(result, getModuleName(path, key), value);
                } else {
                    addHealthObject(result, true, value, getModuleName(path, key));
                }
            }
        });
        return result;
    }

    function addHealthObject(result, isLeaf, healthObject, name) {

        var healthData = {
            'name': name
        };
        var details = {};
        var hasDetails = false;

        angular.forEach(healthObject, function (value, key) {
            if (key === 'status' || key === 'error') {
                healthData[key] = value;
            } else {
                if (!isHealthObject(value)) {
                    details[key] = value;
                    hasDetails = true;
                }
            }
        });

        // Add the of the details
        if (hasDetails) {
            angular.extend(healthData, {'details': details});
        }

        // Only add nodes if they provide additional information
        if (isLeaf || hasDetails || healthData.error) {
            result.push(healthData);
        }
        return healthData;
    }

    function getModuleName(path, name) {
        var result;
        if (path && name) {
            result = path + separator + name;
        } else if (path) {
            result = path;
        } else if (name) {
            result = name;
        } else {
            result = '';
        }
        return result;
    }

    function hasSubSystem(healthObject) {
        var result = false;
        angular.forEach(healthObject, function (value) {
            if (value && value.status) {
                result = true;
            }
        });
        return result;
    }

    function isHealthObject(healthObject) {
        var result = false;
        angular.forEach(healthObject, function (value, key) {
            if (key === 'status') {
                result = true;
            }
        });
        return result;
    }
}

function ConfigurationService($filter, $http, APP_NAME) {
    var service = {
        get: get,
        getEnv: getEnv
    };

    return service;

    function get() {
        return $http.get('management/configprops').then(getConfigPropsComplete);

        function getConfigPropsComplete(response) {
            var properties = [];
            var propertiesObject = getConfigPropertiesObjects(response.data);
            for (var key in propertiesObject) {
                if (propertiesObject.hasOwnProperty(key)) {
                    properties.push(propertiesObject[key]);
                }
            }
            var orderBy = $filter('orderBy');
            return orderBy(properties, 'prefix');
        }
    }

    function getConfigPropertiesObjects(res) {
        // This code is for Spring Boot 2
        if (res['contexts'] !== undefined) {
            for (var key in res['contexts']) {
                // If the key is not bootstrap, it will be the ApplicationContext Id
                // For default app, it is baseName
                // For microservice, it is baseName-1
                if (!key.startsWith('bootstrap')) {
                    return res['contexts'][key]['beans'];
                }
            }
        }
        // by default, use the default ApplicationContext Id
        return res['contexts'][APP_NAME]['beans'];
    }

    function getEnv() {
        return $http.get('management/env').then(getEnvComplete);

        function getEnvComplete(response) {
            var properties = {};
            angular.forEach(response.data['propertySources'], function (val, key) {
                var vals = [];
                angular.forEach(val['properties'], function (v, k) {
                    vals.push({key: k, val: v});
                });
                properties[val['name']] = vals;
            });
            return properties;
        }
    }
}

function LoggerService($resource) {
    var service = $resource('management/loggers/:name', {}, {
        'query': {method: 'GET'},
        'changeLevel': {method: 'POST'}
    });

    return service;
}

/**
 * TimingTaskService
 */
function TimingTaskService($resource) {
    var service = $resource('api/tasks/:id', {}, {
        'query': {method: 'GET', isArray: true},
        'get': {
            method: 'GET',
            transformResponse: function (data) {
                data = angular.fromJson(data);
                return data;
            }
        },
        'create': {method: 'POST'},
        'update': {method: 'PUT'},
        'del': {method: 'DELETE'},
        'queryBeans': {method: 'GET', isArray: true}
    });
    return service;
}

/**
 * TimingTaskHistoryService
 */
function TimingTaskHistoryService($resource) {
    var service = $resource('api/task-histories', {}, {
        'query': {method: 'GET', isArray: true},
        'get': {
            method: 'GET',
            transformResponse: function (data) {
                data = angular.fromJson(data);
                return data;
            }
        }
    });
    return service;
}

/**
 * AdminMenuService
 */
function AdminMenuService($resource) {
    return $resource('api/admin-menus/:extension/:id', {}, {
        'query': {method: 'GET', isArray: true}
    });
}

/**
 * AuthorityService
 */
function AuthorityService($resource) {
    var service = $resource('api/authorities/:name', {}, {
        'query': {method: 'GET', isArray: true},
        'get': {
            method: 'GET',
            transformResponse: function (data) {
                data = angular.fromJson(data);
                return data;
            }
        },
        'save': {method: 'POST'},
        'update': {method: 'PUT'},
        'del': {method: 'DELETE'}
    });
    return service;
}

/**
 * AppService
 */
function AppService($resource) {
    var service = $resource('api/apps/:name', {}, {
        'query': {method: 'GET', isArray: true},
        'get': {
            method: 'GET',
            transformResponse: function (data) {
                data = angular.fromJson(data);
                return data;
            }
        },
        'save': {method: 'POST'},
        'update': {method: 'PUT'},
        'del': {method: 'DELETE'}
    });
    return service;
}
