package be.robinj.iconpack;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by nadavfima on 14/05/2017.
 */

class IconMasking {
	static final float DEFAULT_FACTOR = 1.0f;

	List<Bitmap> backImages = new ArrayList<Bitmap>();
	Bitmap maskImage = null;
	Bitmap frontImage = null;

	private float factor = DEFAULT_FACTOR;


	public void addBackgroundBitmap(final Bitmap iconback) {
		this.backImages.add(iconback);
	}

	public void setMaskBitmap(final Bitmap bitmap) {
		this.maskImage = bitmap;
	}

	public void setFrontBitmap(final Bitmap bitmap) {
		this.frontImage = bitmap;
	}

	public void setFactor(final Float factor) {
		this.factor = factor;
	}

	public List<Bitmap> getBackgroundImages() {
		return this.backImages;
	}

	public float getFactor() {
		return this.factor;
	}
}
