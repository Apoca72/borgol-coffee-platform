package borgol.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Lab 06 – User SOAP Service (Authentication Service)
 *
 * SOAP operations exposed at http://localhost:8081/ws :
 *   • RegisterUser  – creates a new auth account, returns userId
 *   • LoginUser     – validates credentials, returns JWT token
 *   • ValidateToken – checks whether a JWT token is valid
 *
 * WSDL: http://localhost:8081/ws/authService.wsdl
 */
@SpringBootApplication
public class SoapAuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(SoapAuthApplication.class, args);
        System.out.println();
        System.out.println("  ╔═══════════════════════════════════════════════╗");
        System.out.println("  ║   SOAP Auth Service  –  Lab 06                ║");
        System.out.println("  ║   Endpoint : http://localhost:8081/ws          ║");
        System.out.println("  ║   WSDL     : /ws/authService.wsdl             ║");
        System.out.println("  ╚═══════════════════════════════════════════════╝");
    }
}
