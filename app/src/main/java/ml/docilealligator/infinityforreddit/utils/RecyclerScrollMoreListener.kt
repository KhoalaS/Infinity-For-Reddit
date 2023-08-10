package ml.docilealligator.infinityforreddit.utils


import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

private const val VISIBLE_THRESHOLD = 10

internal class RecyclerScrollMoreListener(
    private val layoutManager: LinearLayoutManager,
    private val onLoadMore: () -> Unit
) : RecyclerView.OnScrollListener() {

    private var previousTotalItemCount = 0

    override fun onScrolled(view: RecyclerView, dx: Int, dy: Int) {
        val totalItemCount = layoutManager.itemCount
        val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
        if (totalItemCount < previousTotalItemCount) {
            previousTotalItemCount = totalItemCount
        }
        if (totalItemCount > previousTotalItemCount) {
            previousTotalItemCount = totalItemCount
        }
        if (lastVisibleItemPosition + VISIBLE_THRESHOLD > totalItemCount) {
            onLoadMore()
        }
    }
}