package ml.docilealligator.infinityforreddit.utils

import android.graphics.drawable.Drawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.OvalShape
import android.widget.ImageView
import com.squareup.picasso.Picasso
import jp.wasabeef.picasso.transformations.CropCircleTransformation
import ml.docilealligator.infinityforreddit.SessionHolder
import org.matrix.android.sdk.api.session.content.ContentUrlResolver
import org.matrix.android.sdk.api.util.MatrixItem


class AvatarRenderer(private val matrixItemColorProvider: MatrixItemColorProvider) {

    companion object {
        private const val THUMBNAIL_SIZE = 250
    }

    fun render(avatarUrl: String?, imageView: ImageView) {
        val resolvedUrl = resolvedUrl(avatarUrl)
        Picasso.get()
            .load(resolvedUrl)
            .transform(CropCircleTransformation())
            .into(imageView)
    }

    fun render(matrixItem: MatrixItem, imageView: ImageView) {
        val resolvedUrl = resolvedUrl(matrixItem.avatarUrl)
        val placeholder = getPlaceholderDrawable(matrixItem)
        Picasso.get()
            .load(resolvedUrl)
            .placeholder(placeholder)
            .transform(CropCircleTransformation())
            .into(imageView)
    }

    fun getPlaceholderDrawable(matrixItem: MatrixItem): Drawable {
        val avatarColor = matrixItemColorProvider.getColor(matrixItem)
        val drawable = ShapeDrawable(OvalShape())
        drawable.paint.color = avatarColor
        drawable.intrinsicWidth = THUMBNAIL_SIZE
        drawable.intrinsicHeight = THUMBNAIL_SIZE
        return drawable
    }

    // PRIVATE API *********************************************************************************

    private fun resolvedUrl(avatarUrl: String?): String? {
        // Take care of using contentUrlResolver to use with mxc://
        return SessionHolder.currentSession?.contentUrlResolver()
            ?.resolveThumbnail(
                avatarUrl,
                THUMBNAIL_SIZE,
                THUMBNAIL_SIZE,
                ContentUrlResolver.ThumbnailMethod.SCALE
            )
    }
}