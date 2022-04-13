package com.luixtech.luixrpc.core.utils;

import org.apache.commons.lang3.StringUtils;
import com.luixtech.luixrpc.core.constant.RpcConstants;
import com.luixtech.luixrpc.core.url.Url;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public abstract class UrlUtils {
    public static String urlEncode(String value) {
        if (StringUtils.isEmpty(value)) {
            return "";
        }
        try {
            return URLEncoder.encode(value, RpcConstants.DEFAULT_CHARACTER);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static String urlDecode(String value) {
        if (StringUtils.isBlank(value)) {
            return "";
        }
        try {
            return URLDecoder.decode(value, RpcConstants.DEFAULT_CHARACTER);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    public static List<Url> parseDirectUrls(String directUrlStr) {
        return Arrays.stream(RpcConstants.COMMA_SPLIT_PATTERN.split(directUrlStr))
                .map(Url::valueOf)
                .collect(Collectors.toList());
    }
}
