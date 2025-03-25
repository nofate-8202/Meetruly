package com.meetruly.web.config;

import com.meetruly.user.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class ApprovalCheckFilter extends OncePerRequestFilter {

    private final UserService userService;

    @Autowired
    public ApprovalCheckFilter(UserService userService) {
        this.userService = userService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Пропускаме публични ресурси и самата страница за чакащо одобрение
        if (requestURI.startsWith("/auth/") ||
                requestURI.startsWith("/css/") ||
                requestURI.startsWith("/js/") ||
                requestURI.startsWith("/images/") ||
                requestURI.equals("/")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Проверяваме дали потребителят е автентикиран
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {

            try {
                if (!userService.isUserApproved(authentication.getName())) {
                    response.sendRedirect("/auth/pending-approval");
                    return;
                }
            } catch (Exception e) {
                // Продължаваме филтър веригата при грешка
            }
        }

        filterChain.doFilter(request, response);
    }

}
