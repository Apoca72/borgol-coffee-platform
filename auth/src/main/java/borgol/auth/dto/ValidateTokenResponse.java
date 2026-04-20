package borgol.auth.dto;

import jakarta.xml.bind.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SOAP response for the ValidateToken operation.
 *
 * On valid token:   valid=true,  userId, username
 * On expired/bad:   valid=false
 */
@Data
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"valid", "userId", "username"})
@XmlRootElement(name = "ValidateTokenResponse")
public class ValidateTokenResponse {

    @XmlElement(required = true)
    private boolean valid;

    /** Present only when valid = true. */
    private Integer userId;

    /** Present only when valid = true. */
    private String username;
}
