package de.kirchrath.luna.task;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;

@SpringBootApplication
@OpenAPIDefinition(
        info = @Info(title = "Luna Task API", version = "${spring.application.version}"),
        servers = @Server(url = "/api/luna-task"))
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
