package com.servicekit.data;

import com.servicekit.data.repository.BaseRepositoryImpl;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "com.servicekit.data")
@EnableJpaRepositories(basePackages = "com.servicekit.data", repositoryBaseClass = BaseRepositoryImpl.class)
public class TestApplication {
}
