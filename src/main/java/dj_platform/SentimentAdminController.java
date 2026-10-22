
package dj_platform;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/admin/sentiment")
public class SentimentAdminController {

    private final SentimentBackfillService backfillService;

    public SentimentAdminController(
            SentimentBackfillService backfillService) {
        this.backfillService = backfillService;
    }

    @PostMapping("/backfill")
    public Map<String, Object> backfill(Authentication authentication) {
        if (authentication == null
                || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Please log in first.");
        }

        int updated = backfillService.updateOldMessages();

        return Map.of(
                "message", "Sentiment backfill completed",
                "updatedMessages", updated);
    }
}
