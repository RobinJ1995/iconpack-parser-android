package be.robinj.iconpack;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.WorkerThread;

import be.robinj.iconpack.exceptions.AppFilterNotLoadedException;
import be.robinj.iconpack.exceptions.XMLNotFoundException;
import be.robinj.iconpack.utils.XmlPullParserGenerator;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import be.robinj.iconpack.utils.ResourceHelper;

/**
 * Created by nadavfima on 14/05/2017.
 */

public class IconPack {

	private final String packageName;
	private final WeakReference<Context> contextReference;
	private final PackageManager packageManager;

	private String title;
	private Resources resources;
	private IconMasking masking;

	private HashMap<String, String> appFilterMap = new HashMap<>();
	private LinkedHashMap<String, ArrayList<IconDrawable>> drawableMap = new LinkedHashMap<>();

	private boolean loadedAppFilter;
	private boolean loadingAppFilter;
	private boolean loadedDrawableMap;
	private BitmapDrawable temporaryDrawable;

	public IconPack(final Context context, final String packageName)
			throws PackageManager.NameNotFoundException {
		super();

		this.contextReference = new WeakReference<>(context);
		this.packageName = packageName;
		this.packageManager = context.getPackageManager();

		this.initIconPack();
	}

	private void initIconPack() throws PackageManager.NameNotFoundException {
		final ApplicationInfo appInfo = packageManager.getApplicationInfo(packageName, PackageManager.GET_META_DATA);

		this.title = packageManager.getApplicationLabel(appInfo).toString();
		this.resources = packageManager.getResourcesForApplication(packageName);
	}

	/**
	 * @return Icon Pack's title as a @{@link String}
	 */
	public String getTitle() {
		return this.title;
	}

	/**
	 * Returns a @{@link Resources} object that contains the Icon Pack's resources (drawables and everything)
	 * <p>
	 * You basically don't really need that.
	 *
	 * @return
	 */
	public Resources getResources() {
		return this.resources;
	}

	/**
	 * Parsing the AppFilter.XML file.
	 *
	 * @param initMasking
	 */
	public void initAppFilter(final boolean initMasking)
			throws IOException, XmlPullParserException {
		this.loadingAppFilter = true;

		final XmlPullParser parser = XmlPullParserGenerator.getXmlPullParser(
				this.resources, this.packageName, Constants.FILE_APPFILTER);

		this.onLoadAppFilter(parser, initMasking);
		this.loadedAppFilter = true;

		this.loadingAppFilter = false;
	}

	/**
	 * Parse the Dawable.XML file.
	 */
	private void initDrawableMap() throws XmlPullParserException, IOException {
		final XmlPullParser parser = this.getDrawableXmlPullParser();

		this.onLoadDrawableMap(parser);

		this.loadedDrawableMap = true;
	}

	/**
	 * @return - @{@link XmlPullParser} of the drawable.xml from the icon pack
	 * @throws XMLNotFoundException
	 * @throws XmlPullParserException
	 */
	public XmlPullParser getDrawableXmlPullParser() throws XMLNotFoundException, XmlPullParserException {
		return XmlPullParserGenerator.getXmlPullParser(this.resources, this.packageName,
				Constants.FILE_DRAWABLE);
	}

	/**
	 * @param drawableName
	 * @return - specific Drawable Icon, not necessarily found in the appfilter.xml
	 */
	public Drawable getDrawableIconForName(final String drawableName) {
		return this.loadDrawable(drawableName);
	}


	/**
	 * This method will first try to look up the icon in the AppFilterMap.
	 * If no icon is found in the AppFilter, a masked icon will be generated for this app.
	 *
	 * @param context
	 * @param componentName
	 * @param maskFallback
	 * @return
	 */
	public Drawable getDefaultIconForPackage(final Context context,
											 final ComponentName componentName,
											 final boolean maskFallback) {
		final Intent intent = new Intent();
		intent.setComponent(componentName);
		final List<ResolveInfo> activites = context.getPackageManager()
				.queryIntentActivities(intent, PackageManager.GET_META_DATA);

		if (activites.size() == 0) {
			return null;
		}

		return this.getDefaultIconForPackage(activites.get(0), maskFallback);
	}

	/**
	 * This method will first try to look up the icon in the AppFilterMap.
	 * If no icon is found in the AppFilter, a masked icon will be generated for this app.
	 *
	 * @param info
	 * @param maskFallback
	 * @return
	 */
	public Drawable getDefaultIconForPackage(final ResolveInfo info, final boolean maskFallback) {
		if (! this.loadedAppFilter) {
			throw new AppFilterNotLoadedException();
		}

		final String appPackageName = info.activityInfo.packageName;
		final Drawable defaultIcon = info.loadIcon(this.packageManager);

		final Intent launchIntent = this.packageManager.getLaunchIntentForPackage(appPackageName);

		String componentName = null;
		if (launchIntent != null) {
			componentName = this.packageManager.getLaunchIntentForPackage(appPackageName)
					.getComponent().toString();
		}

		String drawableName = this.appFilterMap.get(componentName);

		if (drawableName != null) {
			final Drawable drawable = this.loadDrawable(drawableName);

			if (drawable != null) {
				return drawable;
			} else if (maskFallback) {
				return this.generateMaskedIcon(defaultIcon);
			}

			return defaultIcon;
		}

		// not found
		// try to get a resource with the component filename
		if (componentName != null) {
			int start = componentName.indexOf("{") + 1;
			int end = componentName.indexOf("}", start);
			if (end > start) {
				drawableName = componentName.substring(start, end).toLowerCase(Locale.getDefault())
						.replace(".", "_").replace("/", "_");

				if (ResourceHelper.getDrawableResourceId(resources, drawableName, packageName) > 0)
					return this.loadDrawable(drawableName);
			}
		}

		if (maskFallback && masking != null) {
			return this.generateMaskedIcon(defaultIcon);
		}

		return defaultIcon;
	}

	private void onLoadDrawableMap(final XmlPullParser parser)
			throws XmlPullParserException, IOException {
		int eventType = parser.getEventType();
		String currentTitle = "";

		while (eventType != XmlPullParser.END_DOCUMENT) {
			if (eventType == XmlPullParser.START_TAG) {
				if (Constants.CATEGORY.equals(parser.getName())) {
					currentTitle = onAddCategoryToMap(
							parser.getAttributeValue(null, Constants.TITLE));
				} else if (Constants.ITEM.equals(parser.getName())) {
					final String name = parser.getAttributeValue(null, Constants.DRAWABLE);

					this.onAddIconToCategory(currentTitle, name);
				}
			}

			eventType = parser.next();
		}
	}

	private void onAddIconToCategory(final String currentTitle, final String name) {
		final int id = ResourceHelper.getDrawableResourceId(resources, name, packageName);

		if (id > 0) {
			final IconDrawable icon = new IconDrawable(name, id);
			icon.setTitle(IconDrawable.replaceName(this.contextReference.get(),
					true, icon.getDrawableName()));
			this.drawableMap.get(currentTitle).add(icon);
		}
	}

	private String onAddCategoryToMap(final String title) {
		if (title != null && title.length() > 0) {
			this.drawableMap.put(title, new ArrayList<IconDrawable>());

			return title;
		}

		return "";
	}

	private void onLoadAppFilter(final XmlPullParser parser, final boolean initMasking)
			throws XmlPullParserException, IOException {
		if (parser != null) {
			int eventType = parser.getEventType();

			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (eventType == XmlPullParser.START_TAG) {
					if (initMasking) {
						this.onLoadMask(parser);
					}

					if (Constants.ITEM.equals(parser.getName())) {
						this.onLoadAppFilter(parser);
					}
				}

				eventType = parser.next();
			}
		}
	}

	private void onLoadAppFilter(final XmlPullParser parser) {
		String componentName = null;
		String drawableName = null;

		for (int i = 0; i < parser.getAttributeCount(); i++) {
			if (parser.getAttributeName(i).equals(Constants.COMPONENT)) {
				componentName = parser.getAttributeValue(i);
			} else if (parser.getAttributeName(i).equals(Constants.DRAWABLE)) {
				drawableName = parser.getAttributeValue(i);
			}
		}

		if (! this.appFilterMap.containsKey(componentName)) {
			this.appFilterMap.put(componentName, drawableName);
		}
	}

	@WorkerThread
	private void onLoadMask(final XmlPullParser parser) {
		if (Constants.BACKGROUND.equals(parser.getName())) {
			for (int i = 0; i < parser.getAttributeCount(); i++) {
				if (parser.getAttributeName(i).startsWith(Constants.BACKGROUND_IMG)) {
					final String drawableName = parser.getAttributeValue(i);

					this.temporaryDrawable = this.loadDrawable(drawableName);
					if (this.temporaryDrawable != null) {
						final Bitmap iconback = this.temporaryDrawable.getBitmap();

						if (iconback != null) {
							if (this.masking == null) {
								this.masking = new IconMasking();
							}

							this.masking.addBackgroundBitmap(iconback);
						}
					}
				}
			}
		} else if (Constants.MASK.equals(parser.getName())) {
			if (parser.getAttributeCount() > 0
					&& Constants.IMG_1_VALUE.equals(parser.getAttributeName(0))) {
				final String drawableName = parser.getAttributeValue(0);

				if (this.masking == null) {
					this.masking = new IconMasking();
				}

				this.temporaryDrawable = loadDrawable(drawableName);
				if (this.temporaryDrawable != null) {
					this.masking.setMaskBitmap(this.temporaryDrawable.getBitmap());
				}
			}
		} else if (Constants.FRONT.equals(parser.getName())) {
			if (parser.getAttributeCount() > 0
					&& Constants.IMG_1_VALUE.equals(parser.getAttributeName(0))) {
				final String drawableName = parser.getAttributeValue(0);

				if (this.masking == null) {
					this.masking = new IconMasking();
				}

				this.temporaryDrawable = this.loadDrawable(drawableName);

				if (this.temporaryDrawable != null) {
					this.masking.setFrontBitmap(this.temporaryDrawable.getBitmap());
				}
			}
		} else if (Constants.SCALE.equals(parser.getName())) {
			// mFactor
			if (parser.getAttributeCount() > 0
					&& Constants.FACTOR.equals(parser.getAttributeName(0))) {

				if (this.masking == null) {
					this.masking = new IconMasking();
				}

				float factor = IconMasking.DEFAULT_FACTOR;

				try {
					factor = Float.parseFloat(parser.getAttributeValue(0));
				} catch (final NumberFormatException ex) {
					ex.printStackTrace();
				}

				this.masking.setFactor(factor);
			}
		}
	}

	@WorkerThread
	private BitmapDrawable loadDrawable(final String drawableName) {
		final int id = ResourceHelper.getDrawableResourceId(resources, drawableName, packageName);

		if (id <= 0) {
			return null;
		}

		if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
			return (BitmapDrawable) this.resources.getDrawable(id);
		}

		return (BitmapDrawable) this.resources.getDrawable(id, this.contextReference.get().getTheme());
	}

	private Drawable generateMaskedIcon(final Drawable defaultIcon) {
		final Bitmap defaultBitmap = ((BitmapDrawable) defaultIcon).getBitmap();

		// if no support images in the icon pack return the bitmap itself
		final List<Bitmap> backgroundImages = masking.getBackgroundImages();

		if (backgroundImages.size() == 0) {
			return defaultIcon;
		}

		final Random random = new Random();
		final int backImageInd = random.nextInt(backgroundImages.size());
		final Bitmap backImage = backgroundImages.get(backImageInd);
		final int w = backImage.getWidth();
		final int h = backImage.getHeight();

		// create a bitmap for the result
		final Bitmap result = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
		final Canvas canvas = new Canvas(result);

		// draw the background first
		canvas.drawBitmap(backImage, 0, 0, null);

		// create a mutable mask bitmap with the same mask
		final Bitmap scaledBitmap;
		if (defaultBitmap.getWidth() > w || defaultBitmap.getHeight() > h) {
			final float factor = masking.getFactor();

			scaledBitmap = Bitmap.createScaledBitmap(
					defaultBitmap,(int) (w * factor), (int) (h * factor), false);
		} else {
			scaledBitmap = Bitmap.createBitmap(defaultBitmap);
		}

		if (this.masking.maskImage != null) {
			// draw the scaled bitmap with mask
			final Bitmap mutableMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			final Canvas maskCanvas = new Canvas(mutableMask);
			maskCanvas.drawBitmap(masking.maskImage, 0, 0, new Paint());

			// paint the bitmap with mask into the result
			final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_OUT));
			canvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth()) / 2, (h - scaledBitmap.getHeight()) / 2, null);
			canvas.drawBitmap(mutableMask, 0, 0, paint);
			paint.setXfermode(null);
		} else {
			// draw the scaled bitmap with the back image as mask
			final Bitmap mutableMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
			final Canvas maskCanvas = new Canvas(mutableMask);
			maskCanvas.drawBitmap(backImage, 0, 0, new Paint());

			// paint the bitmap with mask into the result
			final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
			paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
			canvas.drawBitmap(scaledBitmap, (w - scaledBitmap.getWidth()) / 2, (h - scaledBitmap.getHeight()) / 2, null);
			canvas.drawBitmap(mutableMask, 0, 0, paint);
			paint.setXfermode(null);
		}

		if (this.masking.frontImage != null) {
			// paint the front
			canvas.drawBitmap(this.masking.frontImage, 0, 0, null);
		}

		return new BitmapDrawable(this.resources, result);
	}

	public String getPackageName() {
		return this.packageName;
	}

	public boolean isAppFilterLoaded() {
		return this.loadedAppFilter;
	}

	public boolean isLoadingAppFilter() {
		return this.loadingAppFilter;
	}
}
