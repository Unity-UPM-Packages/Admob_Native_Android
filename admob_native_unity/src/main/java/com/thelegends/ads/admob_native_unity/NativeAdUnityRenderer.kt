package com.thelegends.ads.admob_native_unity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.NinePatch
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.NinePatchDrawable
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.util.LruCache
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.nativead.MediaView
import org.json.JSONObject
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.Executors
import kotlin.math.abs


/**
 * Memory-efficient asset cache using LruCache for ad textures and 9-slice sprites.
 * Manages rapid decoding and background loading of PNG assets cached from Unity.
 */
object NativeAdImageCache {
    // Allocation: 1/8th of total VM heap for texture storage
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // KB

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024 // Size in KB
        }
    }

    private val executor = Executors.newFixedThreadPool(4)
    private val uiHandler = Handler(Looper.getMainLooper())

    /**
     * Decodes a bitmap from an absolute file path and updates the target ImageView.
     */
    fun loadImage(imageView: ImageView, path: String) {
        val cachedBitmap = memoryCache.get(path)
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap)
            return
        }

        // Decode from disk in background to avoid dropping frames on the main UI thread
        executor.execute {
            try {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    memoryCache.put(path, bitmap)
                    uiHandler.post {
                        imageView.setImageBitmap(bitmap)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Renders an image with optional NinePatch (9-slice) metadata.
     * Logic: If 'border' metadata exists, processes the sprite as a NinePatchDrawable; 
     * otherwise, displays it as a standard stretched bitmap.
     */
    fun displayImage(context: Context, imageView: ImageView, path: String, border: JSONObject?, screenH: Float, refH: Float) {
        if (border == null) {
            loadImage(imageView, path)
            return
        }

        val cachedBitmap = memoryCache.get(path)
        if (cachedBitmap != null) {
            applyNinePatch(context, imageView, cachedBitmap, border, screenH, refH)
            return
        }

        executor.execute {
            try {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    memoryCache.put(path, bitmap)
                    uiHandler.post { applyNinePatch(context, imageView, bitmap, border, screenH, refH) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Constructs a NinePatchDrawable from a Bitmap using target 9-slice pixel borders.
     * Scales borders from Unity's original sprite coordinates to the actual Android screen density.
     */
    private fun applyNinePatch(context: Context, imageView: ImageView, bitmap: Bitmap, border: JSONObject, screenH: Float, refH: Float) {
        val ppum = border.optDouble("ppuMultiplier", 1.0).toFloat().takeIf { it > 0f } ?: 1f
        // Scale = (1 / ppuMultiplier) * (screenHeight / referenceHeight)
        val targetScale = (1f / ppum) * (screenH / refH)

        val oLeft   = border.optDouble("left",   0.0).toFloat()
        val oBottom = border.optDouble("bottom", 0.0).toFloat()
        val oRight  = border.optDouble("right",  0.0).toFloat()
        val oTop    = border.optDouble("top",    0.0).toFloat()

        if (oLeft == 0f && oBottom == 0f && oRight == 0f && oTop == 0f) {
            imageView.setImageBitmap(bitmap)
            return
        }

        val scaledBw = (bitmap.width * targetScale).toInt().coerceAtLeast(1)
        val scaledBh = (bitmap.height * targetScale).toInt().coerceAtLeast(1)

        val scaledBitmap = try {
            if (abs(targetScale - 1f) < 0.01f) bitmap
            else Bitmap.createScaledBitmap(bitmap, scaledBw, scaledBh, true)
        } catch (e: Exception) { bitmap }

        val left   = (oLeft   * targetScale).toInt()
        val right  = (oRight  * targetScale).toInt()
        val top    = (oTop    * targetScale).toInt()
        val bottom = (oBottom * targetScale).toInt()

        val bw = scaledBitmap.width
        val bh = scaledBitmap.height

        try {
            // Cut points for Nine-patch: X (Left->Right) and Y (Top->Bottom)
            val xDivs = intArrayOf(left, bw - right)
            val yDivs = intArrayOf(top, bh - bottom)

            if (xDivs[0] >= xDivs[1] || yDivs[0] >= yDivs[1]) {
                imageView.setImageBitmap(scaledBitmap)
                return
            }

            val numColors = 9
            val NO_COLOR  = 0x00000001.toInt()
            val colors    = IntArray(numColors) { NO_COLOR }

            // Standard Android Binary Chunk Structure (32 bytes header + data)
            val buffer = ByteBuffer
                .allocate(32 + (xDivs.size + yDivs.size + colors.size) * 4)
                .order(ByteOrder.nativeOrder())

            buffer.put(1.toByte())
            buffer.put(xDivs.size.toByte())
            buffer.put(yDivs.size.toByte())
            buffer.put(numColors.toByte())
            buffer.putInt(0)
            buffer.putInt(0)
            buffer.putInt(0); buffer.putInt(0); buffer.putInt(0); buffer.putInt(0)
            buffer.putInt(0) // Reserved

            for (v in xDivs) buffer.putInt(v)
            for (v in yDivs) buffer.putInt(v)
            for (c in colors) buffer.putInt(c)

            val chunkBytes = buffer.array()
            
            if (NinePatch.isNinePatchChunk(chunkBytes)) {
                scaledBitmap.density = context.resources.displayMetrics.densityDpi
                val drawable = NinePatchDrawable(context.resources, scaledBitmap, chunkBytes,
                    Rect(), null)
                
                imageView.setImageDrawable(null)
                imageView.background = drawable
            } else {
                imageView.setImageBitmap(scaledBitmap)
            }
        } catch (e: Exception) {
            imageView.setImageBitmap(scaledBitmap)
        }
    }
}

/**
 * Data container for normalized element coordinates (transformed from Unity Screen Space).
 */
class NormBounds(
    val xMin: Float, 
    val yMin: Float, 
    val xMax: Float, 
    val yMax: Float,
    val originalFontSize: Float? = null // Backup font size for DPI-aware scaling
)

/**
 * Core engine responsible for rendering AdMob Native Ads using Unity-exported blueprints.
 * Orthogonally transforms JSON data into functional Android View Hierarchies.
 */
class NativeAdUnityRenderer(context: Context) : FrameLayout(context) {

    var jsonPayload: String? = null
    var referenceWidth: Float = 1080f
    var referenceHeight: Float = 1920f

    private var screenWidth: Float = 1080f
    private var screenHeight: Float = 1920f

    private var rootPixelRect = Rect(0, 0, 0, 0)

    fun getRootPixelRect(): Rect = rootPixelRect

    // Mapping generated Android Views for AdMob Registration (Headline, Body, CTA, etc.)
    val registeredViews = HashMap<String, View>()

    init {
        clipChildren = false
        clipToPadding = false
    }

    /**
     * Converts Unity RGBA HTML color (#RRGGBBAA) string to Android (#AARRGGBB) integer.
     */
    private fun parseUnityColor(rgbaHtml: String): Int {
        try {
            var str = rgbaHtml.removePrefix("#")
            if (str.length == 8) {
                val rrggbb = str.substring(0, 6)
                val aa = str.substring(6, 8)
                str = aa + rrggbb
            }
            return Color.parseColor("#$str")
        } catch (e: Exception) {
            return Color.TRANSPARENT
        }
    }

    private fun parseGravity(alignment: String?): Int {
        return when(alignment) {
            "UpperLeft" -> Gravity.TOP or Gravity.START
            "UpperCenter" -> Gravity.TOP or Gravity.CENTER_HORIZONTAL
            "UpperRight" -> Gravity.TOP or Gravity.END
            "MiddleLeft" -> Gravity.CENTER_VERTICAL or Gravity.START
            "MiddleCenter" -> Gravity.CENTER
            "MiddleRight" -> Gravity.CENTER_VERTICAL or Gravity.END
            "LowerLeft" -> Gravity.BOTTOM or Gravity.START
            "LowerCenter" -> Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
            "LowerRight" -> Gravity.BOTTOM or Gravity.END
            else -> Gravity.CENTER
        }
    }

    private fun applyTypeface(tv: TextView, isBold: Boolean, isItalic: Boolean) {
        if (isBold && isItalic) tv.setTypeface(null, Typeface.BOLD_ITALIC)
        else if (isBold) tv.setTypeface(null, Typeface.BOLD)
        else if (isItalic) tv.setTypeface(null, Typeface.ITALIC)
    }

    /**
     * The Main Orchestrator: Inflates a full Android Ad UI from a JSON Blueprint.
     */
    fun buildFromJson(jsonString: String) {
        this.jsonPayload = jsonString
        this.removeAllViews()
        this.registeredViews.clear()

        try {
            val root = JSONObject(jsonString)
            referenceWidth = root.optDouble("referenceWidth", 1080.0).toFloat()
            referenceHeight = root.optDouble("referenceHeight", 1920.0).toFloat()

            val elements = root.optJSONArray("elements") ?: return

            // PASS 1: Locate the RootAdView to define the scaling boundary on the physical device screen
            val metrics = context.resources.displayMetrics
            val screenW = metrics.widthPixels.toFloat()
            val screenH = metrics.heightPixels.toFloat()
            this.screenWidth  = screenW
            this.screenHeight = screenH

            var rootXMin = 0f; var rootYMin = 0f
            var rootXMax = 1f; var rootYMax = 1f

            for (i in 0 until elements.length()) {
                val el = elements.getJSONObject(i)
                if (el.optString("elementType") == "RootAdView") {
                    val rt = el.getJSONObject("rectTransform")
                    rootXMin = rt.getJSONObject("anchorMin").optDouble("x", 0.0).toFloat()
                    rootYMin = rt.getJSONObject("anchorMin").optDouble("y", 0.0).toFloat()
                    rootXMax = rt.getJSONObject("anchorMax").optDouble("x", 1.0).toFloat()
                    rootYMax = rt.getJSONObject("anchorMax").optDouble("y", 1.0).toFloat()
                    break
                }
            }

            // Flip Y-Axis: Unity (bottom=0) to Android (top=0) mapping
            val rootPixelLeft   = (screenW * rootXMin).toInt()
            val rootPixelTop    = (screenH * (1f - rootYMax)).toInt()
            val rootPixelRight  = (screenW * rootXMax).toInt()
            val rootPixelBottom = (screenH * (1f - rootYMin)).toInt()
            rootPixelRect.set(rootPixelLeft, rootPixelTop, rootPixelRight, rootPixelBottom)

            val rootW = if (rootXMax - rootXMin > 0f) rootXMax - rootXMin else 1f
            val rootH = if (rootYMax - rootYMin > 0f) rootYMax - rootYMin else 1f

            // PASS 2: Elemental Reconstruction - Local Space projection
            for (i in 0 until elements.length()) {
                val el = elements.getJSONObject(i)
                val elementType = el.optString("elementType", "Unknown")

                val rt = el.getJSONObject("rectTransform")
                val anchorMin = rt.getJSONObject("anchorMin")
                val anchorMax = rt.getJSONObject("anchorMax")

                // Transpose Screen Coordinates to Relative Root-Space
                val localXMin = ((anchorMin.optDouble("x").toFloat() - rootXMin) / rootW).coerceIn(0f, 1f)
                val localYMin = ((anchorMin.optDouble("y").toFloat() - rootYMin) / rootH).coerceIn(0f, 1f)
                val localXMax = ((anchorMax.optDouble("x").toFloat() - rootXMin) / rootW).coerceIn(0f, 1f)
                val localYMax = ((anchorMax.optDouble("y").toFloat() - rootYMin) / rootH).coerceIn(0f, 1f)
                
                val hasValidText = el.has("text")
                val hasValidImage = el.has("image")
                
                val txtObj = el.optJSONObject("text")
                val imgObj = el.optJSONObject("image")

                var oFontSize: Float? = null
                var finalView: View? = null

                if (elementType == "MediaView") {
                    finalView = MediaView(context)
                } else if (elementType == "IconView") {
                    finalView = ImageView(context).apply { 
                        scaleType = ImageView.ScaleType.FIT_XY 
                        setBackgroundColor(Color.TRANSPARENT)
                    }
                } else if (hasValidText && hasValidImage) {
                    val container = FrameLayout(context)
                    val iv = ImageView(context)
                    iv.scaleType = ImageView.ScaleType.FIT_XY
                    val htmlColor = imgObj?.optString("color", "#FFFFFF") ?: "#FFFFFF"
                    val imgPath = imgObj?.optString("imagePath")?.takeIf { it != "null" } ?: ""
                    if (imgPath.isNotEmpty()) {
                        iv.setBackgroundColor(Color.TRANSPARENT)
                        val border = imgObj?.optJSONObject("border")
                        NativeAdImageCache.displayImage(context, iv, imgPath, border, screenHeight, referenceHeight)
                    } else {
                        iv.setBackgroundColor(parseUnityColor(htmlColor))
                    }

                    val tv = TextView(context)
                    tv.text = txtObj?.optString("textContent", "")
                    tv.setTextColor(parseUnityColor(txtObj?.optString("color", "#FFFFFF") ?: "#FFFFFF"))
                    val fontSize = txtObj?.optDouble("fontSize", 14.0)?.toFloat() ?: 14f
                    oFontSize = fontSize
                    tv.gravity = parseGravity(txtObj?.optString("alignment", "MiddleCenter"))
                    tv.includeFontPadding = true 
                    tv.maxLines = 1
                    tv.ellipsize = TextUtils.TruncateAt.END
                    applyTypeface(tv, txtObj?.optBoolean("isBold", false) ?: false, txtObj?.optBoolean("isItalic", false) ?: false)

                    val txtRt = txtObj?.optJSONObject("rectTransform")
                    val tvBounds = if (txtRt != null) {
                        val tAnchorMin = txtRt.getJSONObject("anchorMin")
                        val tAnchorMax = txtRt.getJSONObject("anchorMax")
                        NormBounds(
                            tAnchorMin.optDouble("x").toFloat(),
                            tAnchorMin.optDouble("y").toFloat(),
                            tAnchorMax.optDouble("x").toFloat(),
                            tAnchorMax.optDouble("y").toFloat(),
                            oFontSize
                        )
                    } else {
                        NormBounds(0f, 0f, 1f, 1f, oFontSize)
                    }
                    tv.tag = tvBounds
                    registeredViews["${elementType}_Text"] = tv
                    container.addView(iv,
                        LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
                    )
                    container.addView(tv, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
                    finalView = container
                } else if (hasValidImage) {
                    val iv = ImageView(context)
                    iv.scaleType = ImageView.ScaleType.FIT_XY
                    val htmlColor = imgObj?.optString("color", "#FFFFFF") ?: "#FFFFFF"
                    val imgPath = imgObj?.optString("imagePath")?.takeIf { it != "null" } ?: ""
                    if (imgPath.isNotEmpty()) {
                        iv.setBackgroundColor(Color.TRANSPARENT)
                        val border = imgObj?.optJSONObject("border")
                        NativeAdImageCache.displayImage(context, iv, imgPath, border, screenHeight, referenceHeight)
                    } else {
                        iv.setBackgroundColor(parseUnityColor(htmlColor))
                    }
                    finalView = iv
                } else if (hasValidText) {
                    val tv = TextView(context)
                    tv.text = txtObj?.optString("textContent", "")
                    tv.setTextColor(parseUnityColor(txtObj?.optString("color", "#FFFFFF") ?: "#FFFFFF"))
                    val fontSize = txtObj?.optDouble("fontSize", 14.0)?.toFloat() ?: 14f
                    oFontSize = fontSize
                    tv.gravity = parseGravity(txtObj?.optString("alignment", "MiddleCenter"))
                    tv.includeFontPadding = false 
                    tv.maxLines = if (elementType == "Body") 2 else 1
                    tv.ellipsize = TextUtils.TruncateAt.END
                    applyTypeface(tv, txtObj?.optBoolean("isBold", false) ?: false, txtObj?.optBoolean("isItalic", false) ?: false)
                    finalView = tv
                } else {
                    finalView = View(context)
                }

                if (finalView != null) {
                    finalView.tag = NormBounds(localXMin, localYMin, localXMax, localYMax, oFontSize)
                    registeredViews[elementType] = finalView
                    addView(finalView)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = measuredWidth.toFloat()
        val h = measuredHeight.toFloat()
        if (w <= 0 || h <= 0) return

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val bounds = child.tag as? NormBounds ?: continue
            val childWidth = (w * (bounds.xMax - bounds.xMin)).toInt()
            val childHeight = (h * (bounds.yMax - bounds.yMin)).toInt()
            child.measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY))

            if (child.javaClass == FrameLayout::class.java) {
                val container = child as FrameLayout
                for (j in 0 until container.childCount) {
                    val innerChild = container.getChildAt(j)
                    val innerBounds = innerChild.tag as? NormBounds
                    if (innerBounds != null) {
                        val icWidth = (childWidth * (innerBounds.xMax - innerBounds.xMin)).toInt().coerceAtLeast(0)
                        val icHeight = (childHeight * (innerBounds.yMax - innerBounds.yMin)).toInt().coerceAtLeast(0)
                        innerChild.measure(MeasureSpec.makeMeasureSpec(icWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(icHeight, MeasureSpec.EXACTLY))
                    } else {
                        innerChild.measure(MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY), MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY))
                    }
                }
            }
        }
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        val w = measuredWidth.toFloat()
        val h = measuredHeight.toFloat()
        if (w <= 0 || h <= 0) return

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val bounds = child.tag as? NormBounds ?: continue
            val cLeft = (w * bounds.xMin).toInt()
            val cRight = (w * bounds.xMax).toInt()
            val cTop = (h * (1f - bounds.yMax)).toInt()
            val cBottom = (h * (1f - bounds.yMin)).toInt()
            child.layout(cLeft, cTop, cRight, cBottom)

            if (child is FrameLayout && child.javaClass == FrameLayout::class.java) {
                val container = child
                val cWidth = cRight - cLeft
                val cHeight = cBottom - cTop
                for (j in 0 until container.childCount) {
                    val innerChild = container.getChildAt(j)
                    val innerBounds = innerChild.tag as? NormBounds
                    if (innerBounds != null) {
                        val icLeft = (cWidth * innerBounds.xMin).toInt()
                        val icRight = (cWidth * innerBounds.xMax).toInt()
                        val icTop = (cHeight * (1f - innerBounds.yMax)).toInt()
                        val icBottom = (cHeight * (1f - innerBounds.yMin)).toInt()
                        innerChild.layout(icLeft, icTop, icRight, icBottom)
                    } else {
                        innerChild.layout(0, 0, cWidth, cHeight)
                    }
                }
            }

            // High-precision Font Scaling based on screen density and reference resolution
            if (bounds.originalFontSize != null) {
                val scaleY = screenHeight / referenceHeight
                val finalPixelSize = bounds.originalFontSize * scaleY

                if (child is TextView) {
                    child.setTextSize(TypedValue.COMPLEX_UNIT_PX, finalPixelSize)
                } else if (child is FrameLayout) {
                    for (j in 0 until child.childCount) {
                        val innerChild = child.getChildAt(j)
                        if (innerChild is TextView) {
                            innerChild.setTextSize(TypedValue.COMPLEX_UNIT_PX, finalPixelSize)
                            break
                        }
                    }
                }
            }
        }
    }
}
