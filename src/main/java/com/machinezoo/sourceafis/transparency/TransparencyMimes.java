// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.transparency;

import com.machinezoo.stagean.*;

@DraftCode("Use some existing MIME library.")
public class TransparencyMimes {
	/*
	 * Specifying MIME type of the data allows construction of generic transparency data consumers.
	 * For example, ZIP output for transparency data uses MIME type to assign file extension.
	 * It is also possible to create generic transparency data browser that changes visualization based on MIME type.
	 * 
	 * We will define short table mapping MIME types to file extensions, which is used by the ZIP implementation,
	 * but it is currently also used to support the old API that used file extensions.
	 * There are some MIME libraries out there, but no one was just right.
	 * There are also public MIME type lists, but they have to be bundled and then kept up to date.
	 * We will instead define only a short MIME type list covering data types we are likely to see here.
	 */
	public static String suffix(String mime) {
		switch (mime) {
		/*
		 * Our primary serialization format.
		 */
		case "application/cbor":
			return ".cbor";
		/*
		 * Plain text for simple records.
		 */
		case "text/plain":
			return ".txt";
		/*
		 * Common serialization formats.
		 */
		case "application/json":
			return ".json";
		case "application/xml":
			return ".xml";
		/*
		 * Image formats commonly used to encode fingerprints.
		 */
		case "image/jpeg":
			return ".jpeg";
		case "image/png":
			return ".png";
		case "image/bmp":
			return ".bmp";
		case "image/tiff":
			return ".tiff";
		case "image/jp2":
			return ".jp2";
		/*
		 * WSQ doesn't have a MIME type. We will invent one.
		 */
		case "image/x-wsq":
			return ".wsq";
		/*
		 * Fallback is needed, because there can be always some unexpected MIME type.
		 */
		default:
			return ".dat";
		}
	}
}
