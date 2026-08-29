package info.plateaukao.einkbro.view

import android.content.Context
import android.view.View
import android.widget.FrameLayout
import androidx.compose.ui.platform.ComposeView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import info.plateaukao.einkbro.R

class MainActivityLayout(
    val root: ConstraintLayout,
    val appBar: FrameLayout,
    val appBar2: FrameLayout,
    val composeIconBar: ComposeView,
    val composeIconBar2: ComposeView,
    val mainSearchPanel: ComposeView,
    val twoPanelLayout: TwoPaneLayout,
    val activityMainContent: MainContentLayout,
    val inputUrl: ComposeView,
    val contentSeparator: View,
    val contentSeparator2: View,
    val layoutOverview: ComposeView,
    val statusBar: ComposeView,
    val sideTabBar: ComposeView,
) {
    /**
     * Whether the second toolbar is currently part of the layout. Owned by
     * ViewUnit.updateAppbarPosition, which is the only place that decides it;
     * [setAppBarsVisibility] reads it so callers don't each need the config.
     */
    var secondAppBarEnabled: Boolean = false

    /**
     * Shows or hides the whole toolbar area at once. Callers that hide the chrome
     * (fullscreen, the url input, the search panel) mean "the toolbars", not "the
     * first toolbar" — and the second bar has to stay gone while it is switched off.
     */
    fun setAppBarsVisibility(visibility: Int) {
        appBar.visibility = visibility
        contentSeparator.visibility = visibility
        val secondVisibility = if (secondAppBarEnabled) visibility else View.GONE
        appBar2.visibility = secondVisibility
        contentSeparator2.visibility = secondVisibility
    }

    companion object {
        fun create(context: Context): MainActivityLayout {
            val root = ConstraintLayout(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.MATCH_PARENT
                )
                fitsSystemWindows = false
            }

            // appBar FrameLayout
            val appBar = FrameLayout(context).apply {
                id = R.id.appBar
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
            }

            // ComposeView for icon bar
            val composeIconBar = ComposeView(context).apply {
                id = R.id.compose_icon_bar
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            appBar.addView(composeIconBar)

            // ComposeView for search panel
            val mainSearchPanel = ComposeView(context).apply {
                id = R.id.main_search_panel
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            appBar.addView(mainSearchPanel)

            root.addView(appBar)

            // Optional second bar, on the edge opposite the first one. GONE until
            // ViewUnit.updateAppbarPosition decides it applies; the URL input and
            // the search panel stay with the primary bar.
            val appBar2 = FrameLayout(context).apply {
                id = R.id.app_bar_2
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT
                )
                visibility = View.GONE
            }
            val composeIconBar2 = ComposeView(context).apply {
                id = R.id.compose_icon_bar_2
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
                )
            }
            appBar2.addView(composeIconBar2)
            root.addView(appBar2)

            // TwoPaneLayout
            val twoPanelLayout = TwoPaneLayout(context).apply {
                id = R.id.two_panel_layout
                layoutParams = ConstraintLayout.LayoutParams(0, 0)
            }

            // Create main content and add it to TwoPaneLayout
            val mainContentLayout = MainContentLayout.create(context)
            mainContentLayout.root.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            twoPanelLayout.addView(mainContentLayout.root)

            root.addView(twoPanelLayout)

            // inputUrl ComposeView
            val inputUrl = ComposeView(context).apply {
                id = R.id.input_url
                layoutParams = ConstraintLayout.LayoutParams(0, 0)
                visibility = View.INVISIBLE
            }
            root.addView(inputUrl)

            // contentSeparator View
            val contentSeparator = View(context).apply {
                id = R.id.content_separator
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT, 1
                )
                setBackgroundColor(android.graphics.Color.DKGRAY)
            }
            root.addView(contentSeparator)

            val contentSeparator2 = View(context).apply {
                id = R.id.content_separator_2
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT, 1
                )
                setBackgroundColor(android.graphics.Color.DKGRAY)
                visibility = View.GONE
            }
            root.addView(contentSeparator2)

            // layoutOverview ComposeView
            val layoutOverview = ComposeView(context).apply {
                id = R.id.layout_overview
                layoutParams = ConstraintLayout.LayoutParams(0, 0)
                visibility = View.INVISIBLE
            }
            root.addView(layoutOverview)

            // statusBar ComposeView (shown when toolbar is hidden)
            val statusBar = ComposeView(context).apply {
                id = R.id.status_bar
                layoutParams = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                )
                visibility = View.GONE
            }
            root.addView(statusBar)

            // sideTabBar ComposeView: the tab strip for vertical toolbar mode, where
            // the 50dp toolbar column has no room for it. Positioned at runtime by
            // ViewUnit.updateAppbarPosition; GONE for top/bottom toolbars, which keep
            // the strip inside the app bar itself.
            val sideTabBar = ComposeView(context).apply {
                id = R.id.side_tab_bar
                layoutParams = ConstraintLayout.LayoutParams(
                    0,
                    ConstraintLayout.LayoutParams.WRAP_CONTENT,
                )
                visibility = View.GONE
            }
            root.addView(sideTabBar)

            // Apply constraints
            val constraintSet = ConstraintSet()
            constraintSet.clone(root)

            // appBar: bottom to parent bottom
            constraintSet.connect(appBar.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)

            // appBar2: opposite edge by default; re-pinned at runtime.
            constraintSet.connect(appBar2.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(appBar2.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(appBar2.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(contentSeparator2.id, ConstraintSet.TOP, appBar2.id, ConstraintSet.BOTTOM)

            // twoPanelLayout: start/end to parent, top to parent, bottom to appBar top
            constraintSet.connect(twoPanelLayout.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(twoPanelLayout.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(twoPanelLayout.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(twoPanelLayout.id, ConstraintSet.BOTTOM, appBar.id, ConstraintSet.TOP)

            // inputUrl: all edges to parent
            constraintSet.connect(inputUrl.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(inputUrl.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.connect(inputUrl.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(inputUrl.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

            // contentSeparator: bottom to appBar top
            constraintSet.connect(contentSeparator.id, ConstraintSet.BOTTOM, appBar.id, ConstraintSet.TOP)

            // layoutOverview: all edges to parent
            constraintSet.connect(layoutOverview.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(layoutOverview.id, ConstraintSet.BOTTOM, ConstraintSet.PARENT_ID, ConstraintSet.BOTTOM)
            constraintSet.connect(layoutOverview.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(layoutOverview.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

            // statusBar: full-width; anchored to top by default (position is applied at runtime)
            constraintSet.connect(statusBar.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(statusBar.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)
            constraintSet.connect(statusBar.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)

            // sideTabBar: anchored to the top edge; the start/end sides are attached to
            // the app bar at runtime, since which side it clears depends on the toolbar.
            constraintSet.connect(sideTabBar.id, ConstraintSet.TOP, ConstraintSet.PARENT_ID, ConstraintSet.TOP)
            constraintSet.connect(sideTabBar.id, ConstraintSet.START, ConstraintSet.PARENT_ID, ConstraintSet.START)
            constraintSet.connect(sideTabBar.id, ConstraintSet.END, ConstraintSet.PARENT_ID, ConstraintSet.END)

            constraintSet.applyTo(root)

            return MainActivityLayout(
                root = root,
                appBar = appBar,
                appBar2 = appBar2,
                composeIconBar = composeIconBar,
                composeIconBar2 = composeIconBar2,
                mainSearchPanel = mainSearchPanel,
                twoPanelLayout = twoPanelLayout,
                activityMainContent = mainContentLayout,
                inputUrl = inputUrl,
                contentSeparator = contentSeparator,
                contentSeparator2 = contentSeparator2,
                layoutOverview = layoutOverview,
                statusBar = statusBar,
                sideTabBar = sideTabBar,
            )
        }
    }
}
