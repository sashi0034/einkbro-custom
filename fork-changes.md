# Fork changes

Changes in `sashi0034/einkbro-custom` that are not in upstream
[`plateaukao/einkbro`](https://github.com/plateaukao/einkbro). Everything here is
aimed at day-to-day reading on an e-ink device.

Divergence point: upstream 16.5.0 (`5f295eb3`).

---

## 1. Toolbar icon spacing

The 46 dp icon slots used to abut edge to edge, which is easy to mis-tap on a
low-refresh e-ink panel. A single setting now inserts a gap between them.

- **Setting**: Settings → Toolbar → *Icon spacing* (dp, 0–100, default 0)
- **Preference key**: `sp_toolbar_icon_spacing`
- Applied with `Arrangement.spacedBy` in `view/compose/Toolbar.kt`, to the live
  bar and to the reorderable preview in the config screen, so the preview stays
  WYSIWYG.
- The Spacer/Title width maths subtracts the inserted gaps (`totalGapWidth`),
  otherwise a wide spacing pushes icons off the bar.

Files: `preference/UiConfig.kt`, `view/compose/Toolbar.kt`,
`activity/ToolbarConfigActivity.kt`, `setting/screens/ToolbarSettings.kt`,
`view/viewControllers/ComposeToolbarViewController.kt`.

## 2. Second toolbar (top + bottom)

Upstream supports one toolbar on one of four edges. This fork can show a second
one on the edge the first leaves free, with its own independently configured
icon list.

- **Settings**: Settings → Toolbar → *Show second toolbar* and *Second toolbar
  icons*
- **Preference keys**: `sp_second_toolbar_enabled`, `sp_second_toolbar_icons`,
  `sp_second_toolbar_icons_for_large`
- Default icon set: `ToolbarAction.defaultSecondActions`
  (Back, Forward, PageUp, PageDown, Bookmark, Search).
- The second bar is icon-only: the tab strip, the url input bar and the search
  panel all stay with the primary bar.
- `MainActivityLayout.setAppBarsVisibility()` is now the single writer for
  toolbar visibility, so fullscreen, the url input and the search panel hide and
  restore both bars together.
- `ChromeSetupDelegate.applyStatusbarConstraints()` was reworked to resolve
  "what owns the top edge / the bottom edge" before chaining the status bar and
  the content between them, instead of branching on the toolbar position. The
  six pre-existing arrangements are reproduced exactly; the two new ones fall
  out of the same rule.

**Limitation**: horizontal positions only. With a Left/Right (vertical) toolbar
the second bar stays hidden — a second 50 dp column would eat the page. The
EPUB reader also keeps a single bar, since it already swaps the whole toolbar
for its own action set.

Files: `view/MainActivityLayout.kt`, `unit/ViewUnit.kt`,
`view/viewControllers/ComposeToolbarViewController.kt`,
`activity/ToolbarConfigActivity.kt`, `setting/screens/ToolbarSettings.kt`,
`activity/delegates/{ChromeSetup,Fullscreen,InputBar,SearchPanel}Delegate.kt`,
`preference/UiConfig.kt`, `view/toolbaricons/ToolbarAction.kt`,
`res/values/ids.xml`.

## 3. Content margin, and a side-only reader margin

Reader mode had one CSS padding value for all four sides. A CSS page padding
scrolls away with the text, so it cannot keep a gap between the page and the
toolbar — the text reaches the bar as soon as you turn a page. The two axes are
now handled by different mechanisms, because they have different requirements.

**Vertical — a view inset, applied to every page**

The content area is inset from the top and the bottom, leaving a blank gutter
that stays put while the page scrolls. Top and bottom are independent, since
usually only one of the two edges carries a toolbar.

- **Setting**: Settings → Toolbar → *Content margin (top)* / *(bottom)*
  (dp, 0–200, default 0)
- **Preference keys**: `sp_content_margin_top`, `sp_content_margin_bottom`
- Implemented as margins on `MainContentLayout.swipeRefreshLayout` (switched to
  `MATCH_CONSTRAINT`, since `MATCH_PARENT` ignores them), applied by
  `ViewUnit.updateContentMargins()`. `MainContentLayout.root` now paints the
  theme background so the gutter reads as part of the page.
- The page-turn marker (§4) gets the same insets, so it keeps sharing a
  coordinate space with the web view it draws over.
- Not reader-mode specific: it is a property of the browser chrome, and applies
  to every page.

**Horizontal — still a reader-mode CSS padding**

Side margins do not scroll away under vertical scrolling, so they stay as
`body.mozac-readerview-body { padding: 0 Npx }`.

- **Setting**: long-press the reader-mode icon (or Settings → UI → Reader mode
  settings) → *Page margin (left/right)*
- **Preference key**: `sp_reader_padding_horizontal`
- **Migration**: falls back to the old all-sides `sp_padding_for_reader_mode`
  until written, so an upgrade keeps the side margin the user had.
- The two-column landscape `column-gap` follows this value.

Files: `preference/UiConfig.kt`, `preference/DisplayConfig.kt`,
`unit/ViewUnit.kt`, `view/MainContentLayout.kt`, `view/WebViewReaderHelper.kt`,
`view/dialog/compose/ReaderSettingsDialogFragment.kt`,
`setting/screens/ToolbarSettings.kt`, `activity/BrowserActivity.kt`.

## 4. Page-turn seam marker

A page turn overlaps the outgoing and incoming screens by the reserved offset,
so after a turn part of the old screen is still on display and it is easy to
re-read or skip a line. A dotted rule now marks where the previous screen ended.

- **Setting**: Settings → Gestures → *Page turn marker* (default on)
- **Preference key**: `sp_page_turn_marker`
- **Covers**: volume keys, touch areas / edge taps, and the toolbar
  PageUp/PageDown icons — every path goes through
  `WebViewNavigationHelper.pageUp/DownWithNoAnimation()`, so that is the only
  hook. Plain finger scrolling is deliberately not marked.
- The line stays until the next page turn replaces it (no fade-out timer, which
  on e-ink would cost a full-screen refresh for nothing) and is cleared on
  manual scrolling, pinch zoom, or navigation. Drag detection also covers inner
  CSS scroll containers, whose movement does not change the WebView's own
  scroll coordinates.
- Placement rule: a content point at view coordinate `v` before the turn is at
  `v - d` after it, where `d` is the signed scroll delta. So a forward turn
  leaves the old far edge at `size - d`, and a backward turn leaves the old near
  edge at `-d`. That one rule covers normal scrolling, vertical-rl (where
  forward means `d < 0`) and the two-column reader; the rule runs down the
  screen instead of across it whenever the page scrolls sideways.
- `assets/fix_scrolling.js`: `__einkbroPageScroll` now returns
  `"true:<fraction>"` instead of `"true"`, reporting how far an inner CSS scroll
  container actually moved as a fraction of its own height. Without this the
  marker never appears on pages that scroll an inner container (many SPAs),
  because the WebView's own `scrollY` does not change. A ratio is used rather
  than pixels so it survives the CSS-px to device-px conversion.

**Limitation**: `EpubReaderView` overrides both page methods without calling
through, so there is no marker in the EPUB reader.

Files: `view/PageTurnMarkerView.kt` (new), `view/WebViewNavigationHelper.kt`,
`view/EBWebView.kt`, `view/MainContentLayout.kt`, `assets/fix_scrolling.js`,
`preference/TouchConfig.kt`, `setting/screens/GestureSettings.kt`,
`activity/BrowserActivity.kt`, `res/values/ids.xml`.

## 5. Reader-mode toolbar icon reflects the mode

The `ReaderMode` toolbar icon was stateless, so nothing on the bar showed
whether reader mode was on. It now uses the existing `IconActiveInfo`
active/inactive mechanism: an outlined card for off, and the same silhouette
inverted for on — the only contrast that survives on a greyscale panel.

The truth source (`EBWebView.isReaderModeOn`) is a per-WebView field rather than
a preference, so it is pulled through an `isWebReaderMode` lambda and refreshed
explicitly from `BrowserActivity.toggleReaderMode()`. Page navigation resets the
flag and also flips the loading state, which already triggers a rebuild.

Files: `res/drawable/ic_reader_mode.xml` (new),
`res/drawable/ic_reader_mode_active.xml` (new),
`view/toolbaricons/ToolbarAction.kt`,
`view/viewControllers/ComposeToolbarViewController.kt`,
`activity/BrowserActivity.kt`.

## 6. Reader-mode footer link returns to the matching source position

Reader mode now repeats the original-page `link` after the extracted article.
Inside the app, that footer link exits reader mode and scrolls the restored page
to the original DOM element that produced the end of the extracted content,
instead of reopening the source at the top.

- Before Readability parses, temporary indexes are added only to cloned
  elements and map them to the untouched live source DOM. Only the selected
  article-end element is retained in the cached original body; temporary
  indexes never appear in reader content.
- JSON-LD-scoped extraction keeps the same mapping because the scope is cloned
  after the temporary indexes are attached.
- If no matching source element survives extraction, the restored page falls
  back to its document end. Outside the Android bridge (for example exported
  reader HTML), the footer remains a normal original-URL link.
- The existing top link and normal reader-mode exit behavior are unchanged.

Files: `assets/MozReadability.js`,
`assets/disable_reader_mode_at_source_end.js` (new),
`view/WebViewJsBridge.kt`, `view/WebViewReaderHelper.kt`,
`browser/JsWebInterface.kt`, `activity/BrowserActivity.kt`.

## 7. Distinct ReleaseAlt launcher name

The normal release keeps the launcher name **EinkBro Custom**, while the
side-by-side `.a` build is shown as **EinkBro Custom Alt**. This makes the two
otherwise identical custom icons distinguishable on the launcher and app-info
screens without changing either package ID or its saved data.

File: `src/releaseAlt/res/values/strings.xml` (new).

## 8. "Open favorite in new tab" toolbar icon

Upstream only offers *Open homepage* (`menu_openFav`, "お気に入り開く") from the
menu dialog, and it replaces the current tab. The same destination is now also
available as a toolbar icon that opens the favorite URL in a **new** tab, so it
can be reached in one tap without losing the page being read.

- **Toolbar action**: `ToolbarAction.OpenFavInNewTab` (home icon), added in
  Settings → Toolbar → toolbar icon configuration
- **Label**: `menu_openFav_newTab` — "Open homepage in new tab" /
  "新規タブでお気に入り開く"
- Dispatches `BrowserAction.AddNewTab(config.favoriteUrl)`, unlike the menu item
  which dispatches `UpdateAlbum` on the current tab.
- The enum entry is appended last: toolbar configs persist ordinals, so existing
  toolbar layouts are untouched.

Files: `view/toolbaricons/ToolbarAction.kt`,
`view/handlers/ToolbarActionHandler.kt`, `res/values*/strings.xml`.

## 9. Not listed as an external dictionary on BOOX

Onyx BOOX builds the "external app" list behind its dictionary/translation
popup in `com.onyx.android.sdk.data.utils.EditorUtils.loadSupportedActivities()`
(seen in `com.onyx.kreader`). It runs `queryIntentActivities` twice — once for
`android.intent.action.PROCESS_TEXT` + `text/plain`, once for
`colordict.intent.action.SEARCH` — merges both results and excludes only
`com.onyx.dict` and its own package. There is no BOOX-side way to remove an
entry, so declaring either filter put EinkBro in that picker permanently.

`DictActivity` therefore declares no intent-filter at all and is
`exported="false"`. Dropped:

- `colordict.intent.action.SEARCH` / `PICK_RESULT` (the ColorDict protocol)
- `android.intent.action.PROCESS_TEXT` — which also removes EinkBro from the
  system text-selection menu in other apps. Same registration, so the two
  cannot be separated.

`BrowserActivity`'s `SEND` + `text/plain` filter is untouched and is not part of
that query, so sharing text to EinkBro still works. `DictActivity.kt` itself is
unchanged; its `colordict.*` and `PROCESS_TEXT` branches are simply unreachable
now, which keeps this to one manifest hunk to revert.

File: `AndroidManifest.xml`.

## 10. Keep the default text-selection web search inside EinkBro

Android WebView/Chromium implements the standard selection menu's **Web
search** item by sending an implicit `android.intent.action.WEB_SEARCH` intent.
It does not target the host browser, so Android can resolve that intent to a
different installed browser or search app.

When **Show Default Selected Text Menu** is enabled, EinkBro now identifies the
WebView-owned web-search item and consumes its click before Chromium launches
that implicit intent. The selected text is sent through EinkBro's normal search
path instead, respecting the existing **external search in same tab** setting.
Copy, share, text-processing actions, and editable-field selection menus are
unchanged. Both Chromium's `select_action_menu_web_search` resource ID and the
older framework WebView `websearch` ID are recognized.

Files: `activity/delegates/ActionModeDelegate.kt`, `activity/BrowserActivity.kt`.

## 11. Scrollbar stays up after a page turn

The WebView scrollbar used to fade out ~250ms after every page turn. On e-ink
that fade is pure cost: the turn has already repainted the whole screen, so
drawing the bar in that frame is free and pinning it costs nothing until the
next turn, while the fade spends an animation erasing the position the reader
just asked for. Same reasoning as the seam line's missing fade-out timer in
`PageTurnMarkerView`. Continuous scrolling keeps the platform behaviour, where
the moving content already conveys progress and a persistent thumb only leaves
a trail down the edge.

- `WebViewNavigationHelper.reportPageTurn()` sets
  `isScrollbarFadingEnabled = false`. It is the join point of every turn path,
  so normal, vertical-read and two-column reading are covered at once.
- **Except** when `fix_scrolling.js` handled the turn inside the page (a CSS
  scrollable container under the viewport centre, i.e. many SPAs): the web
  view's own `scrollY` did not move, so a pinned bar would sit at the top
  telling a lie. Those paths pass `nativeScroll = false`, which restores fading.
- `EBWebView.clearPageTurnMarks()` takes down the seam line and the bar
  together — they share a lifetime, ending at a finger scroll, a pinch or a
  scale change. Setting `isScrollbarFadingEnabled = true` returns the platform
  default and hides the bar immediately, so no timer of our own is needed.
- `ViewUnit` wraps its two capture helpers in `withoutScrollbars {}`.
  `View.draw()` paints scrollbars, so a pinned bar would otherwise be baked into
  a saved screenshot and into what the image translator reads.

Not wired into the EPUB reader: `EpubReaderView` overrides
`pageDownWithNoAnimation()` and never goes through `WebViewNavigationHelper`.

There is deliberately no setting for this.

Files: `view/WebViewNavigationHelper.kt`, `view/EBWebView.kt`,
`browser/NinjaWebViewClient.kt`, `unit/ViewUnit.kt`.


---

## New preference keys

| Key | Type | Default |
| --- | --- | --- |
| `sp_toolbar_icon_spacing` | Int (dp) | `0` |
| `sp_second_toolbar_enabled` | Boolean | `false` |
| `sp_second_toolbar_icons` | String (ordinals) | `defaultSecondActions` |
| `sp_second_toolbar_icons_for_large` | String (ordinals) | `defaultSecondActions` |
| `sp_content_margin_top` | Int (dp) | `0` |
| `sp_content_margin_bottom` | Int (dp) | `0` |
| `sp_reader_padding_horizontal` | Int (CSS px) | old `sp_padding_for_reader_mode`, else `10` |
| `sp_page_turn_marker` | Boolean | `true` |

New strings are in `values/strings.xml` and `values-ja/strings.xml`; the other
locales fall back to the default resources.

## Building

The project needs JDK 17 or 21 (AGP 8.13.2 / Gradle 8.14.5). Note that JetBrains
runtimes do not ship `jlink`, which the `ad-filter` module's build requires, so a
JBR cannot be used even at a supported version — use a real JDK:

    JAVA_HOME=<a real JDK 17 or 21> ./gradlew assembleDebug
