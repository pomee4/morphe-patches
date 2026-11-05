/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to this code.
 */
package app.morphe.extension.reddit.settings.preference.categories;

import static app.morphe.extension.shared.StringRef.str;

import android.content.Context;
import android.preference.Preference;
import android.preference.PreferenceScreen;

import app.morphe.extension.reddit.patches.CommentSwipeActionsPatch;
import app.morphe.extension.reddit.settings.Settings;
import app.morphe.extension.reddit.settings.preference.BooleanSettingPreference;
import app.morphe.extension.shared.settings.Setting;
import app.morphe.extension.shared.settings.preference.SortedListPreference;

@SuppressWarnings("deprecation")
public class CommentsPreferenceCategory extends ConditionalPreferenceCategory {
    public CommentsPreferenceCategory(Context context, PreferenceScreen screen) {
        super(context, screen);
        setTitle(str("morphe_screen_comments_title"));
    }

    @Override
    public boolean getSettingsStatus() {
        return CommentSwipeActionsPatch.isPatchIncluded();
    }

    @Override
    public void addPreferences(Context context) {
        if (!CommentSwipeActionsPatch.isPatchIncluded()) return;

        BooleanSettingPreference enabled = new BooleanSettingPreference(context, Settings.COMMENT_SWIPE_ACTIONS);
        addPreference(enabled);

        Preference[] dependents = {
                listPreference(context, Settings.COMMENT_SWIPE_FIRST),
                listPreference(context, Settings.COMMENT_SWIPE_SECOND),
                listPreference(context, Settings.COMMENT_SWIPE_THIRD),
                listPreference(context, Settings.COMMENT_SWIPE_DIRECTION),
                new BooleanSettingPreference(context, Settings.COMMENT_SWIPE_VIBRATE),
        };
        for (Preference preference : dependents) {
            addPreference(preference);
            preference.setEnabled(Settings.COMMENT_SWIPE_ACTIONS.get());
        }

        enabled.setOnPreferenceChangeListener((preference, newValue) -> {
            boolean on = Boolean.TRUE.equals(newValue);
            for (Preference dependent : dependents) dependent.setEnabled(on);
            return true;
        });
    }

    private static Preference listPreference(Context context, Setting<?> setting) {
        SortedListPreference preference = new SortedListPreference(context, setting);
        preference.setSummary("%s");
        return preference;
    }
}
