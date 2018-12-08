package be.robinj.iconpack.exceptions;

import java.io.IOException;

/**
 * Created by nadavfima on 14/05/2017.
 */

public class XMLNotFoundException extends IOException {
	public XMLNotFoundException() {
		super();
	}

	public XMLNotFoundException(final String message) {
		super(message);
	}

	public XMLNotFoundException(final String filename, final Throwable cause) {
		super(filename + " not found", cause);
	}

	public XMLNotFoundException(final Throwable cause) {
		super(cause);
	}
}
