package com.averygrimes.secretschest.config;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.UUID;

@Component
public class RequestFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        Filter.super.init(filterConfig);
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        String requestId = ((HttpServletRequest) servletRequest).getHeader("request-identifier");
        if (requestId == null) {
            requestId = UUID.randomUUID().toString();
        }
        // Add the requestId to MDC context
        MDC.put("requestId", requestId);
        String groupId = ((HttpServletRequest) servletRequest).getHeader("group-identifier");
        MDC.put("groupId", groupId);
        try {
            // Continue the request-response cycle
            filterChain.doFilter(servletRequest, servletResponse);
        } finally {
            // Clean up the MDC after the request is processed
            MDC.remove("requestId");
        }
    }

    @Override
    public void destroy() {
        Filter.super.destroy();
    }
}