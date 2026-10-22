
package dj_platform;

import org.springframework.stereotype.Service;

@Service
public class DjTransitionService {

    public String recommendTransition(
            MusicQueueItem current,
            MusicQueueItem next) {

        if (current == null || next == null) {
            return "NOT_AVAILABLE";
        }

        Integer currentBpm = current.getBpm();
        Integer nextBpm = next.getBpm();

        if (currentBpm == null || nextBpm == null) {
            return "ADD_BPM";
        }

        if (currentBpm < 30 || currentBpm > 300
                || nextBpm < 30 || nextBpm > 300) {
            return "INVALID_BPM";
        }

        double difference = calculateBpmDifference(
                currentBpm, nextBpm);

        boolean sameGenre = hasSameGenre(current, next);

        if (difference <= 5) {
            return "SMOOTH";
        }

        if (difference <= 10 && sameGenre) {
            return "SMOOTH";
        }

        if (difference <= 10 || sameGenre) {
            return "MODERATE";
        }

        return "CHALLENGING";
    }

    public String explainTransition(
            MusicQueueItem current,
            MusicQueueItem next) {

        String recommendation = recommendTransition(current, next);

        switch (recommendation) {
            case "NOT_AVAILABLE":
                return "One or both songs are unavailable.";

            case "ADD_BPM":
                return "Add BPM values for both songs to evaluate the transition.";

            case "INVALID_BPM":
                return "Check the song BPM. Valid values must be between 30 and 300.";

            default:
                break;
        }

        double difference = calculateBpmDifference(
                current.getBpm(), next.getBpm());

        boolean sameGenre = hasSameGenre(current, next);

        String bpmText = String.format("%.0f", difference);

        if ("SMOOTH".equals(recommendation)) {
            if (sameGenre) {
                return "Effective BPM difference is "
                        + bpmText
                        + " and genres match, so this transition should be relatively smooth.";
            }

            return "Effective BPM difference is "
                    + bpmText
                    + ". Tempo compatibility looks favorable despite the genre difference.";
        }

        if ("MODERATE".equals(recommendation)) {
            if (sameGenre) {
                return "Genres match, but effective BPM difference is "
                        + bpmText
                        + ". Gradual tempo adjustment may help.";
            }

            return "Effective BPM difference is "
                    + bpmText
                    + ". Some tempo adjustment may be needed.";
        }

        return "Effective BPM difference is "
                + bpmText
                + " and genres differ. Plan the transition carefully.";
    }

    private double calculateBpmDifference(
            int currentBpm, int nextBpm) {

        double directDifference = Math.abs(currentBpm - nextBpm);

        double halfTempoDifference = Math.abs(currentBpm - nextBpm * 2.0);

        double doubleTempoDifference = Math.abs(currentBpm * 2.0 - nextBpm);

        return Math.min(
                directDifference,
                Math.min(halfTempoDifference, doubleTempoDifference));
    }

    private boolean hasSameGenre(
            MusicQueueItem current,
            MusicQueueItem next) {

        String currentGenre = current.getGenre();
        String nextGenre = next.getGenre();

        return currentGenre != null
                && !currentGenre.isBlank()
                && nextGenre != null
                && !nextGenre.isBlank()
                && currentGenre.trim()
                        .equalsIgnoreCase(nextGenre.trim());
    }
}
