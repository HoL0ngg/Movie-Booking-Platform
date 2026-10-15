package com.cinema.ai.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.cinema.ai.dto.ChatMessage;
import com.cinema.ai.dto.ChatRequest;
import com.cinema.ai.dto.ChatResponse;

@Service
@ConditionalOnProperty(name = "spring.ai.model.chat", havingValue = "openai")
public class ChatService {

    private static final String SYSTEM_PROMPT = """
            Bạn là Cinemate, trợ lý điện ảnh của nền tảng đặt vé rạp chiếu phim.
            Hãy trả lời bằng tiếng Việt, thân thiện, ngắn gọn và hữu ích.
            Hiện tại bạn chỉ hỗ trợ hội thoại tư vấn phim, phim nổi bật và lịch chiếu
            ở mức hướng dẫn chung. Bạn chưa được truy cập dữ liệu phim/lịch chiếu thật,
            nên không được bịa tên suất chiếu, giá vé, số ghế, tình trạng còn chỗ hoặc
            đường dẫn đặt vé. Bạn cũng không được tự nhận đã đặt vé hay gọi service khác.
            Khi người dùng hỏi dữ liệu cụ thể, hãy nói rõ tính năng tra cứu đang được
            tích hợp và đề nghị họ cung cấp thêm nhu cầu xem phim nếu phù hợp.
            """;

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.defaultSystem(SYSTEM_PROMPT).build();
    }

    public ChatResponse chat(ChatRequest request) {
        List<Message> messages = new ArrayList<>();
        for (ChatMessage message : request.history()) {
            messages.add(toSpringMessage(message));
        }
        messages.add(new UserMessage(request.message()));

        String response = chatClient.prompt()
                .messages(messages)
                .call()
                .content();

        return new ChatResponse(response == null ? "" : response);
    }

    private Message toSpringMessage(ChatMessage message) {
        return message.role() == ChatMessage.Role.USER
                ? new UserMessage(message.content())
                : new AssistantMessage(message.content());
    }
}
