package borgol.auth.dto;

import jakarta.xml.bind.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * SOAP request for the LoginUser operation.
 *
 * Flow: LoginUser(email, password) → generate JWT → return token
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"email", "password"})
@XmlRootElement(name = "LoginUserRequest")
public class LoginUserRequest {

    @XmlElement(required = true)
    private String email;

    @XmlElement(required = true)
    private String password;
}
