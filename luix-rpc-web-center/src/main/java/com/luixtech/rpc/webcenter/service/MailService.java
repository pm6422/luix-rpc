package com.luixtech.rpc.webcenter.service;

import com.luixtech.rpc.webcenter.domain.User;

public interface MailService {

    void sendEmail(String[] sendTo, String subject, String content, boolean isMultipart, boolean isHtml);

    void sendEmailFromTemplate(User user, String templateName, String titleKey, String baseUrl);

    void sendActivationEmail(User user, String baseUrl);

    void sendCreationEmail(User user, String baseUrl);

    void sendPasswordResetMail(User user, String baseUrl);

}
