package com.thelegends.ads.admob_native_unity.parser

import com.thelegends.ads.admob_native_unity.model.*
import org.json.JSONArray
import org.json.JSONObject

/**
 * Parses a JSON blueprint string (exported from the Unity AdMob Native layout tool) into a
 * typed [AdLayoutData] object ready for rendering.
 *
 * Responsibilities:
 *  - Extract referenceWidth / referenceHeight scaling baselines
 *  - Detect the RootAdView element to compute [RootBounds]
 *  - Convert each element's rectTransform anchors into [ElementData] with optional [TextData] / [ImageData]
 *
 * This class has no Android View dependencies and can be unit-tested without instrumentation.
 */
class AdLayoutParser {

    /**
     * @throws org.json.JSONException if the top-level JSON structure is invalid
     */
    fun parse(jsonString: String): AdLayoutData {
        val root = JSONObject(jsonString)
        val referenceWidth  = root.optDouble("referenceWidth",  1080.0).toFloat()
        val referenceHeight = root.optDouble("referenceHeight", 1920.0).toFloat()

        val elementsArray = root.optJSONArray("elements")

        val rootBounds   = extractRootBounds(elementsArray)
        val elementList  = parseElements(elementsArray)

        return AdLayoutData(
            referenceWidth  = referenceWidth,
            referenceHeight = referenceHeight,
            elements        = elementList,
            rootBounds      = rootBounds
        )
    }

    // ── Root Bounds (PASS 1) ──────────────────────────────────────────────────

    private fun extractRootBounds(elements: JSONArray?): RootBounds {
        if (elements == null) return RootBounds()
        for (i in 0 until elements.length()) {
            val el = elements.getJSONObject(i)
            if (el.optString("elementType") != "RootAdView") continue

            val rt        = el.getJSONObject("rectTransform")
            val anchorMin = rt.getJSONObject("anchorMin")
            val anchorMax = rt.getJSONObject("anchorMax")
            return RootBounds(
                xMin = anchorMin.optDouble("x", 0.0).toFloat(),
                yMin = anchorMin.optDouble("y", 0.0).toFloat(),
                xMax = anchorMax.optDouble("x", 1.0).toFloat(),
                yMax = anchorMax.optDouble("y", 1.0).toFloat()
            )
        }
        return RootBounds() // full-screen fallback
    }

    // ── Elements (PASS 2) ─────────────────────────────────────────────────────

    private fun parseElements(elements: JSONArray?): List<ElementData> {
        if (elements == null) return emptyList()
        val list = mutableListOf<ElementData>()
        for (i in 0 until elements.length()) {
            list.add(parseElement(elements.getJSONObject(i)))
        }
        return list
    }

    private fun parseElement(el: JSONObject): ElementData {
        val elementType = el.optString("elementType", "Unknown")

        val rt        = el.getJSONObject("rectTransform")
        val anchorMin = rt.getJSONObject("anchorMin")
        val anchorMax = rt.getJSONObject("anchorMax")

        val textData  = if (el.has("text"))  parseTextData(el.getJSONObject("text"), elementType) else null
        val imageData = if (el.has("image")) parseImageData(el.getJSONObject("image"))             else null

        return ElementData(
            elementType = elementType,
            anchorMin   = Pair(anchorMin.optDouble("x").toFloat(), anchorMin.optDouble("y").toFloat()),
            anchorMax   = Pair(anchorMax.optDouble("x").toFloat(), anchorMax.optDouble("y").toFloat()),
            textData    = textData,
            imageData   = imageData
        )
    }

    private fun parseTextData(txtObj: JSONObject, elementType: String): TextData {
        // Body elements may wrap to 2 lines; all other text elements are single-line
        val maxLines = if (elementType == "Body") 2 else 1

        val rectTransform = txtObj.optJSONObject("rectTransform")?.let { rt ->
            val tMin = rt.getJSONObject("anchorMin")
            val tMax = rt.getJSONObject("anchorMax")
            RectBoundsData(
                anchorMin = Pair(tMin.optDouble("x").toFloat(), tMin.optDouble("y").toFloat()),
                anchorMax = Pair(tMax.optDouble("x").toFloat(), tMax.optDouble("y").toFloat())
            )
        }

        return TextData(
            textContent        = txtObj.optString("textContent", ""),
            color              = txtObj.optString("color", "#FFFFFFFF"),
            fontSize           = txtObj.optDouble("fontSize", 14.0).toFloat(),
            alignment          = txtObj.optString("alignment", "MiddleCenter"),
            isBold             = txtObj.optBoolean("isBold", false),
            isItalic           = txtObj.optBoolean("isItalic", false),
            includeFontPadding = false, // builders override this based on composite context
            maxLines           = maxLines,
            rectTransform      = rectTransform
        )
    }

    private fun parseImageData(imgObj: JSONObject): ImageData {
        return ImageData(
            imagePath = imgObj.optString("imagePath", "").takeIf { it != "null" } ?: "",
            color     = imgObj.optString("color", "#FFFFFFFF"),
            border    = imgObj.optJSONObject("border")
        )
    }
}
