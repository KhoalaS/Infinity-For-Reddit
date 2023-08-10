package ml.docilealligator.infinityforreddit.utils

import org.matrix.android.sdk.api.MatrixItemDisplayNameFallbackProvider
import org.matrix.android.sdk.api.util.MatrixItem

class MatrixItemDisplayNameFallbackProviderImpl : MatrixItemDisplayNameFallbackProvider {
    override fun getDefaultName(matrixItem: MatrixItem): String {
        return matrixItem.id;
    }
}