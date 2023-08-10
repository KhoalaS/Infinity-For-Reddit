package ml.docilealligator.infinityforreddit.utils


import android.content.Context
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.VisibleForTesting
import androidx.core.content.ContextCompat
import org.matrix.android.sdk.api.util.MatrixItem
import ml.docilealligator.infinityforreddit.R
import kotlin.math.abs

class MatrixItemColorProvider(private val context: Context) {
    private val cache = mutableMapOf<String, Int>()

    @ColorInt
    fun getColor(matrixItem: MatrixItem): Int {
        return cache.getOrPut(matrixItem.id) {
            ContextCompat.getColor(
                context,
                when (matrixItem) {
                    is MatrixItem.UserItem -> getColorFromUserId(matrixItem.id)
                    else -> getColorFromRoomId(matrixItem.id)
                }
            )
        }
    }

    companion object {
        @ColorRes
        @VisibleForTesting
        fun getColorFromUserId(userId: String?): Int {
            var hash = 0

            userId?.toList()?.map { chr -> hash = (hash shl 5) - hash + chr.code }

            return when (abs(hash) % 8) {
                1 -> R.color.username_2
                2 -> R.color.username_3
                3 -> R.color.username_4
                4 -> R.color.username_5
                5 -> R.color.username_6
                6 -> R.color.username_7
                7 -> R.color.username_8
                else -> R.color.username_1
            }
        }

        @ColorRes
        private fun getColorFromRoomId(roomId: String?): Int {
            return when ((roomId?.toList()?.sumOf { it.code } ?: 0) % 3) {
                1 -> R.color.avatar_fill_2
                2 -> R.color.avatar_fill_3
                else -> R.color.avatar_fill_1
            }
        }
    }
}