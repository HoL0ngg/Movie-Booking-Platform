package com.cinema.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest(properties = {"spring.ai.model.chat=none", "spring.ai.openai.api-key="})
class AiDisabledStartupTest {
    @Autowired ApplicationContext context;

    @Test
    void startsWithoutProviderCredentialsOrChatBean() {
        assertThat(context.getBeansOfType(ChatService.class)).isEmpty();
        assertThat(context.getBeansOfType(CatalogQueryService.class)).hasSize(1);
    }
}
