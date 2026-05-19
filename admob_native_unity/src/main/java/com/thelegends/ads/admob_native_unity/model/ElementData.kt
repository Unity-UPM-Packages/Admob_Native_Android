package com.thelegends.ads.admob_native_unity.model

import org.json.JSONObject

/**
 * Normalized anchor coordinates of an element's rectTransform child (e.g. a TextView inside a composite element).
 * Values are relative to the parent element's bounding box.
 */
data class RectBoundsData(
    val anchorMin: Pair<Float, Float>,
    val anchorMax: Pair<Float, Float>
)

/**
 * Parsed text component of a UI element.
 *
 * @param textContent       Default/placeholder text string (overridden at data-binding time by AdMob SDK)
 * @param color             Unity RGBA HTML color string (#RRGGBBAA)
 * @param fontSize          Font size in Unity pixel units (scaled by referenceHeight ratio at layout time)
 * @param alignment         Unity TextAnchor enum name (e.g. "MiddleCenter", "UpperLeft")
 * @param isBold            Whether bold style should be applied
 * @param isItalic          Whether italic style should be applied
 * @param includeFontPadding Whether to include top/bottom font padding (true for composite overlays)
 * @param maxLines          Maximum number of visible text lines (currently always 1)
 * @param rectTransform     Optional sub-rect for the text inside a composite (text + image) element
 */
data class TextData(
    val textContent: String,
    val color: String,
    val fontName: String?,
    val fontSize: Float,
    val alignment: String,
    val isBold: Boolean,
    val isItalic: Boolean,
    val includeFontPadding: Boolean,
    val maxLines: Int,
    val rectTransform: RectBoundsData?
)

/**
 * Parsed image/background component of a UI element.
 *
 * @param imagePath Absolute file-system path to the decoded PNG texture (empty string = color-only)
 * @param color     Unity RGBA HTML fallback color when no image path is present
 * @param border    Optional 9-slice metadata (left/right/top/bottom in Unity pixel units + ppuMultiplier)
 */
data class ImageData(
    val imagePath: String,
    val color: String,
    val border: JSONObject?
)

/**
 * Single UI element parsed from a JSON blueprint.
 *
 * @param elementType Semantic type string matching Unity's AdMob layout element (e.g. "Headline", "MediaView")
 * @param anchorMin   Screen-space normalised bottom-left anchor (Unity coordinate system)
 * @param anchorMax   Screen-space normalised top-right anchor (Unity coordinate system)
 * @param textData    Non-null when element carries a text component
 * @param imageData   Non-null when element carries an image/background component
 */
data class ElementData(
    val elementType: String,
    val anchorMin: Pair<Float, Float>,
    val anchorMax: Pair<Float, Float>,
    val textData: TextData?,
    val imageData: ImageData?
) {
    val hasText: Boolean get() = textData != null
    val hasImage: Boolean get() = imageData != null
}
