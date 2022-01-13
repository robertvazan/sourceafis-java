// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.templates;

import java.util.*;
import com.machinezoo.fingerprintio.*;

public abstract class TemplateCodec {
	public abstract byte[] encode(List<MutableTemplate> templates);
	public abstract List<MutableTemplate> decode(byte[] serialized, boolean strict);
	public List<MutableTemplate> decode(byte[] serialized) {
		try {
			return decode(serialized, true);
		} catch (Throwable ex) {
			return decode(serialized, false);
		}
	}
	public static final Map<TemplateFormat, TemplateCodec> ALL = new HashMap<>();
	static {
		ALL.put(TemplateFormat.ANSI_378_2004, new Ansi378v2004Codec());
		ALL.put(TemplateFormat.ANSI_378_2009, new Ansi378v2009Codec());
		ALL.put(TemplateFormat.ANSI_378_2009_AM1, new Ansi378v2009Am1Codec());
		ALL.put(TemplateFormat.ISO_19794_2_2005, new Iso19794p2v2005Codec());
		ALL.put(TemplateFormat.ISO_19794_2_2011, new Iso19794p2v2011Codec());
	}
}
