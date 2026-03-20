package com.example.soapauth.dto;

import jakarta.xml.bind.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * SOAP request for the ValidateToken operation.
 *
 * Called by the JSON service's authentication middleware:
 *   ValidateToken(token) → valid: true/false
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"token"})
@XmlRootElement(name = "ValidateTokenRequest")
public class ValidateTokenRequest {

    @XmlElement(required = true)
    private String token;
}
