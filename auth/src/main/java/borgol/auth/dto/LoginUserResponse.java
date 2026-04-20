package borgol.auth.dto;

import jakarta.xml.bind.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SOAP response for the LoginUser operation.
 *
 * On success: success=true, token=<JWT>, userId, username
 * On failure: success=false, message describes the error
 */
@Data
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"success", "token", "userId", "username", "message"})
@XmlRootElement(name = "LoginUserResponse")
public class LoginUserResponse {

    @XmlElement(required = true)
    private boolean success;

    /** JWT token – present only on successful login. */
    private String token;

    /** User ID – present only on successful login. */
    private Integer userId;

    /** Username – present only on successful login. */
    private String username;

    @XmlElement(required = true)
    private String message;
}
