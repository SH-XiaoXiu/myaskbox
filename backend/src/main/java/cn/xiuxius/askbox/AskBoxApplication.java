package cn.xiuxius.askbox;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.scheduling.annotation.EnableScheduling;

import lombok.extern.slf4j.Slf4j;

@SpringBootApplication
@MapperScan("cn.xiuxius.askbox.**.mapper")
@EnableCaching
@EnableScheduling
@Slf4j
public class AskBoxApplication {

    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        ConfigurableApplicationContext app = SpringApplication.run(AskBoxApplication.class, args);
        ConfigurableEnvironment env = app.getEnvironment();
        printStartupInfo(env);
        log.info("AskBox started in {} ms", System.currentTimeMillis() - startTime);
    }

    private static void printStartupInfo(ConfigurableEnvironment env) {
        String port = env.getProperty("server.port", "8080");
        log.info(
                "AskBox is running! Access URLs:\n"
                        + "\tLocal:      http://localhost:{}/\n"
                        + "\tSwagger UI: http://localhost:{}/doc.html\n"
                        + "\tHealth:     http://localhost:{}/api/health",
                port,
                port,
                port);
    }
}
