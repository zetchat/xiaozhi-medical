package com.atguigu.yygh.appointment;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("需要本地 MySQL/Redis/RabbitMQ 环境后再启用")
class AppointmentCoreSmokeTest {

    @Test
    void contextLoads() {
    }
}
