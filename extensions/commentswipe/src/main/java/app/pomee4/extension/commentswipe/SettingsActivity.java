package app.pomee4.extension.commentswipe;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import java.util.Collections;

import app.pomee4.extension.commentswipe.CommentSwipeActionsPatch.SwipeAction;

/**
 * Settings screen of the comment swipe actions. Added to the manifest during patching and opened
 * from the "Comment swipe" row of the Reddit settings (General section).
 */
@SuppressWarnings("deprecation")
public final class SettingsActivity extends Activity {
    private static final String SHORTCUT_ID = "pomee4_comment_swipe_settings";
    private static boolean oldShortcutRemoved;

    /**
     * Removes the launcher shortcut of older versions of the patch, once per process.
     * The settings are now a row of the Reddit settings.
     */
    static void removeOldShortcut(Context context) {
        if (oldShortcutRemoved) return;
        oldShortcutRemoved = true;
        try {
            ShortcutManager manager = context.getSystemService(ShortcutManager.class);
            if (manager != null) manager.removeDynamicShortcuts(Collections.singletonList(SHORTCUT_ID));
        } catch (Exception ex) {
            Logger.printException(() -> "removeOldShortcut failure", ex);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                ? android.R.style.Theme_DeviceDefault_DayNight
                : android.R.style.Theme_DeviceDefault_Settings);
        super.onCreate(savedInstanceState);
        setTitle("Comment swipe actions");
        Settings.init(this);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    public static final class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName(Settings.PREFERENCES_NAME);

            Context context = getActivity();
            PreferenceScreen screen = getPreferenceManager().createPreferenceScreen(context);
            setPreferenceScreen(screen);

            SwitchPreference enabled = new SwitchPreference(context);
            enabled.setKey(Settings.COMMENT_SWIPE_ACTIONS.key);
            enabled.setDefaultValue(Settings.COMMENT_SWIPE_ACTIONS.defaultValue);
            enabled.setTitle("Comment swipe actions");
            enabled.setSummary("Swipe a comment left or right to vote, save, reply and more. How far you swipe picks the first or second action");
            screen.addPreference(enabled);

            addDirection(context, screen, enabled, "Swipe left", Settings.LEFT_FIRST, Settings.LEFT_SECOND);
            addDirection(context, screen, enabled, "Swipe right", Settings.RIGHT_FIRST, Settings.RIGHT_SECOND);

            PreferenceCategory other = new PreferenceCategory(context);
            other.setTitle("Feedback");
            screen.addPreference(other);
            Preference vibrate = vibratePreference(context);
            other.addPreference(vibrate);
            vibrate.setDependency(enabled.getKey());
        }

        private static void addDirection(Context context, PreferenceScreen screen, Preference enabled, String title,
                                         Settings.EnumSetting<SwipeAction> first,
                                         Settings.EnumSetting<SwipeAction> second) {
            PreferenceCategory category = new PreferenceCategory(context);
            category.setTitle(title);
            screen.addPreference(category);

            ListPreference firstPreference = actionPreference(context, first, "First action");
            category.addPreference(firstPreference);
            firstPreference.setDependency(enabled.getKey());

            ListPreference secondPreference = actionPreference(context, second, "Second action");
            category.addPreference(secondPreference);
            secondPreference.setDependency(enabled.getKey());
            updateSecondSummary(secondPreference, second.get());
            secondPreference.setOnPreferenceChangeListener((preference, value) -> {
                updateSecondSummary((ListPreference) preference, SwipeAction.valueOf((String) value));
                return true;
            });
        }

        /**
         * Explains what a longer swipe does when the second action is None.
         */
        private static void updateSecondSummary(ListPreference preference, SwipeAction action) {
            preference.setSummary(action == SwipeAction.NONE
                    ? "None: any swipe distance does the first action. Set both to None to turn off this direction"
                    : Labels.action(action) + ", on a longer swipe");
        }

        private static ListPreference actionPreference(Context context,
                                                       Settings.EnumSetting<SwipeAction> setting, String title) {
            SwipeAction[] values = SwipeAction.values();
            String[] entries = new String[values.length];
            String[] entryValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                entries[i] = Labels.action(values[i]);
                entryValues[i] = values[i].name();
            }
            return listPreference(context, setting.key, setting.defaultValue.name(), title, entries, entryValues);
        }

        private static ListPreference listPreference(Context context, String key, String defaultValue, String title,
                                                     String[] entries, String[] entryValues) {
            ListPreference preference = new ListPreference(context);
            preference.setKey(key);
            preference.setDefaultValue(defaultValue);
            preference.setTitle(title);
            preference.setDialogTitle(title);
            preference.setEntries(entries);
            preference.setEntryValues(entryValues);
            preference.setSummary("%s");
            return preference;
        }

        private static SwitchPreference vibratePreference(Context context) {
            SwitchPreference preference = new SwitchPreference(context);
            preference.setKey(Settings.COMMENT_SWIPE_VIBRATE.key);
            preference.setDefaultValue(Settings.COMMENT_SWIPE_VIBRATE.defaultValue);
            preference.setTitle("Vibrate on each action");
            preference.setSummary("Vibrates when the swipe reaches the next action, even when touch vibration is off");
            return preference;
        }
    }
}
