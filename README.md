# pomee4 Patches

Extra patches for [Morphe](https://morphe.software), meant to be applied **together with** the official
[Morphe Patches](https://github.com/MorpheApp/morphe-patches). This bundle only contains the patches that
are not part of Morphe Patches.

## Add this repository to Morphe

In Morphe, add a patch source with the URL:

`https://github.com/pomee4/morphe-patches`

Then patch the app with both Morphe Patches and pomee4 Patches selected.

## Patches

<!-- PATCHES_START EXPANDED -->
> **[v3.0.0](https://github.com/pomee4/morphe-patches/releases/tag/v3.0.0)**&nbsp;&nbsp;&middot;&nbsp;&nbsp;`main`&nbsp;&nbsp;&middot;&nbsp;&nbsp;**1 patches** across **1 apps**&nbsp;&nbsp;&middot;&nbsp;&nbsp;[Full details](PATCHES.md)

| # | App | Patches | Version | Package |
|---|---|---|---|---|
| 1 | [**Reddit**](PATCHES.md#reddit-comredditfrontpage) | 1 | `experimental 2026.46.0`, `experimental 2026.45.0`, `experimental 2026.44.0`, `2026.40.0` | [`com.reddit.frontpage`](https://play.google.com/store/apps/details?id=com.reddit.frontpage) |
<!-- PATCHES_END -->

### Reddit: Comment swipe actions

Swipe a comment sideways to upvote, downvote, reply, save, share, collapse the thread, open the overflow
menu or open the author's profile. How far you swipe picks one of up to three actions.

**Settings:** long press the Reddit app icon and pick **Comment swipe settings**. There you can turn the
swipe actions on or off, pick the first, second and third action, the swipe direction and whether each
action vibrates.

The patch has its own extension and settings, so it does not depend on the Reddit patches of Morphe
Patches and keeps working when they change.

Supported Reddit versions: the same as Morphe Patches (2026.40.0, 2026.44.0 - 2026.46.0).

## Build

```bash
./gradlew :patches:buildAndroid
```

Building needs a GitHub token with the `read:packages` scope (`GITHUB_ACTOR` and `GITHUB_TOKEN`, or the
`gpr.user` and `gpr.key` Gradle properties), for the Morphe Gradle plugin. The bundle is written to
`patches/build/libs/patches-<version>.mpp`.

## License

GPLv3, see [LICENSE](LICENSE) and [NOTICE](NOTICE). The Comment swipe actions patch was first written in a
fork of Morphe Patches.
