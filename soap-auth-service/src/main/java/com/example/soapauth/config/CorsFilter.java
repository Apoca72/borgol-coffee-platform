package com.example.soapauth.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Global CORS filter so the frontend (running on any port) can call this SOAP service directly.
 *
 * Adds the following headers to every response:
 *   Access-Control-Allow-Origin:  *
 *   Access-Control-Allow-Methods: POST, GET, OPTIONS
 *   Access-Control-Allow-Headers: Content-Type, SOAPAction, Authorization
 */
@Component
@Order(1)
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest  request  = (HttpServletRequest)  req;

        response.setHeader("Access-Control-Allow-Origin",  "*");
        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type, SOAPAction, Authorization");
        response.setHeader("Access-Control-Max-Age",       "3600");

        // Pre-flight – respond immediately without forwarding
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        chain.doFilter(req, res);
    }
}
