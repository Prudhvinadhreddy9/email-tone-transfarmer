package com.learning.emailtone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;

/**
 * Entry point.
 *
 * <p>{@code @EnableRetry} activates the {@code @Retryable} annotation on
 * EmailTransformService. Retry works via Spring AOP proxies, so retries only
 * trigger when the method is called through the injected bean — NOT for
 * self-invocations within the same class.
 */
@SpringBootApplication
@EnableRetry
public class EmailToneApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmailToneApplication.class, args);
    }
}
