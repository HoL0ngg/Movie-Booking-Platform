package com.cinema.ai.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import com.cinema.ai.config.CatalogProperties;
import com.cinema.ai.dto.CatalogQuery;
import com.cinema.ai.dto.ChatMessage;
import com.cinema.ai.dto.ChatRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.ChatOptions;

class ChatServiceTest {
    private final ChatModel model = mock(ChatModel.class);
    private final CatalogQueryService queries = mock(CatalogQueryService.class);
    private final CatalogProperties properties = new CatalogProperties("http://movies", "http://cinemas",
            "Asia/Ho_Chi_Minh", Duration.ofSeconds(1), Duration.ofSeconds(2), 40, 80);
    private final ChatService service = new ChatService(ChatClient.builder(model), queries, properties);

    @Test
    void mapsStructuredOutputToTypedQueryAndPassesHistoryWithoutASecondModelCall() {
        respond("""
                {"intent":"SHOWTIMES","movieQuery":"Phim A","dateFrom":"2026-11-14",
                 "dateTo":"2026-11-14","timeFrom":"19:00:00"}
                """);
        when(queries.answer(any())).thenReturn("Dữ liệu thật");
        assertThat(service.chat(new ChatRequest("Còn tối mai?", List.of(
                new ChatMessage(ChatMessage.Role.USER, "Tìm phim A")))).message()).isEqualTo("Dữ liệu thật");
        ArgumentCaptor<CatalogQuery> query = ArgumentCaptor.forClass(CatalogQuery.class);
        verify(queries).answer(query.capture());
        assertThat(query.getValue().dateFrom()).isEqualTo(LocalDate.of(2026, 11, 14));
        assertThat(query.getValue().movieQuery()).isEqualTo("Phim A");
        ArgumentCaptor<Prompt> prompt = ArgumentCaptor.forClass(Prompt.class);
        verify(model, times(1)).call(prompt.capture());
        assertThat(prompt.getValue().getInstructions()).hasSize(3);
        assertThat(prompt.getValue().getSystemMessage().getText()).contains("Ngày hiện tại", "schema");
    }

    @ParameterizedTest
    @ValueSource(strings = {"not JSON", "null", "{}", "{\"intent\":0}", "{\"intent\":\"DELETE_DATABASE\"}",
            "{\"intent\":\"MOVIES\",\"url\":\"http://internal/admin\"}",
            "{\"intent\":\"SHOWTIMES\",\"dateFrom\":\"2026-02-30\"}",
            "{\"intent\":\"MOVIES\"} {\"intent\":\"CINEMAS\"}"})
    void malformedOutputCannotTriggerApiOrchestration(String output) {
        respond(output);
        // Null/absent intent is also validated before orchestration.
        assertThat(service.chat(new ChatRequest("Tra cứu", List.of())).message())
                .isEqualTo(CatalogQueryService.CLARIFICATION);
        verifyNoInteractions(queries);
    }

    private void respond(String text) {
        when(model.getOptions()).thenReturn(ChatOptions.builder().build());
        when(model.call(any(Prompt.class))).thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage(text)))));
    }
}
