package pl.mo.planz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"pl.mo.planz", "pl.mo.planz.controllers"})
public class PlanzApplication extends SpringBootServletInitializer  {

	public static void main(String[] args) {
		SpringApplication.run(PlanzApplication.class, args);
	}

}
