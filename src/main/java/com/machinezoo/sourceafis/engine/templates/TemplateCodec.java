// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis.engine.templates;

import java.util.*;
import com.machinezoo.fingerprintio.*;
import com.machinezoo.noexception.*;

public abstract class TemplateCodec {
	public abstract byte[] encode(List<FeatureTemplate> templates);
	public abstract List<FeatureTemplate> decode(byte[] serialized, ExceptionHandler handler);
	public static final Map<TemplateFormat, TemplateCodec> ALL = new HashMap<>();
	static {
		ALL.put(TemplateFormat.ANSI_378_2004, new Ansi378v2004Codec());
		ALL.put(TemplateFormat.ANSI_378_2009, new Ansi378v2009Codec());
		ALL.put(TemplateFormat.ANSI_378_2009_AM1, new Ansi378v2009Am1Codec());
		ALL.put(TemplateFormat.ISO_19794_2_2005, new Iso19794p2v2005Codec());
		ALL.put(TemplateFormat.ISO_19794_2_2011, new Iso19794p2v2011Codec());
	}
}
