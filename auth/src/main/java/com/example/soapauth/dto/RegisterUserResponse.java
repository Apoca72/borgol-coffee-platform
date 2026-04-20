package com.example.soapauth.dto;

import jakarta.xml.bind.annotation.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SOAP response for the RegisterUser operation.
 */
@Data
@NoArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {"success", "message", "userId"})
@XmlRootElement(name = "RegisterUserResponse")
public class RegisterUserResponse {

    @XmlElement(required = true)
    private boolean success;

    @XmlElement(required = true)
    private String message;

    /** Present only when success = true. */
    private Integer userId;

    public RegisterUserResponse(boolean success, String message, Integer userId) {
        this.success = success;
        this.message = message;
        this.userId  = userId;
    }
}
