package org.infinity.rpc.core.url;

import org.apache.commons.lang3.StringUtils;
import org.infinity.rpc.core.constant.RpcConstants;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

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
}
