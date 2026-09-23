package com.hengaikj.haifactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** HA AI Software Factory 后端应用入口。 */
@SpringBootApplication
public class HaAiFactoryApplication {

    /** 启动后端 Spring 容器。 */
    public static void main(String[] args) {
        SpringApplication.run(HaAiFactoryApplication.class, args);
    }
}
