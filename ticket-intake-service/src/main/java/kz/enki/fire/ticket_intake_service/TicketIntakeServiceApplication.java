package kz.enki.fire.ticket_intake_service;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.io.File;
import java.net.URL;
import java.util.Enumeration;

@SpringBootApplication
public class TicketIntakeServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TicketIntakeServiceApplication.class, args);
	}

	@PostConstruct
	public void debugClasspath() {
		System.out.println("--- CLASSPATH DEBUG ---");
		try {
			Enumeration<URL> resources = getClass().getClassLoader().getResources("db/changelog/");
			while (resources.hasMoreElements()) {
				System.out.println("Found resource: " + resources.nextElement());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		System.out.println("-----------------------");
	}

}
