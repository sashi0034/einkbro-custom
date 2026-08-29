package info.plateaukao.einkbro.preference

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Point
import androidx.core.content.edit
import info.plateaukao.einkbro.unit.ViewUnit
import info.plateaukao.einkbro.view.statusbar.StatusbarItem
import info.plateaukao.einkbro.view.statusbar.StatusbarPosition
import info.plateaukao.einkbro.view.toolbaricons.ToolbarAction

class UiConfig(private val context: Context, private val sp: SharedPreferences) {

    var toolbarPosition: ToolbarPosition
        get() {
            val posOrdinal = sp.getInt(K_TOOLBAR_POSITION, -1)
            if (posOrdinal >= 0) return ToolbarPosition.entries.getOrElse(posOrdinal) { ToolbarPosition.Bottom }
            // migrate from old boolean preference
            return if (sp.getBoolean(K_TOOLBAR_TOP, false)) ToolbarPosition.Top else ToolbarPosition.Bottom
        }
        set(value) = sp.edit { putInt(K_TOOLBAR_POSITION, value.ordinal) }

    var isToolbarOnTop: Boolean
        get() = toolbarPosition == ToolbarPosition.Top
        set(value) { toolbarPosition = if (value) ToolbarPosition.Top else ToolbarPosition.Bottom }

    val isVerticalToolbar: Boolean
        get() = toolbarPosition == ToolbarPosition.Left || toolbarPosition == ToolbarPosition.Right

    /**
     * Extra gap in dp inserted between toolbar icons. The 46dp icon slots abut by
     * default, which is hard to hit accurately on a low-refresh e-ink panel.
     * Clamped on read so a bad hand-edited value can't push every icon off the bar.
     */
    var toolbarIconSpacing: Int
        get() = sp.getInt(K_TOOLBAR_ICON_SPACING, 0).coerceIn(0, MAX_TOOLBAR_ICON_SPACING)
        set(value) = sp.edit {
            putInt(K_TOOLBAR_ICON_SPACING, value.coerceIn(0, MAX_TOOLBAR_ICON_SPACING))
        }

    /**
     * Blank space kept between the web content and the top / bottom edge of the
     * content area, in dp. Unlike a CSS page margin this is a view inset, so it
     * stays put while the page scrolls — which is the point: it keeps text from
     * running right up against the toolbar. Top and bottom are independent
     * because only one of the two edges usually carries a toolbar.
     */
    var contentMarginTop: Int
        get() = sp.getInt(K_CONTENT_MARGIN_TOP, 0).coerceIn(0, MAX_CONTENT_MARGIN)
        set(value) = sp.edit { putInt(K_CONTENT_MARGIN_TOP, value.coerceIn(0, MAX_CONTENT_MARGIN)) }

    var contentMarginBottom: Int
        get() = sp.getInt(K_CONTENT_MARGIN_BOTTOM, 0).coerceIn(0, MAX_CONTENT_MARGIN)
        set(value) = sp.edit { putInt(K_CONTENT_MARGIN_BOTTOM, value.coerceIn(0, MAX_CONTENT_MARGIN)) }

    var statusbarEnabled by BooleanPreference(sp, K_STATUSBAR_ENABLED, false)

    var statusbarPosition: StatusbarPosition
        get() = StatusbarPosition.entries.getOrElse(sp.getInt(K_STATUSBAR_POSITION, 0)) { StatusbarPosition.Top }
        set(value) = sp.edit { putInt(K_STATUSBAR_POSITION, value.ordinal) }

    var statusbarItems: List<StatusbarItem>
        get() {
            val raw = sp.getString(K_STATUSBAR_ITEMS, null)
            if (raw.isNullOrBlank()) return StatusbarItem.defaultItems
            return raw.split(",").mapNotNull { token ->
                token.toIntOrNull()?.let { StatusbarItem.fromOrdinal(it) }
            }
        }
        set(value) = sp.edit {
            putString(K_STATUSBAR_ITEMS, value.joinToString(",") { it.ordinal.toString() })
        }

    var shouldHideToolbar by BooleanPreference(sp, K_HIDE_TOOLBAR, false)
    var showToolbarFirst by BooleanPreference(sp, K_SHOW_TOOLBAR_FIRST, true)
    var hideStatusbar by BooleanPreference(sp, K_HIDE_STATUSBAR, false)
    var keepAwake by BooleanPreference(sp, K_KEEP_AWAKE, false)
    var isBookmarkGridView by BooleanPreference(sp, K_BOOKMARK_GRID_VIEW, false)
    var showHistoryThumbnailGrid by BooleanPreference(sp, K_HISTORY_THUMBNAIL_GRID, false)
    var showShareSaveMenu by BooleanPreference(sp, K_SHOW_SHARE_SAVE_MENU, false)
    var showContentMenu by BooleanPreference(sp, K_SHOW_CONTENT_MENU, false)
    var showDefaultActionMenu by BooleanPreference(sp, K_SHOW_DEFAULT_ACTION_MENU, false)
    var showActionMenuIcons by BooleanPreference(sp, K_SHOW_ACTION_MENU_ICONS, true)

    var hiddenMenuItems: Set<String>
        get() = sp.getStringSet(K_HIDDEN_MENU_ITEMS, emptySet())?.toSet() ?: emptySet()
        // Defensive copy: SharedPreferences takes ownership of the Set passed to
        // putStringSet and the caller must not mutate it afterward.
        set(value) = sp.edit { putStringSet(K_HIDDEN_MENU_ITEMS, value.toSet()) }

    // Flat ordering of menu item names. Empty → use defaults.
    var menuItemOrder: List<String>
        get() = sp.getString(K_MENU_ITEM_ORDER, "").orEmpty()
            .split(",").filter { it.isNotBlank() }
        set(value) = sp.edit { putString(K_MENU_ITEM_ORDER, value.joinToString(",")) }

    var fabPosition: FabPosition
        get() = FabPosition.entries[sp.getString(K_NAV_POSITION, "0")?.toInt() ?: 0]
        set(value) {
            sp.edit { putString(K_NAV_POSITION, value.ordinal.toString()) }
        }

    var fabCustomPosition: Point
        get() {
            val str = sp.getString(K_FAB_POSITION, "").orEmpty()
            return if (str.isBlank()) Point(0, 0)
            else Point(str.split(",").first().toInt(), str.split(",").last().toInt())
        }
        set(value) {
            sp.edit { putString(K_FAB_POSITION, "${value.x},${value.y}") }
        }

    var fabCustomPositionLandscape: Point
        get() {
            val str = sp.getString(K_FAB_POSITION_LAND, "").orEmpty()
            return if (str.isBlank()) Point(0, 0)
            else Point(str.split(",").first().toInt(), str.split(",").last().toInt())
        }
        set(value) {
            sp.edit { putString(K_FAB_POSITION_LAND, "${value.x},${value.y}") }
        }

    val shouldUseLargeToolbarConfig: Boolean
        get() = if (isVerticalToolbar) !ViewUnit.isLandscape(context) else ViewUnit.isLandscape(context)

    var toolbarActions: List<ToolbarAction>
        get() {
            val key =
                if (shouldUseLargeToolbarConfig) K_TOOLBAR_ICONS_FOR_LARGE else K_TOOLBAR_ICONS
            val iconListString =
                sp.getString(key, sp.getString(K_TOOLBAR_ICONS, getDefaultIconStrings())).orEmpty()
            return iconStringToEnumList(iconListString)
        }
        set(value) {
            sp.edit {
                val key =
                    if (shouldUseLargeToolbarConfig) K_TOOLBAR_ICONS_FOR_LARGE else K_TOOLBAR_ICONS
                putString(key, value.map { it.ordinal }.joinToString(","))
            }
        }

    /**
     * Whether a second toolbar is shown on the edge opposite [toolbarPosition].
     * Only meaningful for the horizontal positions: the vertical toolbar already
     * owns a whole screen edge, and a second column would eat the page.
     */
    var secondToolbarEnabled by BooleanPreference(sp, K_SECOND_TOOLBAR_ENABLED, false)

    /** True when a second bar should actually be laid out right now. */
    val hasSecondToolbar: Boolean
        get() = secondToolbarEnabled && !isVerticalToolbar

    /** The edge the second bar occupies: always the one the first bar is not on. */
    val secondToolbarPosition: ToolbarPosition
        get() = if (toolbarPosition == ToolbarPosition.Top) ToolbarPosition.Bottom
        else ToolbarPosition.Top

    var secondToolbarActions: List<ToolbarAction>
        get() {
            val key =
                if (shouldUseLargeToolbarConfig) K_SECOND_TOOLBAR_ICONS_FOR_LARGE
                else K_SECOND_TOOLBAR_ICONS
            val iconListString = sp.getString(key, sp.getString(K_SECOND_TOOLBAR_ICONS, null))
            if (iconListString == null) return ToolbarAction.defaultSecondActions
            return iconStringToEnumList(iconListString)
        }
        set(value) {
            sp.edit {
                val key =
                    if (shouldUseLargeToolbarConfig) K_SECOND_TOOLBAR_ICONS_FOR_LARGE
                    else K_SECOND_TOOLBAR_ICONS
                putString(key, value.map { it.ordinal }.joinToString(","))
            }
        }

    var readerToolbarActions: List<ToolbarAction>
        get() {
            val iconListString =
                sp.getString(K_READER_TOOLBAR_ICONS, "").orEmpty()
            if (iconListString.isBlank()) return ToolbarAction.defaultReaderActions
            return iconStringToEnumList(iconListString)
        }
        set(value) {
            sp.edit {
                putString(K_READER_TOOLBAR_ICONS, value.map { it.ordinal }.joinToString(","))
            }
        }

    private fun iconStringToEnumList(iconListString: String): List<ToolbarAction> {
        if (iconListString.isBlank()) return listOf()
        return iconListString.split(",").map { ToolbarAction.fromOrdinal(it.toInt()) }
    }

    private fun getDefaultIconStrings(): String =
        if (ViewUnit.isWideLayout(context)) {
            ToolbarAction.defaultActions.joinToString(",") { action ->
                action.ordinal.toString()
            }
        } else {
            ToolbarAction.defaultActionsForPhone.joinToString(",") { action ->
                action.ordinal.toString()
            }
        }

    companion object {
        const val K_TOOLBAR_POSITION = "sp_toolbar_position"
        const val K_TOOLBAR_TOP = "sp_toolbar_top"
        const val K_HIDE_TOOLBAR = "hideToolbar"
        const val K_SHOW_TOOLBAR_FIRST = "sp_toolbarShow"
        const val K_HIDE_STATUSBAR = "sp_hide_statusbar"
        const val K_KEEP_AWAKE = "sp_screen_awake"
        const val K_BOOKMARK_GRID_VIEW = "sp_bookmark_grid_view"
        const val K_HISTORY_THUMBNAIL_GRID = "sp_history_thumbnail_grid"
        const val K_SHOW_SHARE_SAVE_MENU = "sp_show_share_save_menu"
        const val K_SHOW_CONTENT_MENU = "sp_show_content_menu"
        const val K_SHOW_DEFAULT_ACTION_MENU = "sp_show_default_action_menu"
        const val K_SHOW_ACTION_MENU_ICONS = "sp_show_action_menu_icons"
        const val K_NAV_POSITION = "nav_position"
        const val K_FAB_POSITION = "sp_fab_position"
        const val K_FAB_POSITION_LAND = "sp_fab_position_land"
        const val K_TOOLBAR_ICONS = "sp_toolbar_icons"
        const val K_TOOLBAR_ICONS_FOR_LARGE = "sp_toolbar_icons_for_large"
        const val K_READER_TOOLBAR_ICONS = "sp_reader_toolbar_icons"
        const val K_TOOLBAR_ICON_SPACING = "sp_toolbar_icon_spacing"
        const val K_SECOND_TOOLBAR_ENABLED = "sp_second_toolbar_enabled"
        const val K_SECOND_TOOLBAR_ICONS = "sp_second_toolbar_icons"
        const val K_SECOND_TOOLBAR_ICONS_FOR_LARGE = "sp_second_toolbar_icons_for_large"
        const val MAX_TOOLBAR_ICON_SPACING = 100
        const val K_CONTENT_MARGIN_TOP = "sp_content_margin_top"
        const val K_CONTENT_MARGIN_BOTTOM = "sp_content_margin_bottom"
        const val MAX_CONTENT_MARGIN = 200
        const val K_STATUSBAR_ENABLED = "sp_statusbar_enabled"
        const val K_STATUSBAR_POSITION = "sp_statusbar_position"
        const val K_STATUSBAR_ITEMS = "sp_statusbar_items"
        const val K_HIDDEN_MENU_ITEMS = "sp_hidden_menu_items"
        const val K_MENU_ITEM_ORDER = "sp_menu_item_order"
    }
}
