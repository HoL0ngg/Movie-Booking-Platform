package com.cinema.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(properties = {"spring.ai.model.chat=openai", "spring.ai.openai.api-key=fake-no-inference"})
class AiOpenRouterStartupTest {
    @Autowired ApplicationContext context;

    @Test
    void createsChatServiceWithFakeKeyWithoutInference() {
        assertThat(context.getBeansOfType(ChatService.class)).hasSize(1);
        assertThat(context.getEnvironment().getProperty("spring.ai.openai.base-url"))
                .isEqualTo("https://openrouter.ai/api/v1");
        assertThat(context.getEnvironment().getProperty("spring.ai.openai.chat.options.model"))
                .isEqualTo("openrouter/free");
    }
}
