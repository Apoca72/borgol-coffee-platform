package borgol.infrastructure.email;

import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * SMTP имэйл үйлчилгээ — Double-Checked Locking Singleton.
 *
 * ════════════════════════════════════════════════════════════
 * Загвар: Singleton (GoF) — DatabaseConnection-тэй ижил хэв маяг
 * ════════════════════════════════════════════════════════════
 * Зорилго: Програмын туршид ганц нэг Jakarta Mail Session байна.
 *
 * Зарчим (аюулгүй байдал):
 *  - Silent fail — имэйл илгээлт амжилтгүй болбол алдаа лог болгоно,
 *    caller-д exception шидэхгүй (имэйл нь туслах функц, үндсэн биш).
 *  - Орчны хувьсагчаас тохиргоо уншина — кодод нууц мэдээлэл байхгүй.
 *
 * Тохиргооны орчны хувьсагчид:
 *  SMTP_HOST     — SMTP сервер (жишээ: smtp.gmail.com)
 *  SMTP_PORT     — порт (587 STARTTLS, 465 SSL)
 *  SMTP_USER     — нэвтрэх нэр (имэйл хаяг)
 *  SMTP_PASSWORD — нууц үг / app password
 *  EMAIL_FROM    — илгээгчийн хаяг (жишээ: Borgol <no-reply@borgol.mn>)
 */
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    /** DCL Singleton-д volatile зайлшгүй шаардлагатай */
    private static volatile EmailService instance;

    /** Jakarta Mail холболтын тохиргоо */
    private final Session session;

    /** Имэйл илгээгчийн хаяг */
    private final String fromAddress;

    /** SMTP тохиргоо хийгдсэн эсэх — буруу env бол false */
    private final boolean configured;

    // ── HTML загвар тогтмолууд ───────────────────────────────────────────────
    // Гадны файл ашиглахгүй — inline HTML байна (хялбар байршуулалт)

    /** Тавтай морилох имэйлийн HTML загвар */
    private static final String WELCOME_TEMPLATE =
        "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
        "<style>" +
        "body{font-family:Arial,sans-serif;background:#f5f0eb;margin:0;padding:20px}" +
        ".card{max-width:520px;margin:0 auto;background:#fff;border-radius:12px;" +
        "padding:40px;box-shadow:0 2px 12px rgba(0,0,0,.08)}" +
        ".logo{font-size:32px;text-align:center;margin-bottom:8px}" +
        "h1{color:#3d2c1e;font-size:22px;text-align:center;margin:0 0 24px}" +
        "p{color:#5a4a3a;line-height:1.6;margin:0 0 16px}" +
        ".btn{display:inline-block;background:#6f4e37;color:#fff;padding:12px 28px;" +
        "border-radius:8px;text-decoration:none;font-weight:bold;margin:8px 0}" +
        ".footer{color:#9e8a7a;font-size:12px;text-align:center;margin-top:32px}" +
        "</style></head><body>" +
        "<div class='card'>" +
        "<div class='logo'>☕</div>" +
        "<h1>Borgol — Кофе Платформд тавтай морил!</h1>" +
        "<p>Сайн байна уу, <strong>%s</strong>!</p>" +
        "<p>Бүртгэл амжилттай үүслээ. Та одоо кофе жор хуваалцах, " +
        "кафе судлах, дарлалтын тэмдэглэл хөтлөх боломжтой боллоо.</p>" +
        "<p>Borgol нийгэмлэгт нэгдсэнд баярлалаа ☕</p>" +
        "<div class='footer'>Borgol Coffee Platform · Улаанбаатар</div>" +
        "</div></body></html>";

    /** Нууц үг сэргээх имэйлийн HTML загвар */
    private static final String RESET_TEMPLATE =
        "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
        "<style>" +
        "body{font-family:Arial,sans-serif;background:#f5f0eb;margin:0;padding:20px}" +
        ".card{max-width:520px;margin:0 auto;background:#fff;border-radius:12px;" +
        "padding:40px;box-shadow:0 2px 12px rgba(0,0,0,.08)}" +
        "h1{color:#3d2c1e;font-size:22px;text-align:center;margin:0 0 24px}" +
        "p{color:#5a4a3a;line-height:1.6;margin:0 0 16px}" +
        ".token{font-family:monospace;background:#f0e8df;padding:12px 16px;" +
        "border-radius:8px;font-size:14px;word-break:break-all;color:#3d2c1e}" +
        ".warn{color:#a05c2e;font-size:13px}" +
        ".footer{color:#9e8a7a;font-size:12px;text-align:center;margin-top:32px}" +
        "</style></head><body>" +
        "<div class='card'>" +
        "<h1>☕ Нууц үг сэргээх</h1>" +
        "<p>Borgol дансны нууц үг сэргээх хүсэлт ирлээ.</p>" +
        "<p>Дараах токен ашиглан нууц үгийг шинэчилнэ үү:</p>" +
        "<div class='token'>%s</div>" +
        "<p class='warn'>⚠️ Токен 30 минутын дараа хүчингүй болно.<br>" +
        "Хэрэв та хүсэлт гаргаагүй бол энэ имэйлийг үл тоомсорлоно уу.</p>" +
        "<div class='footer'>Borgol Coffee Platform · Улаанбаатар</div>" +
        "</div></body></html>";

    /** Ерөнхий мэдэгдлийн имэйлийн HTML загвар */
    private static final String NOTIFICATION_TEMPLATE =
        "<!DOCTYPE html><html><head><meta charset='UTF-8'>" +
        "<style>" +
        "body{font-family:Arial,sans-serif;background:#f5f0eb;margin:0;padding:20px}" +
        ".card{max-width:520px;margin:0 auto;background:#fff;border-radius:12px;" +
        "padding:40px;box-shadow:0 2px 12px rgba(0,0,0,.08)}" +
        "h1{color:#3d2c1e;font-size:20px;margin:0 0 20px}" +
        "p{color:#5a4a3a;line-height:1.6;white-space:pre-wrap}" +
        ".footer{color:#9e8a7a;font-size:12px;text-align:center;margin-top:32px}" +
        "</style></head><body>" +
        "<div class='card'>" +
        "<h1>☕ %s</h1>" +
        "<p>%s</p>" +
        "<div class='footer'>Borgol Coffee Platform · Улаанбаатар</div>" +
        "</div></body></html>";

    /**
     * Орчны хувьсагчаас SMTP тохиргоо уншиж Jakarta Mail Session үүсгэнэ.
     * Тохиргоо хийгдээгүй бол (SMTP_HOST хоосон) configured=false,
     * имэйл илгээхийг оролдохгүй.
     */
    private EmailService() {
        String smtpHost = System.getenv("SMTP_HOST");
        String smtpPort = System.getenv().getOrDefault("SMTP_PORT", "587");
        String smtpUser = System.getenv("SMTP_USER");
        String smtpPass = System.getenv("SMTP_PASSWORD");
        this.fromAddress = System.getenv().getOrDefault("EMAIL_FROM", "Borgol <no-reply@borgol.mn>");

        if (smtpHost == null || smtpHost.isBlank()) {
            log.warn("[Email] SMTP_HOST тохируулагдаагүй — имэйл идэвхгүй");
            this.session    = null;
            this.configured = false;
            return;
        }

        // ── Jakarta Mail SMTP тохиргоо ────────────────────────────────────────
        // STARTTLS: 587 порт дээр plain холболтоос TLS рүү шилждэг
        Properties props = new Properties();
        props.put("mail.smtp.host",            smtpHost);
        props.put("mail.smtp.port",            smtpPort);
        props.put("mail.smtp.auth",            "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout",           "5000");

        // Authenticator: нэвтрэх нэр, нууц үгийг lambda биш anonymous class-аар
        // → Java Mail-ийн Authenticator abstract class шаардлагатай тул
        final String user = smtpUser != null ? smtpUser : "";
        final String pass = smtpPass != null ? smtpPass : "";

        this.session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(user, pass);
            }
        });
        this.configured = true;
        log.info("[Email] SMTP тохиргоо амжилттай → {}:{}", smtpHost, smtpPort);
    }

    /**
     * Double-Checked Locking — thread-safe Singleton хандалт.
     *
     * @return EmailService-ийн цорын ганц жишээ
     */
    public static EmailService get() {
        if (instance == null) {
            synchronized (EmailService.class) {
                if (instance == null) {
                    instance = new EmailService();
                }
            }
        }
        return instance;
    }

    // ── Нийтийн имэйл методууд ───────────────────────────────────────────────
    // Silent fail: exception → ERROR log, caller-д шиддэггүй
    // Зарчим: имэйл нь туслах функц — бүртгэл/нэвтрэлтийн
    //         гол урсгалыг тасалдуулж болохгүй

    /**
     * Тавтай морилох имэйл илгээнэ — шинэ хэрэглэгч бүртгүүлсний дараа.
     *
     * @param toEmail  хүлээн авагчийн и-мэйл хаяг
     * @param username шинэ хэрэглэгчийн нэр
     */
    public void sendWelcomeEmail(String toEmail, String username) {
        String subject = "☕ Borgol дээр тавтай морил, " + username + "!";
        String body    = String.format(WELCOME_TEMPLATE, username);
        send(toEmail, subject, body);
    }

    /**
     * Нууц үг сэргээх имэйл илгээнэ.
     *
     * @param toEmail    хүлээн авагчийн и-мэйл хаяг
     * @param resetToken нууц үг сэргээх токен
     */
    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        String subject = "☕ Borgol — Нууц үг сэргээх хүсэлт";
        String body    = String.format(RESET_TEMPLATE, resetToken);
        send(toEmail, subject, body);
    }

    /**
     * Ерөнхий мэдэгдлийн имэйл илгээнэ.
     *
     * @param toEmail хүлээн авагчийн и-мэйл хаяг
     * @param subject гарчиг
     * @param body    мэдэгдлийн текст
     */
    public void sendNotificationEmail(String toEmail, String subject, String body) {
        String html = String.format(NOTIFICATION_TEMPLATE, subject, body);
        send(toEmail, "☕ " + subject, html);
    }

    // ── Дотоод туслах метод ──────────────────────────────────────────────────

    /**
     * HTML имэйл бэлдэж Transport ашиглан илгээнэ.
     * Алдаа гарвал ERROR лог болгоно — exception шиддэггүй.
     *
     * @param toEmail хүлээн авагч
     * @param subject гарчиг
     * @param html    HTML агуулга
     */
    private void send(String toEmail, String subject, String html) {
        if (!configured) {
            log.debug("[Email] SMTP тохируулагдаагүй — имэйл алгасав: {}", toEmail);
            return;
        }
        try {
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject(subject, "UTF-8");
            // text/html; charset=UTF-8 → HTML форматтай, Монгол тэмдэгт дэмжнэ
            message.setContent(html, "text/html; charset=UTF-8");
            Transport.send(message);
            log.debug("[Email] Амжилттай илгээв → {}", toEmail);
        } catch (Exception e) {
            // Silent fail — имэйл алдаа нь core функцийг тасалдуулахгүй
            log.error("[Email] Илгээхэд алдаа гарлаа → {} : {}", toEmail, e.getMessage());
        }
    }
}
