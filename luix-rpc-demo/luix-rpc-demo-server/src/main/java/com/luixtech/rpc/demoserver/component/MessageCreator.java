package com.luixtech.rpc.demoserver.component;

import com.luixtech.rpc.demoserver.config.ApplicationConstants;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class MessageCreator {
    private MessageSource messageSource;

    public String getMessage(String code, Object... arguments) {
        return messageSource.getMessage(code, arguments, ApplicationConstants.SYSTEM_LOCALE);
    }
}
