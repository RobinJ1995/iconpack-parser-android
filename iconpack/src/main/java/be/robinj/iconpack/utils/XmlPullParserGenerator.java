package be.robinj.iconpack.utils;

import android.content.res.Resources;

import be.robinj.iconpack.exceptions.XMLNotFoundException;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.IOException;
import java.io.InputStream;


public class XmlPullParserGenerator {
	private static final String DEF_XML = "xml";
	private static final String EXT_XML = "." + DEF_XML;
	private static final String UTF_8 = "UTF-8";

	public static XmlPullParser getXmlPullParser(final Resources resources,
												 final String packageName, final String file)
			throws XMLNotFoundException, XmlPullParserException {
		XmlPullParser xpp = null;

		final int xmlId = resources.getIdentifier(file, DEF_XML, packageName);

		if (xmlId > 0) {
			xpp = resources.getXml(xmlId);
		} else {
			// no resource found, try to open it from assets folder
			try {
				final InputStream appfilterstream = resources.getAssets()
						.open(file + EXT_XML);

				final XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
				factory.setNamespaceAware(true);

				xpp = factory.newPullParser();
				xpp.setInput(appfilterstream, UTF_8);
			} catch (final IOException ex) {
				throw new XMLNotFoundException(file + EXT_XML, ex);
			}
		}

		return xpp;
	}
}
