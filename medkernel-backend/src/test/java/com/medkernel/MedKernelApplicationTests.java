package com.medkernel;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("dev")
class MedKernelApplicationTests {

    @Test
    void contextLoads() {
        // smoke: Spring Boot 3.3 + JDK 21 + Jakarta EE 加载 OK
    }
}
