package com.alotra;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import jakarta.annotation.PostConstruct;
import java.time.ZoneId;
import java.util.TimeZone;

@SpringBootApplication
public class AloTraApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(AloTraApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(AloTraApplication.class, args);
    }

    // Ensure JVM default timezone is Ho Chi Minh to align with SQL and templates
    @PostConstruct
    public void initDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of("Asia/Ho_Chi_Minh")));
    }
}