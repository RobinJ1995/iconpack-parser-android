package be.robinj.iconpack.utils;

import android.content.res.Resources;

/**
 * Created by nadavfima on 14/05/2017.
 */

public class ResourceHelper {
    public static int getDrawableResourceId(final Resources packResources, final String resName,
                                            final String iconPack) {
        try {
            return packResources.getIdentifier(resName, "drawable", iconPack);
        } catch (final Exception ex) {
        }

        return -1;
    }
}
