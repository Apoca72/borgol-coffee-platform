package com.example.soapauth.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.config.annotation.WsConfigurerAdapter;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/**
 * Spring-WS configuration.
 *
 * WSDL is auto-generated from auth-service.xsd and served at:
 *   GET http://localhost:8081/ws/authService.wsdl
 *
 * All SOAP requests go to:
 *   POST http://localhost:8081/ws
 */
@EnableWs
@Configuration
public class WebServiceConfig extends WsConfigurerAdapter {

    /**
     * Registers Spring-WS's MessageDispatcherServlet at /ws/*.
     * transformWsdlLocations=true rewrites <soap:address> to match the actual server URL.
     */
    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(
            ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    /**
     * Generates the WSDL from the XSD schema.
     * Bean name = "authService" → WSDL URL = /ws/authService.wsdl
     */
    @Bean(name = "authService")
    public DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema authServiceSchema) {
        DefaultWsdl11Definition wsdl = new DefaultWsdl11Definition();
        wsdl.setPortTypeName("AuthServicePort");
        wsdl.setLocationUri("/ws");
        wsdl.setTargetNamespace("http://num.edu.mn/soapauth");
        wsdl.setSchema(authServiceSchema);
        return wsdl;
    }

    /** Loads the XSD from the classpath. */
    @Bean
    public XsdSchema authServiceSchema() {
        return new SimpleXsdSchema(new ClassPathResource("xsd/auth-service.xsd"));
    }
}
