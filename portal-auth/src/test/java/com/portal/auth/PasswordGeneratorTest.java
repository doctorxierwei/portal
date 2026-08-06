package com.portal.auth;

import org.junit.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 密码加密工具测试类。
 * 修改下方的 RAW_PASSWORD 为你想要的明文，运行本测试即可在控制台得到 BCrypt 密文。
 * 复制打印出的密文去替换 sql/portal.sql 或数据库 sys_user.password 字段。
 */
public class PasswordGeneratorTest {

    /** 在这里改成你想设置的明文密码 */
    private static final String RAW_PASSWORD = "123456";

    @Test
    public void printEncodedPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String encoded = encoder.encode(RAW_PASSWORD);

        System.out.println("=========================================");
        System.out.println("明文密码   : " + RAW_PASSWORD);
        System.out.println("BCrypt密文 : " + encoded);
        System.out.println("校验匹配   : " + encoder.matches(RAW_PASSWORD, encoded));
        System.out.println("=========================================");
    }
}
