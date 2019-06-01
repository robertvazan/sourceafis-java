// Part of SourceAFIS: https://sourceafis.machinezoo.com
package com.machinezoo.sourceafis;

import org.slf4j.*;

enum ForeignMinutiaType {
	ENDING,
	BIFURCATION,
	EITHER,
	OTHER;
	private static final Logger logger = LoggerFactory.getLogger(ForeignMinutiaType.class);
	static ForeignMinutiaType decode(int code, ForeignFormat format) {
		switch (code) {
		case 0:
			if (format == ForeignFormat.ANSI_378_2004 || format == ForeignFormat.ISO_19794_2_2005)
				return OTHER;
			else
				return EITHER;
		case 1:
			return ENDING;
		case 2:
			return BIFURCATION;
		default:
			logger.warn("Bad template: minutia type code must be one of 01, 10, or 00");
			return ENDING;
		}
	}
	int encode(ForeignFormat format) {
		switch (this) {
		case ENDING:
			return 1;
		case BIFURCATION:
			return 2;
		case EITHER:
			if (format == ForeignFormat.ANSI_378_2004)
				throw new IllegalArgumentException("Cannot create template: cannot encode 'either' minutia in format supporting only 'other' type");
			return 0;
		case OTHER:
			if (format != ForeignFormat.ANSI_378_2004)
				throw new IllegalArgumentException("Cannot create template: cannot encode 'other' minutia in format supporting only 'either' type");
			return 0;
		default:
			throw new IllegalStateException();
		}
	}
	static ForeignMinutiaType convert(MinutiaType type) {
		switch (type) {
		case ENDING:
			return ENDING;
		case BIFURCATION:
			return BIFURCATION;
		default:
			throw new IllegalStateException();
		}
	}
	MinutiaType convert() {
		switch (this) {
		case ENDING:
			return MinutiaType.ENDING;
		case BIFURCATION:
			return MinutiaType.BIFURCATION;
		case EITHER:
		case OTHER:
			logger.debug("Imperfect template import: changing 'either' or 'other' minutia type to 'ending'");
			return MinutiaType.ENDING;
		default:
			throw new IllegalStateException();
		}
	}
}
