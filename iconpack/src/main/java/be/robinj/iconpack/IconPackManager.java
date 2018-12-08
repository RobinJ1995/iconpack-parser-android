package be.robinj.iconpack;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

/**
 * Created by robin on 08/12/2018.
 */

public class IconPackManager {
	public IconPackManager() {
	}

	public IconPack getIconPack(final Context context, final String packageName)
			throws PackageManager.NameNotFoundException {
		final PackageManager pacMan = context.getPackageManager();
		final ResolveInfo resInf = pacMan.queryIntentActivities(
				new Intent(packageName), PackageManager.GET_META_DATA).get(0);

		return this.getIconPack(context, resInf);
	}

	public IconPack getIconPack(final Context context, final ResolveInfo resInf)
			throws PackageManager.NameNotFoundException {
		return new IconPack(context, resInf.activityInfo.packageName);
	}
}
