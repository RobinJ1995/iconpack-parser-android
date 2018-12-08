package be.robinj.iconpack;

import android.content.Context;
import android.support.annotation.NonNull;

import com.robinj.iconpack.R;

/**
 * Created by nadavfima on 14/05/2017.
 */

public class IconDrawable {

    private final int resId;
    private final String drawableName;
    private String title;

    public IconDrawable(final String drawableName, final int resId) {
        this.drawableName = drawableName;
        this.resId = resId;
    }

    public static String replaceName(@NonNull final Context context, final boolean iconReplacer,
                                     String name) {
        if (iconReplacer) {
            final String[] replacer = context.getResources()
                    .getStringArray(R.array.icon_name_replacer);
            for (final String replace : replacer) {
                final String[] strings = replace.split(",");
                if (strings.length > 0) {
                    name = name.replace(strings[0], strings.length > 1 ? strings[1] : "");
                }
            }
        }

        name = name.replaceAll("_", " ");
        name = name.trim().replaceAll("\\s+", " ");

        char character = Character.toUpperCase(name.charAt(0));

        return character + name.substring(1);
    }

    public String getDrawableName() {
        return drawableName;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
