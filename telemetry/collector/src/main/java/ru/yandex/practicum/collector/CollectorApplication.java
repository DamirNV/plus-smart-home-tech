package ru.yandex.practicum.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CollectorApplication {

    private static final Logger log = LoggerFactory.getLogger(CollectorApplication.class);

    public static void main(String[] args) {
        log.info("Запуск Collector Application");
        SpringApplication.run(CollectorApplication.class, args);
    }
}