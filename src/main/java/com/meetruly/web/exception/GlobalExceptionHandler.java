package com.meetruly.web.exception;

import com.meetruly.core.exception.MeetrulyException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

   
    @ExceptionHandler(MeetrulyException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleMeetrulyException(MeetrulyException ex, HttpServletRequest request) {
        log.error("MeetrulyException occurred: {}", ex.getMessage());

        ModelAndView mav = new ModelAndView();
        mav.addObject("errorMessage", ex.getMessage());
        mav.addObject("status", HttpStatus.BAD_REQUEST.value());
        mav.addObject("path", request.getRequestURI());
        mav.setViewName("error/custom-error");

        return mav;
    }
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNoResourceFoundException(NoResourceFoundException ex, HttpServletRequest request) {
        log.error("Resource not found: {}", ex.getMessage());

        ModelAndView mav = new ModelAndView();
        mav.addObject("errorMessage", "The requested resource was not found.");
        mav.addObject("status", HttpStatus.NOT_FOUND.value());
        mav.addObject("path", request.getRequestURI());

        mav.setViewName("error/404");

        return mav;
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleValidationExceptions(Exception ex, HttpServletRequest request) {
        log.error("Validation error occurred: {}", ex.getMessage());

        ModelAndView mav = new ModelAndView();
        mav.addObject("errorMessage", "Validation error. Please check your input.");
        mav.addObject("status", HttpStatus.BAD_REQUEST.value());
        mav.addObject("path", request.getRequestURI());
        mav.setViewName("error/custom-error");

        return mav;
    }

    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ModelAndView handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        log.error("Access denied: {}", ex.getMessage());

        ModelAndView mav = new ModelAndView();
        mav.addObject("errorMessage", "Access denied. You don't have permission to access this resource.");
        mav.addObject("status", HttpStatus.FORBIDDEN.value());
        mav.addObject("path", request.getRequestURI());
        mav.setViewName("error/custom-error");

        return mav;
    }

    @ExceptionHandler({BadCredentialsException.class, UsernameNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ModelAndView handleAuthenticationException(Exception ex, HttpServletRequest request) {
        log.error("Authentication error: {}", ex.getMessage());

        ModelAndView mav = new ModelAndView();
        mav.addObject("errorMessage", "Authentication failed. Invalid credentials or account not found.");
        mav.addObject("status", HttpStatus.UNAUTHORIZED.value());
        mav.addObject("path", request.getRequestURI());
        mav.setViewName("error/custom-error");

        return mav;
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNoHandlerFoundException(NoHandlerFoundException ex, HttpServletRequest request) {
        log.error("Page not found: {}", ex.getMessage());

        ModelAndView mav = new ModelAndView();
        mav.addObject("errorMessage", "The page you are looking for does not exist.");
        mav.addObject("status", HttpStatus.NOT_FOUND.value());
        mav.addObject("path", request.getRequestURI());
        mav.setViewName("error/404");

        return mav;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleGlobalException(Exception ex, HttpServletRequest request) {
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);

        ModelAndView mav = new ModelAndView();
        mav.addObject("errorMessage", "An unexpected error occurred. Please try again later.");
        mav.addObject("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        mav.addObject("path", request.getRequestURI());
        mav.setViewName("error/500");

        return mav;
    }
}