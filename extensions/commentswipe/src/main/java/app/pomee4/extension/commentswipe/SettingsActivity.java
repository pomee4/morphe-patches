package app.pomee4.extension.commentswipe;

import android.app.Activity;
import android.content.Context;
import android.content.pm.ShortcutManager;
import android.os.Build;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

import java.util.Collections;

import app.pomee4.extension.commentswipe.CommentSwipeActionsPatch.SwipeAction;
import app.pomee4.extension.commentswipe.CommentSwipeActionsPatch.SwipeDirection;

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
            enabled.setSummary("Swipe a comment sideways to vote, save, reply and more. How far you swipe picks the action");
            screen.addPreference(enabled);

            Preference[] dependents = {
                    actionPreference(context, Settings.COMMENT_SWIPE_FIRST, "First swipe action"),
                    actionPreference(context, Settings.COMMENT_SWIPE_SECOND, "Second swipe action"),
                    actionPreference(context, Settings.COMMENT_SWIPE_THIRD, "Third swipe action"),
                    directionPreference(context),
                    vibratePreference(context),
            };
            for (Preference preference : dependents) {
                screen.addPreference(preference);
                preference.setDependency(enabled.getKey());
            }
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

        private static ListPreference directionPreference(Context context) {
            SwipeDirection[] values = SwipeDirection.values();
            String[] entries = new String[values.length];
            String[] entryValues = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                entries[i] = Labels.direction(values[i]);
                entryValues[i] = values[i].name();
            }
            return listPreference(context, Settings.COMMENT_SWIPE_DIRECTION.key,
                    Settings.COMMENT_SWIPE_DIRECTION.defaultValue.name(), "Swipe direction", entries, entryValues);
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
