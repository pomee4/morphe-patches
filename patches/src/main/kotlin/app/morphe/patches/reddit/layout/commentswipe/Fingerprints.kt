/*
 * Copyright 2026 Morphe.
 * https://github.com/MorpheApp/morphe-patches
 *
 * See the included NOTICE file for GPLv3 Section 7 terms that apply to Morphe contributions.
 */

package app.morphe.patches.reddit.layout.commentswipe

import app.morphe.patcher.Fingerprint
import app.morphe.patcher.StringComparisonType
import app.morphe.patcher.fieldAccess
import app.morphe.patcher.newInstance
import app.morphe.patcher.string
import com.android.tools.smali.dexlib2.Opcode

private const val COMMENT_COMPOSABLES = "Lcom/reddit/comments/elements/usercomment/composables/"

private const val COMMENT_PRESS_LAMBDA =
    "${COMMENT_COMPOSABLES}UserCommentElement\$commentGestureHandling\$1\$3;"

/**
 * The comment row's PointerInputEventHandler. Reddit detects taps, long presses and double taps on
 * comments in it, and creates the press callback below.
 */
internal object CommentGestureHandlerFingerprint : Fingerprint(
    definingClass = COMMENT_COMPOSABLES,
    name = "invoke",
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L", "L"),
    interfaces = listOf("Landroidx/compose/ui/input/pointer/PointerInputEventHandler;"),
    filters = listOf(
        newInstance(COMMENT_PRESS_LAMBDA)
    )
)

/**
 * The press callback of the comment row, called when the finger goes down with its position.
 */
internal object CommentPressFingerprint : Fingerprint(
    definingClass = COMMENT_PRESS_LAMBDA,
    returnType = "Ljava/lang/Object;",
    parameters = listOf("L", "J", "L")
)

/**
 * The main constructor of Comment, the one that sets every field.
 */
internal object CommentConstructorFingerprint : Fingerprint(
    definingClass = "Lcom/reddit/domain/model/Comment;",
    name = "<init>",
    filters = listOf(
        fieldAccess(
            opcode = Opcode.IPUT_BOOLEAN,
            definingClass = "this",
            name = "saved"
        )
    )
)

/**
 * Comment events, by the start of their toString().
 */
internal class CommentEventFingerprint(toStringStart: String) : Fingerprint(
    name = "toString",
    returnType = "Ljava/lang/String;",
    parameters = listOf(),
    filters = listOf(
        string(toStringStart, StringComparisonType.STARTS_WITH)
    )
)

internal val VoteEventFingerprint = CommentEventFingerprint("OnClickVoteEvent(")
internal val ReplyEventFingerprint = CommentEventFingerprint("OnClickCommentReplyEvent(")
internal val SaveEventFingerprint = CommentEventFingerprint("OnClickSaveEvent(")
internal val UnsaveEventFingerprint = CommentEventFingerprint("OnClickUnsaveEvent(")
internal val ShareEventFingerprint = CommentEventFingerprint("OnClickShareEvent(")
internal val CollapseThreadEventFingerprint = CommentEventFingerprint("OnClickCollapseThreadEvent(")
internal val OverflowMenuEventFingerprint = CommentEventFingerprint("OnClickOverflowMenuEvent(")
