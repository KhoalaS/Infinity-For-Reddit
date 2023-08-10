package ml.docilealligator.infinityforreddit.ui


import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment

interface ToolbarConfigurable {

    fun AppCompatActivity.configureToolbar(toolbar: Toolbar, displayBack: Boolean = true) {
        setSupportActionBar(toolbar)
        supportActionBar?.let {
            it.setDisplayShowHomeEnabled(displayBack)
            it.setDisplayHomeAsUpEnabled(displayBack)
            it.title = null
        }
    }

    fun Fragment.configureToolbar(toolbar: Toolbar, displayBack: Boolean = true) {
        (activity as? AppCompatActivity)?.configureToolbar(toolbar, displayBack)
    }
}
