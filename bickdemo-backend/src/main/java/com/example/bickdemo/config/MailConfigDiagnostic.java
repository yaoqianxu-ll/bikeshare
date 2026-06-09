package com.example.bickdemo.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import jakarta.mail.Transport;
import jakarta.mail.Session;
import java.util.Properties;

/**
 * 启动时诊断邮件配置，帮助排查 SMTP 认证问题。
 * 打印主副邮箱的关键配置（密码脱敏），并分别测试 SMTP 连接。
 * 排查完成后可删除此组件。
 */
@Slf4j
@Component
@Lazy(false)
public class MailConfigDiagnostic implements EnvironmentAware {

    @Value("${spring.mail.host:}")
    private String mailHost;

    @Value("${spring.mail.port:0}")
    private int mailPort;

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.mail.secondary.host:}")
    private String secHost;

    @Value("${app.mail.secondary.port:0}")
    private int secPort;

    @Value("${app.mail.secondary.username:}")
    private String secUsername;

    @Value("${app.mail.secondary.password:}")
    private String secPassword;

    @Autowired
    private JavaMailSender mailSender;

    @Autowired(required = false)
    @Qualifier("secondaryMailSender")
    private JavaMailSender secondaryMailSender;

    private Environment environment;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void diagnose() {
        log.info("==================== 邮件配置诊断开始 ====================");

        // ---- 主邮箱 (QQ) ----
        log.info("[主邮箱 QQ] host={}, port={}, username={}, password={}",
                mailHost, mailPort,
                mailUsername.isEmpty() ? "[空!]" : mailUsername,
                mask(mailPassword));

        // ---- 副邮箱 (163) ----
        log.info("[副邮箱 163] host={}, port={}, username={}, password={}",
                secHost.isEmpty() ? "[空]" : secHost, secPort,
                secUsername.isEmpty() ? "[空]" : secUsername,
                mask(secPassword));
        log.info("[副邮箱 Bean] secondaryMailSender = {}",
                secondaryMailSender != null ? "已创建" : "null (未配置)");

        // ---- 属性源追踪 ----
        if (environment instanceof ConfigurableEnvironment ce) {
            for (String key : new String[]{"MAIL_PASSWORD", "MAIL_USERNAME",
                    "MAIL_SECONDARY_PASSWORD", "MAIL_SECONDARY_USERNAME"}) {
                for (PropertySource<?> ps : ce.getPropertySources()) {
                    Object val = ps.getProperty(key);
                    if (val != null) {
                        log.info("  {} 来源 [{}] = {}", key, ps.getName(), mask(val.toString()));
                    }
                }
            }
        }

        // ---- Spring JavaMailSender 实际配置 ----
        if (mailSender instanceof JavaMailSenderImpl impl) {
            log.info("[Spring mailSender 实际值] host={}, port={}, username={}, password={}",
                    impl.getHost(), impl.getPort(),
                    impl.getUsername(), mask(impl.getPassword()));
        }

        // ---- SMTP 连接测试：主邮箱 ----
        testSmtp("主邮箱(QQ)", mailHost, mailPort, mailUsername, mailPassword, false);

        // ---- SMTP 连接测试：副邮箱 ----
        if (!secHost.isEmpty() && !secUsername.isEmpty() && !secPassword.isEmpty()) {
            testSmtp("副邮箱(163)", secHost, secPort, secUsername, secPassword, secPort == 465);
        }

        log.info("==================== 邮件配置诊断结束 ====================");
    }

    private void testSmtp(String label, String host, int port, String user, String pass, boolean ssl) {
        if (user.isEmpty() || pass.isEmpty()) {
            log.warn("[{}] SMTP 测试跳过: username 或 password 为空", label);
            return;
        }
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.port", String.valueOf(port));
            if (ssl) {
                props.put("mail.smtp.ssl.enable", "true");
                props.put("mail.smtp.socketFactory.port", String.valueOf(port));
            } else {
                props.put("mail.smtp.starttls.enable", "true");
            }
            Session session = Session.getInstance(props);
            Transport transport = session.getTransport(ssl ? "smtps" : "smtp");
            transport.connect(host, port, user, pass);
            transport.close();
            log.info("[{}] SMTP 连接测试: 成功", label);
        } catch (Exception e) {
            log.error("[{}] SMTP 连接测试失败: {} - {}", label, e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private String mask(String s) {
        if (s == null || s.isEmpty()) return "[空!]";
        if (s.length() <= 4) return "****(" + s.length() + "字符)";
        return s.substring(0, 2) + "****" + s.substring(s.length() - 2) + "(" + s.length() + "字符)";
    }
}
