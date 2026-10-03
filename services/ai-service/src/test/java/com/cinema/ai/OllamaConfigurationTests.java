package com.cinema.ai;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(properties = {"spring.ai.model.chat=ollama", "spring.ai.ollama.init.pull-model-strategy=never"})
class OllamaConfigurationTests {
    @Autowired
    private ApplicationContext context;

    @Test
    void configuresSpringAiWithoutCallingOrDownloadingAModel() {
        assertThat(context.getBeansOfType(ChatModel.class)).hasSize(1);
    }
}
