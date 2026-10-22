
package dj_platform;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

@Service
public class SentimentAnalyzer {

    private static final List<String> POSITIVE_WORDS = List.of(
            "amazing", "awesome", "excellent", "great",
            "good", "love", "lovely", "happy", "best",
            "fantastic", "wonderful", "enjoy", "enjoying",
            "like", "beautiful", "super", "nice",
            "अच्छा", "बढ़िया", "शानदार", "पसंद", "मज़ा");

    private static final List<String> NEGATIVE_WORDS = List.of(
            "bad", "hate", "terrible", "awful", "boring",
            "worst", "sad", "angry", "dislike", "poor",
            "annoying", "horrible", "disappointed",
            "खराब", "बेकार", "बुरा", "नापसंद", "उदास");

    public String analyze(String message) {
        if (message == null || message.isBlank()) {
            return "NEUTRAL";
        }

        String text = message.toLowerCase(Locale.ROOT);
        int positiveScore = 0;
        int negativeScore = 0;

        for (String word : POSITIVE_WORDS) {
            if (containsWord(text, word)) {
                positiveScore++;
            }
        }

        for (String word : NEGATIVE_WORDS) {
            if (containsWord(text, word)) {
                negativeScore++;
            }
        }

        if (positiveScore > negativeScore) {
            return "POSITIVE";
        }

        if (negativeScore > positiveScore) {
            return "NEGATIVE";
        }

        return "NEUTRAL";
    }

    private boolean containsWord(String text, String word) {
        if (word.codePoints().anyMatch(Character::isLetter)
                && word.codePoints().allMatch(
                        cp -> cp < 128 || !Character.isLetter(cp))) {

            return text.matches(
                    "(?s).*\\b" + java.util.regex.Pattern.quote(word)
                            + "\\b.*");
        }

        return text.contains(word);
    }
}
