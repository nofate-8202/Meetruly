package com.meetruly.core.util;

import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^(?=.*[0-9])(?=.*[A-Z]).{6,}$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]{3,20}$");

    // Валидиране на имейл
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    // Валидиране на парола - минимум 6 символа, поне една цифра и една главна буква
    public static boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    // Валидиране на потребителско име - 3-20 символа, букви, цифри, _ и -
    public static boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    // Валидиране на възраст - между 18 и 120 години
    public static boolean isValidAge(Integer age) {
        return age != null && age >= 18 && age <= 120;
    }

    // Валидиране на височина - между 50 и 250 см
    public static boolean isValidHeight(Integer height) {
        return height != null && height >= 50 && height <= 250;
    }

    // Валидиране на тегло - между 30 и 300 кг
    public static boolean isValidWeight(Integer weight) {
        return weight != null && weight >= 30 && weight <= 300;
    }
}
