package app.pomee4.extension.commentswipe;

import app.pomee4.extension.commentswipe.CommentSwipeActionsPatch.SwipeAction;
import app.pomee4.extension.commentswipe.CommentSwipeActionsPatch.SwipeDirection;

/**
 * Texts of the swipe actions and the settings screen (English).
 */
final class Labels {
    private Labels() {
    }

    static String action(SwipeAction action) {
        switch (action) {
            case UPVOTE:
                return "Upvote";
            case DOWNVOTE:
                return "Downvote";
            case REPLY:
                return "Reply";
            case SAVE:
                return "Save";
            case SHARE:
                return "Share";
            case COLLAPSE:
                return "Collapse thread";
            case OVERFLOW:
                return "More options";
            case PROFILE:
                return "Open profile";
            default:
                return "None";
        }
    }

    static String direction(SwipeDirection direction) {
        return direction == SwipeDirection.LEFT_TO_RIGHT ? "Left to right" : "Right to left";
    }
}
