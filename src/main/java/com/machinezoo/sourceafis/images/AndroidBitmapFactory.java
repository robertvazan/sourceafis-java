// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.images;

import java.lang.reflect.*;
import com.machinezoo.noexception.*;

class AndroidBitmapFactory {
	static Class<?> clazz = Exceptions.sneak().get(() -> Class.forName("android.graphics.BitmapFactory"));
	static Method decodeByteArray = Exceptions.sneak().get(() -> clazz.getMethod("decodeByteArray", byte[].class, int.class, int.class));
	static AndroidBitmap decodeByteArray(byte[] data, int offset, int length) {
		return new AndroidBitmap(Exceptions.sneak().get(() -> decodeByteArray.invoke(null, data, offset, length)));
	}
}
