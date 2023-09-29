package ml.docilealligator.infinityforreddit.markdown;

import android.content.Context;
import android.content.Intent;
import android.text.style.ClickableSpan;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import org.commonmark.node.Image;

import ml.docilealligator.infinityforreddit.activities.ViewImageOrGifActivity;
import io.noties.markwon.AbstractMarkwonPlugin;
import io.noties.markwon.MarkwonConfiguration;
import io.noties.markwon.MarkwonSpansFactory;
import io.noties.markwon.RenderProps;
import io.noties.markwon.image.AsyncDrawableSpan;
import io.noties.markwon.image.ImageSpanFactory;
public class ClickImagePlugin extends AbstractMarkwonPlugin {

    public static Context context;

    public ClickImagePlugin(Context context) {
        this.context = context;
    }

    public static ClickImagePlugin create(Context context) {
        return new ClickImagePlugin(context);
    }
    @Override
    public void configureSpansFactory(@NonNull MarkwonSpansFactory.Builder builder) {
        builder.setFactory(Image.class, new ClickableImageFactory());
    }

    class ClickableImageFactory extends ImageSpanFactory {
        @Override
        public Object getSpans(@NonNull MarkwonConfiguration configuration, @NonNull RenderProps props) {
            AsyncDrawableSpan image = (AsyncDrawableSpan) super.getSpans(configuration, props);
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(@NonNull View widget) {
                    Intent intent = new Intent(context, ViewImageOrGifActivity.class);
                    intent.putExtra(ViewImageOrGifActivity.EXTRA_IMAGE_URL_KEY, image.getDrawable().getDestination());
                    intent.putExtra(ViewImageOrGifActivity.EXTRA_FILE_NAME_KEY, image.getDrawable().getDestination());
                    context.startActivity(intent);
                }
            };
            return new Object[]{image, clickableSpan};
        }
    }


}