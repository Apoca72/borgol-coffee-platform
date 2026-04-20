package borgol.auth.dto;

import jakarta.xml.bind.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * SOAP request for the RegisterUser operation.
 *
 * Example SOAP body:
 * <pre>
 *   &lt;auth:RegisterUserRequest xmlns:auth="http://num.edu.mn/soapauth"&gt;
 *     &lt;auth:username&gt;coffee_master&lt;/auth:username&gt;
 *     &lt;auth:email&gt;coffee@borgol.mn&lt;/auth:email&gt;
 *     &lt;auth:password&gt;secret123&lt;/auth:password&gt;
 *   &lt;/auth:RegisterUserRequest&gt;
 * </pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"username", "email", "password"})
@XmlRootElement(name = "RegisterUserRequest")
public class RegisterUserRequest {

    @XmlElement(required = true)
    private String username;

    @XmlElement(required = true)
    private String email;

    @XmlElement(required = true)
    private String password;
}
