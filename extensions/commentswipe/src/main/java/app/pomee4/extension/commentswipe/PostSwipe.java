package app.pomee4.extension.commentswipe;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Rect;
import android.net.Uri;
import android.os.SystemClock;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import kotlin.jvm.functions.Function1;

/**
 * Swipe actions on posts, in every Reddit post list (feeds, subreddits, profiles, saved, history):
 * all of them draw a post with the same feed post section.
 * <p>
 * The post card tells this class its section (the post), its feed context (which takes the feed events)
 * and the state that holds the card's bounds in the window. The swipe itself is done by
 * {@link CommentSwipeActionsPatch}, and an action sends the same feed event as Reddit's own buttons.
 */
@SuppressWarnings("unused")
public final class PostSwipe {

    public enum PostAction {
        NONE,
        UPVOTE,
        DOWNVOTE,
        SAVE,
        HIDE,
        SHARE,
        OPEN_COMMENTS,
        OVERFLOW,
        PROFILE,
        SUBREDDIT
    }

    private static final int MAX_CACHED_LINKS = 1000;

    static final class Card {
        final WeakReference<Object> section;
        final WeakReference<Object> context;
        final WeakReference<Object> bounds;
        long positionedAt;

        Card(Object section, Object context, Object bounds) {
            this.section = new WeakReference<>(section);
            this.context = new WeakReference<>(context);
            this.bounds = new WeakReference<>(bounds);
        }
    }

    /**
     * The post cards, by the state object that holds their bounds.
     */
    private static final Map<Object, Card> cards = Collections.synchronizedMap(new WeakHashMap<>());

    /**
     * Posts (Link) by kind with id, to read the saved state, the vote, the author and the subreddit.
     */
    private static final Map<String, WeakReference<Object>> links = Collections.synchronizedMap(
            new LinkedHashMap<>(128, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, WeakReference<Object>> eldest) {
                    return size() > MAX_CACHED_LINKS;
                }
            });

    private PostSwipe() {
    }

    // region Accessors of the obfuscated post classes, created during patching.

    // Each has one unused parameter as a scratch register for the generated code.

    static String sectionLinkKindWithId(Object section, Object scratch) {
        return null;  // Modified during patching.
    }

    static String sectionUniqueId(Object section, Object scratch) {
        return null;  // Modified during patching.
    }

    static Object sectionIdentifier(Object section, Object scratch) {
        return null;  // Modified during patching.
    }

    static boolean sectionPromoted(Object section, Object scratch) {
        return false;  // Modified during patching.
    }

    static boolean sectionRecommended(Object section, Object scratch) {
        return false;  // Modified during patching.
    }

    static boolean sectionSubredditMember(Object section, Object scratch) {
        return false;  // Modified during patching.
    }

    static Object sectionChildren(Object section, Object scratch) {
        return null;  // Modified during patching.
    }

    /**
     * @return If a child section of a post has its own horizontal swipes (a gallery or a carousel).
     */
    static boolean isOwnSwipeSection(Object child, Object scratch) {
        return false;  // Modified during patching.
    }

    static Object contextEventSink(Object context, Object scratch) {
        return null;  // Modified during patching.
    }

    static Object contextTransitionParams(Object context, Object scratch) {
        return null;  // Modified during patching.
    }

    // endregion

    // region Injection points

    /**
     * Injection point.
     * <p>
     * Called when a post card is composed.
     *
     * @param section The feed post section of the card.
     * @param context The feed context, which takes the feed events.
     * @param bounds  The state that holds the card's bounds in the window.
     */
    public static void onPostCard(Object section, Object context, Object bounds) {
        try {
            if (section == null || context == null || bounds == null) return;
            Card card = cards.get(bounds);
            if (card == null || card.section.get() != section || card.context.get() != context) {
                Card newCard = new Card(section, context, bounds);
                if (card != null) newCard.positionedAt = card.positionedAt;
                cards.put(bounds, newCard);
            }
        } catch (Exception ex) {
            Logger.printException(() -> "onPostCard failure", ex);
        }
    }

    /**
     * Injection point.
     * <p>
     * Called when a layout position callback runs, with the state it updates. Only post cards that are
     * on screen are laid out, so the newest card at a point is the visible one.
     */
    public static void onBoundsChanged(Object bounds) {
        if (bounds == null) return;
        Card card = cards.get(bounds);
        if (card != null) card.positionedAt = SystemClock.uptimeMillis();
    }

    /**
     * Injection point.
     * <p>
     * Called at the end of the Link constructor.
     */
    public static void onLinkCreated(Object link) {
        try {
            String kindWithId = (String) invoke(link, "getKindWithId");
            if (kindWithId != null) links.put(kindWithId, new WeakReference<>(link));
        } catch (Exception ex) {
            Logger.printException(() -> "onLinkCreated failure", ex);
        }
    }

    // endregion

    // region Finding the post under the finger

    /**
     * A post card under the finger, with its bounds in the window.
     */
    static final class Target {
        final Object section;
        final Object context;
        final Rect bounds;

        Target(Object section, Object context, Rect bounds) {
            this.section = section;
            this.context = context;
            this.bounds = bounds;
        }
    }

    /**
     * @return The post card at a point of the window, or null if there is none or it has its own
     * horizontal swipes.
     */
    static Target findTarget(float x, float y) {
        try {
            List<Card> candidates;
            synchronized (cards) {
                candidates = new ArrayList<>(cards.values());
            }
            candidates.sort(Comparator.comparingLong((Card card) -> card.positionedAt).reversed());
            for (Card card : candidates) {
                Object bounds = card.bounds.get();
                Object section = card.section.get();
                Object context = card.context.get();
                if (bounds == null || section == null || context == null) continue;
                Rect rect = readRect(bounds);
                if (rect == null || !rect.contains((int) x, (int) y)) continue;
                // The newest card at the point is the visible one.
                return hasOwnSwipe(section) ? null : new Target(section, context, rect);
            }
        } catch (Throwable ex) {
            Logger.printException(() -> "findTarget failure", ex);
        }
        return null;
    }

    private static boolean hasOwnSwipe(Object section) {
        Object children = sectionChildren(section, null);
        if (!(children instanceof Iterable)) return false;
        for (Object child : (Iterable<?>) children) {
            if (child != null && isOwnSwipeSection(child, null)) return true;
        }
        return false;
    }

    private static volatile Method getValueMethod;
    private static volatile Field[] rectFields;

    /**
     * Reads the Compose Rect (left, top, right, bottom) in the bounds state.
     */
    private static Rect readRect(Object bounds) throws Exception {
        Method getValue = getValueMethod;
        if (getValue == null) {
            getValue = bounds.getClass().getMethod("getValue");
            getValue.setAccessible(true);
            getValueMethod = getValue;
        }
        Object rect = getValue.invoke(bounds);
        if (rect == null) return null;

        Field[] fields = rectFields;
        if (fields == null) {
            // The four float fields, in declaration order: left, top, right, bottom.
            List<Field> floats = new ArrayList<>();
            for (Field field : rect.getClass().getDeclaredFields()) {
                if (field.getType() == float.class && !Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    floats.add(field);
                }
            }
            if (floats.size() != 4) throw new IllegalStateException("Unexpected rect " + rect.getClass());
            floats.sort(Comparator.comparing(Field::getName));
            fields = floats.toArray(new Field[0]);
            rectFields = fields;
        }
        Rect result = new Rect(Math.round(fields[0].getFloat(rect)), Math.round(fields[1].getFloat(rect)),
                Math.round(fields[2].getFloat(rect)), Math.round(fields[3].getFloat(rect)));
        return result.width() > 0 && result.height() > 0 ? result : null;
    }

    // endregion

    // region Actions

    static void perform(Activity activity, Target target, PostAction action) {
        try {
            Object section = target.section;
            String linkKindWithId = sectionLinkKindWithId(section, null);
            String uniqueId = sectionUniqueId(section, null);
            Object identifier = sectionIdentifier(section, null);
            boolean promoted = sectionPromoted(section, null);
            Object link = getLink(linkKindWithId);

            Object event;
            switch (action) {
                case UPVOTE:
                case DOWNVOTE: {
                    Object current = link == null ? null : invoke(link, "getVoteDirection");
                    if (current == null) current = enumValue("com.reddit.domain.model.vote.VoteDirection", "NONE");
                    event = create("com.reddit.feeds.ui.events.OnVoteClicked", 6,
                            linkKindWithId, identifier, uniqueId, promoted,
                            enumValue("com.reddit.ui.compose.ds.VoteButtonDirection",
                                    action == PostAction.UPVOTE ? "Up" : "Down"),
                            current);
                    break;
                }
                case SAVE: {
                    boolean saved = link != null && Boolean.TRUE.equals(invoke(link, "getSaved"));
                    event = create("com.reddit.feeds.impl.ui.events.IsSaved", 6,
                            identifier, withoutKind(linkKindWithId), uniqueId, linkKindWithId, promoted, !saved);
                    break;
                }
                case HIDE:
                    // The same event as Hide in the post menu.
                    event = create("com.reddit.feeds.ui.events.PostHidden", 9,
                            linkKindWithId, uniqueId, promoted, false,
                            enumValue("com.reddit.ads.analytics.HideAdCaller", "HIDE_CLICK"),
                            enumValue("com.reddit.feeds.ui.events.PostHiddenSource", "OVERFLOW_MENU"),
                            null, null, 0x198);
                    break;
                case SHARE:
                    event = create("com.reddit.feeds.impl.ui.events.OnClickShare", 6,
                            linkKindWithId, uniqueId, promoted, postIdValue(identifier), null, 48);
                    break;
                case OPEN_COMMENTS:
                    event = create("com.reddit.feeds.impl.ui.events.OnClickOpenComments", 6,
                            linkKindWithId, uniqueId, promoted, identifier,
                            contextTransitionParams(target.context, null), null);
                    break;
                case OVERFLOW:
                    // The same event as a long press on the post.
                    event = create("com.reddit.feeds.ui.events.OnOverflowMenuOpened", 7,
                            linkKindWithId, uniqueId, promoted,
                            enumValue("com.reddit.feeds.ui.OverflowMenuType",
                                    sectionRecommended(section, null) ? "POST_RECOMMENDED" : "POST_DEFAULT"),
                            enumValue("com.reddit.feeds.ui.OverflowMenuTrigger", "LONG_PRESS"),
                            identifier, sectionSubredditMember(section, null));
                    break;
                case PROFILE: {
                    String author = link == null ? null : (String) invoke(link, "getAuthor");
                    if (author == null || author.isEmpty() || author.equals("[deleted]")) return;
                    open(activity, "https://www.reddit.com/user/" + author);
                    return;
                }
                case SUBREDDIT: {
                    String subreddit = link == null ? null : (String) invoke(link, "getSubreddit");
                    if (subreddit == null || subreddit.isEmpty()) return;
                    open(activity, "https://www.reddit.com/r/" + subreddit);
                    return;
                }
                default:
                    return;
            }

            @SuppressWarnings("unchecked")
            Function1<Object, Object> sink = (Function1<Object, Object>) contextEventSink(target.context, null);
            if (sink == null) throw new IllegalStateException("No event sink");
            sink.invoke(event);
            Logger.printDebug(() -> "Post swipe action " + action + " on " + linkKindWithId);
        } catch (Exception ex) {
            Logger.printException(() -> "Post swipe action failure: " + action, ex);
        }
    }

    private static Object getLink(String linkKindWithId) {
        if (linkKindWithId == null) return null;
        WeakReference<Object> reference = links.get(linkKindWithId);
        if (reference == null && !linkKindWithId.startsWith("t3_")) reference = links.get("t3_" + linkKindWithId);
        return reference == null ? null : reference.get();
    }

    private static String withoutKind(String linkKindWithId) {
        if (linkKindWithId == null) return null;
        int separator = linkKindWithId.indexOf('_');
        return separator < 0 ? linkKindWithId : linkKindWithId.substring(separator + 1);
    }

    /**
     * @return The value of the post identifier (PostId(value=...)), or null.
     */
    private static String postIdValue(Object identifier) {
        if (identifier == null) return null;
        try {
            for (Field field : identifier.getClass().getDeclaredFields()) {
                if (field.getType() == String.class && !Modifier.isStatic(field.getModifiers())) {
                    field.setAccessible(true);
                    return (String) field.get(identifier);
                }
            }
        } catch (Exception ex) {
            Logger.printException(() -> "postIdValue failure", ex);
        }
        return null;
    }

    private static void open(Activity activity, String url) {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
        intent.setPackage(activity.getPackageName());
        activity.startActivity(intent);
    }

    private static final Map<String, Constructor<?>> constructors = Collections.synchronizedMap(new LinkedHashMap<>());

    /**
     * Creates an event with its constructor of the given parameter count. Reddit keeps the names of the
     * event classes and of the enums they take.
     */
    private static Object create(String className, int parameterCount, Object... arguments) throws Exception {
        String key = className + "/" + parameterCount;
        Constructor<?> constructor = constructors.get(key);
        if (constructor == null) {
            Class<?> type = Class.forName(className, true, PostSwipe.class.getClassLoader());
            for (Constructor<?> candidate : type.getDeclaredConstructors()) {
                if (candidate.getParameterCount() == parameterCount && !candidate.isSynthetic()) {
                    constructor = candidate;
                    break;
                }
            }
            if (constructor == null) {
                throw new IllegalStateException("No constructor with " + parameterCount + " parameters in "
                        + className + ": " + Arrays.toString(type.getDeclaredConstructors()));
            }
            constructor.setAccessible(true);
            constructors.put(key, constructor);
        }
        return constructor.newInstance(arguments);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object enumValue(String className, String name) throws Exception {
        Class type = Class.forName(className, true, PostSwipe.class.getClassLoader());
        return Enum.valueOf(type, name);
    }

    private static Object invoke(Object target, String methodName) throws Exception {
        Method method = target.getClass().getMethod(methodName);
        return method.invoke(target);
    }

    // endregion
}
