package com.thelegends.ads.admob_native_unity

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.NinePatch
import android.graphics.Typeface
import android.graphics.drawable.NinePatchDrawable
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.nativead.NativeAdView
import org.json.JSONObject
import java.util.concurrent.Executors


/**
 * LruCache Mộc (Nguyên thủy): Thay thế hoàn toàn Glide!
 * Quản lý chừng mực RAM, bốc ảnh thả vào Background Thread siêu mượt.
 */
object DynamicImageCache {
    // Cấp phát 1/8 tổng RAM tối đa của App cho kho chứa Ảnh
    private val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    private val cacheSize = maxMemory / 8 // Tính bằng KB

    private val memoryCache = object : LruCache<String, Bitmap>(cacheSize) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int {
            return bitmap.byteCount / 1024 // Kích thước tính bằng KB
        }
    }

    private val executor = Executors.newFixedThreadPool(4)
    private val uiHandler = Handler(Looper.getMainLooper())

    fun loadImage(imageView: ImageView, path: String) {
        android.util.Log.d("DynamicUI", "-> Bắt đầu nạp ảnh từ đường dẫn: $path")
        val cachedBitmap = memoryCache.get(path)
        if (cachedBitmap != null) {
            imageView.setImageBitmap(cachedBitmap) // Ăn sẵn từ RAM (0ms)
            android.util.Log.d("DynamicUI", "-> Bốc ảnh từ RAM thành công: $path")
            return
        }

        // Đẩy nhiệm vụ bốc vác Ổ Cứng ra Background Thread (Chống giật Lag)
        executor.execute {
            try {
                android.util.Log.d("DynamicUI", "-> Chuẩn bị bốc ảnh từ Ổ Cứng (decodeFile): $path")
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    android.util.Log.d("DynamicUI", "-> VỀ ĐÍCH! Giải mã Bitmap thành công: $path (w:${bitmap.width}, h:${bitmap.height})")
                    memoryCache.put(path, bitmap) // Lưu vào Kho
                    // Đẩy mâm ảnh về lại UI Thread để hiện hình
                    uiHandler.post {
                        imageView.setImageBitmap(bitmap)
                    }
                } else {
                    android.util.Log.e("DynamicUI", "-> THẤT BẠI! decodeFile trả về NULL. Đường dẫn có đúng không? Hoặc file bị hỏng? Path: $path")
                }
            } catch (e: Exception) {
                android.util.Log.e("DynamicUI", "-> VỠ TRẬN! Crash khi load ảnh từ: $path - Lỗi: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    /**
     * RENDER IMAGE: Load ảnh và hiển thị. 
     * Tự động kiểm tra nếu có thông số border thì vẽ NinePatch (9-slice), 
     * nếu không có thì vẽ Bitmap thường.
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
                } else {
                    android.util.Log.e("DynamicUI", "-> Image FAILED: NULL bitmap at $path")
                }
            } catch (e: Exception) {
                android.util.Log.e("DynamicUI", "-> Image CRASHED: ${e.message}")
            }
        }
    }

    /**
     * Tạo NinePatchDrawable từ Bitmap và thông số border pixel.
     * Unity sprite.border = (left, bottom, right, top) tính bằng pixel của ảnh GỐC.
     */
    private fun applyNinePatch(context: Context, imageView: ImageView, bitmap: Bitmap, border: JSONObject, screenH: Float, refH: Float) {
        val ppum = border.optDouble("ppuMultiplier", 1.0).toFloat().takeIf { it > 0f } ?: 1f
        // S = (1 / ppuMultiplier) * (screenHeight / referenceHeight)
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
            if (kotlin.math.abs(targetScale - 1f) < 0.01f) bitmap 
            else Bitmap.createScaledBitmap(bitmap, scaledBw, scaledBh, true)
        } catch (e: Exception) { bitmap }

        val left   = (oLeft   * targetScale).toInt()
        val right  = (oRight  * targetScale).toInt()
        val top    = (oTop    * targetScale).toInt()
        val bottom = (oBottom * targetScale).toInt()

        val bw = scaledBitmap.width
        val bh = scaledBitmap.height

        android.util.Log.d("DynamicUI", "-> 9-Slice Processing: Original Img=${bitmap.width}x${bitmap.height}, Scaled=${bw}x${bh}, TargetScale=$targetScale, Pixels=[L:$left, R:$right, T:$top, B:$bottom]")

        try {
            // Tọa độ các đường cắt X (Trái -> Phải) và Y (Trên -> Dưới)
            val xDivs = intArrayOf(left, bw - right)
            val yDivs = intArrayOf(top, bh - bottom)

            // Kiểm tra tính hợp lệ
            if (xDivs[0] >= xDivs[1] || yDivs[0] >= yDivs[1]) {
                android.util.Log.w("DynamicUI", "-> 9-Slice WARNING: Invalid scaled coordinates. Normal Stretch applied.")
                imageView.setImageBitmap(scaledBitmap)
                return
            }

            val numColors = 9
            val NO_COLOR  = 0x00000001.toInt()
            val colors    = IntArray(numColors) { NO_COLOR }

            // Cấu trúc 32 bytes Header CHUẨN của Android:
            // [1byte wasDeser][1byte numX][1byte numY][1byte numColors] -> 4 bytes
            // [4bytes xDivsOffset][4bytes yDivsOffset] -> 8 bytes
            // [16bytes padding L,R,T,B] -> 16 bytes
            // [4bytes reserved/skipFlags] -> 4 bytes (TỔNG: 32 bytes)
            val buffer = java.nio.ByteBuffer
                .allocate(32 + (xDivs.size + yDivs.size + colors.size) * 4)
                .order(java.nio.ByteOrder.nativeOrder())

            // 1. Meta (4 bytes)
            buffer.put(1.toByte())
            buffer.put(xDivs.size.toByte())
            buffer.put(yDivs.size.toByte())
            buffer.put(numColors.toByte())

            // 2. Offsets (8 bytes - dùng 0 để Runtime tự xử lý)
            buffer.putInt(0)
            buffer.putInt(0)

            // 3. Padding (16 bytes - không padding nội dung)
            buffer.putInt(0); buffer.putInt(0); buffer.putInt(0); buffer.putInt(0)

            // 4. RESERVED (4 bytes - CỰC KỲ QUAN TRỌNG ĐỂ KHÔNG BỊ LỆCH MẢNG)
            buffer.putInt(0)

            // 5. Data (2*4 + 2*4 + 9*4 = 52 bytes)
            for (v in xDivs) buffer.putInt(v)
            for (v in yDivs) buffer.putInt(v)
            for (c in colors) buffer.putInt(c)

            val chunkBytes = buffer.array()
            
            if (NinePatch.isNinePatchChunk(chunkBytes)) {
                // Force density so NinePatch doesn't mysteriously rescale again against the screen
                scaledBitmap.density = context.resources.displayMetrics.densityDpi
                val drawable = NinePatchDrawable(context.resources, scaledBitmap, chunkBytes, android.graphics.Rect(), null)
                
                imageView.setImageDrawable(null) // Xóa Image foreground nếu có
                imageView.background = drawable
                
                android.util.Log.d("DynamicUI", "-> 9-Slice SUCCESS: Using setBackground for perfect scaling.")
            } else {
                android.util.Log.e("DynamicUI", "-> 9-Slice FAILED: Invalid chunk bytes.")
                imageView.setImageBitmap(scaledBitmap)
            }
        } catch (e: Exception) {
            android.util.Log.e("DynamicUI", "-> 9-Slice ERROR: ${e.message}")
            imageView.setImageBitmap(scaledBitmap)
        }
    }
}

/**
 * Gói chứa thông số Tọa độ Chuẩn Hóa đã được hút từ C# sang
 */
class NormBounds(
    val xMin: Float, 
    val yMin: Float, 
    val xMax: Float, 
    val yMax: Float,
    val originalFontSize: Float? = null // Backup font size để scale theo độ phân giải màn hình
)

/**
 * Giai Đoạn 3, 5 & 6: Mâm Chứa Quảng Cáo Cục Bộ (Tượng trưng cho RootAdView bên Unity).
 * Chuyển sang kế thừa FrameLayout vì NativeAdView của AdMob là class 'final'.
 */
class DynamicAdBuilderLayout(context: Context) : FrameLayout(context) {

    var jsonPayload: String? = null
    var referenceWidth: Float = 1080f
    var referenceHeight: Float = 1920f

    // Kích thước màn hình thực tế (lưu lại để scale font đúng sau khi NativeAdView đã được thu nhỏ)
    private var screenWidth: Float = 1080f
    private var screenHeight: Float = 1920f

    // Pixel rect của RootAdView trên màn hình thực tế (để DynamicShowBehavior sizing NativeAdView)
    private var rootPixelRect = android.graphics.Rect(0, 0, 0, 0)

    fun getRootPixelRect(): android.graphics.Rect = rootPixelRect

    // Bản đồ định danh (Lưu tên "Native_Headline" -> Bắn ra đúng cái TextView đó)
    // Sẽ dâng cho Giai đoạn 6 xài.
    val registeredViews = HashMap<String, View>()

    init {
        clipChildren = false
        clipToPadding = false
    }

    /**
     * TRÁI TIM CỖ MÁY (GIAI ĐOẠN 5): Đọc JSON > Phân Thể View Android.
     */

    /**
     * Unity xuất mã màu là #RRGGBBAA. Nhưng Android Color.parseColor lại khao khát #AARRGGBB.
     * Hàm này sẽ cắt đuôi Alpha của Unity và nhét lên đầu để Android hiểu đúng màu.
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
     * TRÁI TIM CỖ MÁY (GIAI ĐOẠN 5): Đọc JSON > Phân Thể View Android.
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

            // ─────────── PASS 1: Tìm RootAdView → Tính Pixel Rect trên màn hình thực ───────────
            val metrics = context.resources.displayMetrics
            val screenW = metrics.widthPixels.toFloat()
            val screenH = metrics.heightPixels.toFloat()
            // Lưu lại để dùng trong onLayout cho việc scale font
            // Dùng trực tiếp displayMetrics — hoạt động đúng cả landscape lẫn portrait
            this.screenWidth  = screenW
            this.screenHeight = screenH

            // Mặc định: Root chiếm toàn màn hình (fallback nếu không có RootAdView trong JSON)
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

            // Đảo trục Y (Unity bottom=0 → Android top=0)
            val rootPixelLeft   = (screenW * rootXMin).toInt()
            val rootPixelTop    = (screenH * (1f - rootYMax)).toInt()
            val rootPixelRight  = (screenW * rootXMax).toInt()
            val rootPixelBottom = (screenH * (1f - rootYMin)).toInt()
            rootPixelRect.set(rootPixelLeft, rootPixelTop, rootPixelRight, rootPixelBottom)

            android.util.Log.d("DynamicAdBuilder",
                "Root pixel rect: L=$rootPixelLeft T=$rootPixelTop R=$rootPixelRight B=$rootPixelBottom")

            val rootW = if (rootXMax - rootXMin > 0f) rootXMax - rootXMin else 1f
            val rootH = if (rootYMax - rootYMin > 0f) rootYMax - rootYMin else 1f

            // ─────────── PASS 2: Dựng View, normalize tọa độ về Local Root Space ───────────
            for (i in 0 until elements.length()) {
                val el = elements.getJSONObject(i)
                val elementType = el.optString("elementType", "Unknown")

                val rt = el.getJSONObject("rectTransform")
                val anchorMin = rt.getJSONObject("anchorMin")
                val anchorMax = rt.getJSONObject("anchorMax")

                // Tịnh tiến tọa độ Screen → Local (relative to Root)
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

                // CỖ MÁY DỰNG VIEW LÕI KÉP
                if (elementType == "MediaView") {
                    val mediaView = com.google.android.gms.ads.nativead.MediaView(context)
                    finalView = mediaView
                } else if (elementType == "IconView") {
                    // Tạo một ImageView rỗng (khuôn) cho Icon AdMob
                    val iv = ImageView(context)
                    iv.scaleType = ImageView.ScaleType.FIT_XY
                    iv.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                    finalView = iv
                } else if (hasValidText && hasValidImage) {
                    val container = FrameLayout(context)

                    val iv = ImageView(context)
                    iv.scaleType = ImageView.ScaleType.FIT_XY
                    val htmlColor = imgObj?.optString("color", "#FFFFFF") ?: "#FFFFFF"
                    val imgPath = imgObj?.optString("imagePath")?.takeIf { it != "null" } ?: ""
                    if (imgPath.isNotEmpty()) {
                        iv.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        val border = imgObj?.optJSONObject("border")
                        DynamicImageCache.displayImage(context, iv, imgPath, border, screenHeight, referenceHeight)
                    } else {
                        // imagePath=null trong JSON → chỉ có Unity Color → dùng màu solid
                        iv.setBackgroundColor(parseUnityColor(htmlColor))
                    }

                    val tv = TextView(context)
                    tv.text = txtObj?.optString("textContent", "")
                    tv.setTextColor(parseUnityColor(txtObj?.optString("color", "#FFFFFF") ?: "#FFFFFF"))
                    val fontSize = txtObj?.optDouble("fontSize", 14.0)?.toFloat() ?: 14f
                    oFontSize = fontSize
                    tv.gravity = parseGravity(txtObj?.optString("alignment", "MiddleCenter"))
                    tv.includeFontPadding = true  // PHẢI GIỮ LÀ TRUE: Text trong các nút/nhãn cần padding gốc của font để Android cân bằng (visual center) thay vì ép chặt (math center) gây lệch mắt
                    // Giới hạn 1 dòng và thêm dấu "..." cho các nút bấm/nhãn
                    tv.maxLines = 1
                    tv.ellipsize = android.text.TextUtils.TruncateAt.END
                    
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

                    container.addView(iv, FrameLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
                    container.addView(tv, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))

                    finalView = container
                } else if (hasValidImage) {
                    val iv = ImageView(context)
                    iv.scaleType = ImageView.ScaleType.FIT_XY
                    val htmlColor = imgObj?.optString("color", "#FFFFFF") ?: "#FFFFFF"
                    val imgPath = imgObj?.optString("imagePath")?.takeIf { it != "null" } ?: ""
                    if (imgPath.isNotEmpty()) {
                        iv.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                        val border = imgObj?.optJSONObject("border")
                        DynamicImageCache.displayImage(context, iv, imgPath, border, screenHeight, referenceHeight)
                    } else {
                        // imagePath=null trong JSON → chỉ có Unity Color → dùng màu solid
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
                    tv.includeFontPadding = false  // Bỏ padding ascent/descent để CENTER_VERTICAL thực sự căn giữa
                    
                    // Headline chỉ 1 dòng, Body có thể 2 dòng. Tránh tràn layout
                    if (elementType == "Body") {
                        tv.maxLines = 2
                    } else {
                        tv.maxLines = 1
                    }
                    tv.ellipsize = android.text.TextUtils.TruncateAt.END
                    
                    applyTypeface(tv, txtObj?.optBoolean("isBold", false) ?: false, txtObj?.optBoolean("isItalic", false) ?: false)

                    finalView = tv
                } else {
                    finalView = View(context)
                }

                if (finalView != null) {
                    val bounds = NormBounds(
                        localXMin, localYMin, localXMax, localYMax,
                        oFontSize
                    )
                    finalView.tag = bounds
                    registeredViews[elementType] = finalView
                    addView(finalView)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * ÉP CON (MEASURE): Phải ép các View con đo đạc chính xác kích thước Toán Học
     * trước khi Layout. Nếu không, TextView sẽ bị móp lại theo WRAP_CONTENT và từ chối hiện chữ.
     */
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
            
            val childWidthSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY)
            val childHeightSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
            
            child.measure(childWidthSpec, childHeightSpec)

            // Measure inner children if it is our custom FrameLayout container (not MediaView)
            if (child.javaClass == FrameLayout::class.java) {
                val container = child as FrameLayout
                for (j in 0 until container.childCount) {
                    val innerChild = container.getChildAt(j)
                    val innerBounds = innerChild.tag as? NormBounds
                    if (innerBounds != null) {
                        val icWidth = (childWidth * (innerBounds.xMax - innerBounds.xMin)).toInt().coerceAtLeast(0)
                        val icHeight = (childHeight * (innerBounds.yMax - innerBounds.yMin)).toInt().coerceAtLeast(0)
                        innerChild.measure(
                            MeasureSpec.makeMeasureSpec(icWidth, MeasureSpec.EXACTLY),
                            MeasureSpec.makeMeasureSpec(icHeight, MeasureSpec.EXACTLY)
                        )
                    } else {
                        innerChild.measure(childWidthSpec, childHeightSpec) // MATCH_PARENT
                    }
                }
            }
        }
    }

    /**
     * THUẬT TOÁN ĐẢO CHIỀU (FLIP Y AXIS) VÀ CĂN KHUNG TUYỆT ĐỐI (Toán học Pixel-perfect)
     */
    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        // KHÔNG GỌI super.onLayout() NỮA ĐỂ TRÁNH BỊ FRAMELAYOUT CAN THIỆP
        // super.onLayout(changed, left, top, right, bottom)
        
        val w = measuredWidth.toFloat()
        val h = measuredHeight.toFloat()
        
        if (w <= 0 || h <= 0) return

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            val bounds = child.tag as? NormBounds ?: continue
            
            // Ép viền Tọa Độ Không Gian (X Unity = X Android)
            val cLeft = (w * bounds.xMin).toInt()
            val cRight = (w * bounds.xMax).toInt()
            
            // Ép viền Lộn Ngược (Y Unity Bottom = Y Android Top)
            val cTop = (h * (1f - bounds.yMax)).toInt()
            val cBottom = (h * (1f - bounds.yMin)).toInt()
            
            child.layout(cLeft, cTop, cRight, cBottom)

            // Layout inner children based on their relative coordinates inside the container
            if (child.javaClass == FrameLayout::class.java) {
                val container = child as FrameLayout
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

            // Auto Scale Size Chữ: cần dùng screenHeight thực tế (không phải h của View đã bị thu nhỏ)
            if (bounds.originalFontSize != null) {
                val scaleY = screenHeight / referenceHeight
                val finalPixelSize = bounds.originalFontSize * scaleY

                // Trường hợp 1: child trực tiếp là TextView (element chỉ có text)
                if (child is TextView) {
                    child.setTextSize(TypedValue.COMPLEX_UNIT_PX, finalPixelSize)
                }
                // Trường hợp 2: child là FrameLayout chứa ImageView + TextView (element có cả text lẫn image)
                // Ví dụ: CallToAction (nút xanh + chữ Install), AdAttribution (nền vàng + chữ Ad)
                else if (child is FrameLayout) {
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
