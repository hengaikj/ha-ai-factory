package com.hengaikj.ai.factory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.mybatis.spring.annotation.MapperScan;

/** HA AI Software Factory 后端应用入口。 */
@SpringBootApplication
@MapperScan(basePackages = "com.hengaikj.ai.factory.project", annotationClass = org.apache.ibatis.annotations.Mapper.class)
public class HaAiFactoryApplication {

    /** 启动后端 Spring 容器。 */
    public static void main(String[] args) {
        SpringApplication.run(HaAiFactoryApplication.class, args);
    }
}
