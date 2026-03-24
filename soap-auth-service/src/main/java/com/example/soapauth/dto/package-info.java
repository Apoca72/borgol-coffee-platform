/**
 * JAXB-annotated DTOs for the SOAP auth service.
 *
 * All classes in this package are bound to the namespace
 * http://num.edu.mn/soapauth, which matches the XSD targetNamespace
 * and the @PayloadRoot annotations in AuthEndpoint.
 */
@XmlSchema(
    namespace        = "http://num.edu.mn/soapauth",
    elementFormDefault = XmlNsForm.QUALIFIED
)
package com.example.soapauth.dto;

import jakarta.xml.bind.annotation.XmlNsForm;
import jakarta.xml.bind.annotation.XmlSchema;
