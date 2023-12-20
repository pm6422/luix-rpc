package com.luixtech.rpc.webcenter.listener;

import com.luixtech.rpc.webcenter.event.LogoutEvent;
import com.luixtech.rpc.webcenter.security.AjaxLogoutSuccessHandler;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
@AllArgsConstructor
@Slf4j
public class AuthEventListener {
    private final AjaxLogoutSuccessHandler ajaxLogoutSuccessHandler;

    @Async
    @EventListener
    public void logoutEvent(LogoutEvent event) {
        log.debug("Processing logout event initiated by {}", event.getSource().getClass().getSimpleName());
        ServletRequestAttributes servletRequestAttributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = servletRequestAttributes != null ? servletRequestAttributes.getRequest() : null;
        HttpServletResponse response = servletRequestAttributes != null ? servletRequestAttributes.getResponse() : null;
        if (request != null && response != null) {
            ajaxLogoutSuccessHandler.onLogoutSuccess(request, response, null);
        }
        log.debug("Processed logout event");
    }
}
