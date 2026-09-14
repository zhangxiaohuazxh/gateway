package cn.hubbo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@Slf4j
@SpringBootApplication
public class GatewayApplication {

    static void main(String[] args) {
        log.info("hubbo gateway开始启动");
        SpringApplication.run(GatewayApplication.class, args);
        log.info("hubbo gateway启动成功");
    }

}
