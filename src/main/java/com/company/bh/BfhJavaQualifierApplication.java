package com.company.bh;

import com.company.bh.service.QualifierService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@RequiredArgsConstructor
public class BfhJavaQualifierApplication implements CommandLineRunner {

	private final QualifierService qualifierService;

	public static void main(String[] args) {
		SpringApplication.run(BfhJavaQualifierApplication.class, args);
	}

	@Override
	public void run(String... args) {
		qualifierService.runFlow(); // <-- runs automatically on startup
	}
}
