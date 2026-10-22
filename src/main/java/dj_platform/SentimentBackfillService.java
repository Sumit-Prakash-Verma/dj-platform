
package dj_platform;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SentimentBackfillService {

    private final ChatMessageRepository chatMessageRepository;
    private final SentimentAnalyzer sentimentAnalyzer;

    public SentimentBackfillService(
            ChatMessageRepository chatMessageRepository,
            SentimentAnalyzer sentimentAnalyzer) {
        this.chatMessageRepository = chatMessageRepository;
        this.sentimentAnalyzer = sentimentAnalyzer;
    }

    @Transactional
    public int updateOldMessages() {
        List<ChatMessage> messages = chatMessageRepository.findBySentimentIsNull();

        for (ChatMessage message : messages) {
            message.setSentiment(
                    sentimentAnalyzer.analyze(message.getMessage()));
        }

        chatMessageRepository.saveAll(messages);
        return messages.size();
    }
}
