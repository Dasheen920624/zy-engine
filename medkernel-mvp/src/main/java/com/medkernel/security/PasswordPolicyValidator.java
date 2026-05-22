package com.medkernel.security;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 密码复杂度策略校验器（等保 2.0 三级 - 身份鉴别）。
 *
 * 要求：
 * - 长度 >= 8 位
 * - 包含大写字母
 * - 包含小写字母
 * - 包含数字
 * - 包含特殊字符
 * - 不能与用户名相同
 */
@Component
public class PasswordPolicyValidator {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 128;

    /**
     * 校验密码是否符合复杂度策略。
     *
     * @param password 明文密码
     * @param username 用户名（用于检查密码是否与用户名相同）
     * @return 校验结果，空列表表示通过
     */
    public List<String> validate(String password, String username) {
        List<String> violations = new ArrayList<String>();

        if (password == null || password.isEmpty()) {
            violations.add("密码不能为空");
            return violations;
        }

        if (password.length() < MIN_LENGTH) {
            violations.add("密码长度不能少于 " + MIN_LENGTH + " 位");
        }

        if (password.length() > MAX_LENGTH) {
            violations.add("密码长度不能超过 " + MAX_LENGTH + " 位");
        }

        boolean hasUpper = false;
        boolean hasLower = false;
        boolean hasDigit = false;
        boolean hasSpecial = false;

        for (char c : password.toCharArray()) {
            if (Character.isUpperCase(c)) hasUpper = true;
            else if (Character.isLowerCase(c)) hasLower = true;
            else if (Character.isDigit(c)) hasDigit = true;
            else hasSpecial = true;
        }

        if (!hasUpper) {
            violations.add("密码必须包含大写字母");
        }
        if (!hasLower) {
            violations.add("密码必须包含小写字母");
        }
        if (!hasDigit) {
            violations.add("密码必须包含数字");
        }
        if (!hasSpecial) {
            violations.add("密码必须包含特殊字符");
        }

        if (username != null && password.equalsIgnoreCase(username)) {
            violations.add("密码不能与用户名相同");
        }

        return violations;
    }

    /**
     * 快速判断密码是否符合策略。
     */
    public boolean isValid(String password, String username) {
        return validate(password, username).isEmpty();
    }
}
