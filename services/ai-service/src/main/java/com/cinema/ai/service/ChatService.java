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
            Bên dưới là DỮ LIỆU THẬT về phim đang chiếu, thể loại, rạp và lịch chiếu đã công bố,
            do hệ thống cung cấp. Quy tắc bắt buộc:
            - Khi nói về phim, thể loại, rạp, giờ chiếu hoặc giá, chỉ dùng thông tin trong DỮ LIỆU THẬT.
              Không bịa phim, suất chiếu, rạp, giá, số ghế hay đường dẫn đặt vé.
            - Nếu dữ liệu không có thứ người dùng hỏi (phim không trong danh sách, ngày ngoài khoảng
              đã nạp, hoặc nguồn dữ liệu tạm không tra cứu được), hãy nói rõ và mời họ xem trang lịch chiếu.
            - Bạn không biết tình trạng ghế trống: không được nói còn ghế hay hết ghế.
              Giá là giá niêm yết của suất chiếu; giá cuối cùng hiển thị khi thanh toán.
            - Bạn không thể đặt vé, giữ ghế hay thanh toán; không được tự nhận đã làm việc đó.
            - Gợi ý phim theo thể loại hoặc nhu cầu người dùng nêu, kèm suất chiếu phù hợp nếu có.

            """;

    private final ChatClient chatClient;
    private final CatalogContextService catalogContext;

    public ChatService(ChatClient.Builder chatClientBuilder, CatalogContextService catalogContext) {
        this.chatClient = chatClientBuilder.build();
        this.catalogContext = catalogContext;
    }

    public ChatResponse chat(ChatRequest request) {
        List<Message> messages = new ArrayList<>();
        for (ChatMessage message : request.history()) {
            messages.add(toSpringMessage(message));
        }
        messages.add(new UserMessage(request.message()));

        String response = chatClient.prompt()
                .system(SYSTEM_PROMPT + catalogContext.currentContext())
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
