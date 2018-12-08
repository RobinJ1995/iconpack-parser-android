package be.robinj.iconpack.exceptions;

/**
 * Created by nadavfima on 05/11/2016.
 */
public class IconPacksNotFoundException extends RuntimeException {
	public IconPacksNotFoundException(final String s) {
		super(s + " Icon Pack not found");
	}
}
