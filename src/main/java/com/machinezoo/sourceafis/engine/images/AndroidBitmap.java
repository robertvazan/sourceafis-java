// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.images;

import java.lang.reflect.*;
import com.machinezoo.noexception.*;

class AndroidBitmap {
	static Class<?> clazz = Exceptions.sneak().get(() -> Class.forName("android.graphics.Bitmap"));
	static Method getWidth = Exceptions.sneak().get(() -> clazz.getMethod("getWidth"));
	static Method getHeight = Exceptions.sneak().get(() -> clazz.getMethod("getHeight"));
	static Method getPixels = Exceptions.sneak().get(() -> clazz.getMethod("getPixels", int[].class, int.class, int.class, int.class, int.class, int.class, int.class));
	final Object instance;
	AndroidBitmap(Object instance) {
		this.instance = instance;
	}
	int getWidth() {
		return Exceptions.sneak().getAsInt(() -> (int)getWidth.invoke(instance));
	}
	int getHeight() {
		return Exceptions.sneak().getAsInt(() -> (int)getHeight.invoke(instance));
	}
	void getPixels(int[] pixels, int offset, int stride, int x, int y, int width, int height) {
		Exceptions.sneak().run(() -> getPixels.invoke(instance, pixels, offset, stride, x, y, width, height));
	}
}
