package com.aicopilot.api.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.base-url}")
    private String baseUrl;

    /**
     * 发送团队邀请邮件
     *
     * @param toEmail    被邀请人邮箱
     * @param teamName   团队名称
     * @param inviteCode 邀请码
     */
    @Async
    public void sendTeamInviteEmail(String toEmail, String teamName, String inviteCode) {
        String joinUrl = baseUrl + "/teams/join?inviteCode=" + inviteCode;
        String registerUrl = baseUrl + "/register?inviteCode=" + inviteCode;

        String subject = "您收到一封来自团队「" + teamName + "」的邀请";
        String htmlContent = buildInviteEmailHtml(teamName, inviteCode, joinUrl, registerUrl);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("团队邀请邮件已发送至: {}", toEmail);
        } catch (MessagingException e) {
            log.error("发送邀请邮件失败, toEmail={}, teamName={}", toEmail, teamName, e);
            throw new RuntimeException("邮件发送失败", e);
        }
    }

    private String buildInviteEmailHtml(String teamName, String inviteCode,
            String joinUrl, String registerUrl) {
        return """
                <div style="max-width:600px;margin:0 auto;font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;color:#333;">
                    <div style="background:linear-gradient(135deg,#6366f1,#8b5cf6);padding:32px;border-radius:12px 12px 0 0;text-align:center;">
                        <h1 style="color:#fff;margin:0;font-size:24px;">AI Copilot 团队邀请</h1>
                    </div>
                    <div style="background:#fff;padding:32px;border:1px solid #e5e7eb;border-top:none;border-radius:0 0 12px 12px;">
                        <p style="font-size:16px;margin-bottom:24px;">您好！</p>
                        <p style="font-size:15px;line-height:1.6;">
                            您被邀请加入团队 <strong style="color:#6366f1;">%s</strong>，请使用以下邀请码加入：
                        </p>
                        <div style="background:#f3f4f6;border-radius:8px;padding:16px;text-align:center;margin:24px 0;">
                            <span style="font-family:monospace;font-size:24px;letter-spacing:4px;font-weight:700;color:#6366f1;">%s</span>
                        </div>
                        <p style="font-size:14px;color:#6b7280;margin-bottom:24px;">如果您已有账号，点击下方按钮直接加入团队：</p>
                        <div style="text-align:center;margin-bottom:16px;">
                            <a href="%s" style="display:inline-block;background:#6366f1;color:#fff;padding:12px 32px;border-radius:8px;text-decoration:none;font-size:15px;font-weight:500;">
                                加入团队
                            </a>
                        </div>
                        <p style="font-size:14px;color:#6b7280;margin-bottom:16px;">如果您还没有账号，点击下方按钮注册并自动填写邀请码：</p>
                        <div style="text-align:center;margin-bottom:24px;">
                            <a href="%s" style="display:inline-block;background:#fff;color:#6366f1;padding:12px 32px;border-radius:8px;text-decoration:none;font-size:15px;font-weight:500;border:1px solid #6366f1;">
                                注册并加入
                            </a>
                        </div>
                        <hr style="border:none;border-top:1px solid #e5e7eb;margin:24px 0;" />
                        <p style="font-size:12px;color:#9ca3af;text-align:center;">
                            此邮件由 AI Copilot 系统自动发送，请勿回复。
                        </p>
                    </div>
                </div>
                """
                .formatted(teamName, inviteCode, joinUrl, registerUrl);
    }
}