package info.plateaukao.einkbro.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.view.View

/**
 * Marks where the previous screen ended after a page turn.
 *
 * Page turns overlap the outgoing and incoming screens by a configurable
 * reserved offset, so after a turn part of the old screen is still visible and
 * it is easy to re-read or skip a line. This draws a dotted rule at the seam:
 * the position the old screen edge now occupies.
 *
 * The line stays until the next page turn replaces it (or the page navigates
 * away and [clear] is called) — deliberately no fade-out timer, which on e-ink
 * would cost a full-screen refresh for no information.
 *
 * Like [TouchAreaHintView], the stroke is a dark core over a light halo so it
 * reads on both light and dark pages.
 */
class PageTurnMarkerView(context: Context) : View(context) {
    /** Which way the rule runs; [Horizontal] pairs with vertical scrolling. */
    enum class Axis { Horizontal, Vertical }

    companion object {
        private const val CORE_WIDTH_DP = 1f
        private const val HALO_WIDTH_DP = 3f
        private const val DASH_ON_DP = 4f
        private const val DASH_OFF_DP = 4f
        private const val CORE_COLOR = 0xC0000000.toInt()
        private const val HALO_COLOR = 0xC0FFFFFF.toInt()
    }

    private val density = resources.displayMetrics.density

    private val corePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = CORE_WIDTH_DP * density
        color = CORE_COLOR
        pathEffect = DashPathEffect(floatArrayOf(DASH_ON_DP * density, DASH_OFF_DP * density), 0f)
    }
    private val haloPaint = Paint(corePaint).apply {
        strokeWidth = HALO_WIDTH_DP * density
        color = HALO_COLOR
    }

    private var axis = Axis.Horizontal
    private var offset = -1f

    /** Draws the rule at [offsetPx] from the top (Horizontal) or left (Vertical). */
    fun show(axis: Axis, offsetPx: Float) {
        // A seam right on a screen edge carries no information — the edge is
        // already the boundary — and would just add clutter.
        val inset = haloPaint.strokeWidth
        val limit = if (axis == Axis.Horizontal) height else width
        if (offsetPx <= inset || (limit > 0 && offsetPx >= limit - inset)) {
            clear()
            return
        }
        if (this.axis == axis && offset == offsetPx) return
        this.axis = axis
        offset = offsetPx
        invalidate()
    }

    fun clear() {
        if (offset < 0f) return
        offset = -1f
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (offset < 0f) return
        if (axis == Axis.Horizontal) {
            canvas.drawLine(0f, offset, width.toFloat(), offset, haloPaint)
            canvas.drawLine(0f, offset, width.toFloat(), offset, corePaint)
        } else {
            canvas.drawLine(offset, 0f, offset, height.toFloat(), haloPaint)
            canvas.drawLine(offset, 0f, offset, height.toFloat(), corePaint)
        }
    }
}
