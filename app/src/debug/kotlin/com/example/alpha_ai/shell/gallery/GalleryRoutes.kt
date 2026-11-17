package com.example.alpha_ai.shell.gallery

import androidx.annotation.StringRes
import com.example.alpha_ai.R
import kotlinx.serialization.Serializable

/** The gallery index. */
@Serializable
data object GalleryRoute

/** One gallery section. The argument is an enum, not an object (constitution V). */
@Serializable
data class GallerySectionRoute(val section: GallerySection)

enum class GallerySection(@StringRes val labelRes: Int) {
    Colors(R.string.gallery_section_colors),
    Typography(R.string.gallery_section_typography),
    Spacing(R.string.gallery_section_spacing),
    Shapes(R.string.gallery_section_shapes),
    Motion(R.string.gallery_section_motion),
    Buttons(R.string.gallery_section_buttons),
    TextFields(R.string.gallery_section_text_fields),
    Cards(R.string.gallery_section_cards),
    TopBar(R.string.gallery_section_top_bar),
    States(R.string.gallery_section_states),
    Language(R.string.gallery_section_language),
}
