package app.pomee4.extension.commentswipe;

import android.content.Context;
import android.content.SharedPreferences;

import app.pomee4.extension.commentswipe.CommentSwipeActionsPatch.SwipeAction;
import app.pomee4.extension.commentswipe.CommentSwipeActionsPatch.SwipeDirection;

/**
 * Comment swipe settings, kept in their own preferences file so they do not depend on
 * the settings of other patches. Edited in {@link SettingsActivity}.
 */
public final class Settings {
    static final String PREFERENCES_NAME = "pomee4_comment_swipe";

    public static final BooleanSetting COMMENT_SWIPE_ACTIONS = new BooleanSetting("comment_swipe_actions", true);
    public static final EnumSetting<SwipeAction> COMMENT_SWIPE_FIRST =
            new EnumSetting<>("comment_swipe_first", SwipeAction.UPVOTE);
    public static final EnumSetting<SwipeAction> COMMENT_SWIPE_SECOND =
            new EnumSetting<>("comment_swipe_second", SwipeAction.SAVE);
    public static final EnumSetting<SwipeAction> COMMENT_SWIPE_THIRD =
            new EnumSetting<>("comment_swipe_third", SwipeAction.REPLY);
    public static final BooleanSetting COMMENT_SWIPE_VIBRATE = new BooleanSetting("comment_swipe_vibrate", true);
    public static final EnumSetting<SwipeDirection> COMMENT_SWIPE_DIRECTION =
            new EnumSetting<>("comment_swipe_direction", SwipeDirection.RIGHT_TO_LEFT);

    private static volatile SharedPreferences preferences;

    private Settings() {
    }

    static void init(Context context) {
        if (preferences == null) preferences = preferences(context);
    }

    static SharedPreferences preferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE);
    }

    public static final class BooleanSetting {
        final String key;
        final boolean defaultValue;

        BooleanSetting(String key, boolean defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public boolean get() {
            SharedPreferences prefs = preferences;
            return prefs == null ? defaultValue : prefs.getBoolean(key, defaultValue);
        }
    }

    public static final class EnumSetting<T extends Enum<T>> {
        final String key;
        final T defaultValue;

        EnumSetting(String key, T defaultValue) {
            this.key = key;
            this.defaultValue = defaultValue;
        }

        public T get() {
            SharedPreferences prefs = preferences;
            if (prefs == null) return defaultValue;
            String name = prefs.getString(key, null);
            if (name == null) return defaultValue;
            try {
                return Enum.valueOf(defaultValue.getDeclaringClass(), name);
            } catch (IllegalArgumentException ex) {
                return defaultValue;
            }
        }
    }
}
