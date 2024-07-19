package com.touchtrust.touchtrustbank.Utils;

import org.mindrot.jbcrypt.BCrypt;

public class PasswordUtil {

    // Method to hash a password
    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    // Method to check if a password matches a hashed password
    public static boolean checkPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}
