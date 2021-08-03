# SourceAFIS for D #

A D port of the SourceAFIS Java library.

This is a work in progress.

* Documentation: [SourceAFIS for Java](https://sourceafis.machinezoo.com/java), [Javadoc](https://sourceafis.machinezoo.com/javadoc/com/machinezoo/sourceafis/package-summary.html), [SourceAFIS overview](https://sourceafis.machinezoo.com/), [Algorithm](https://sourceafis.machinezoo.com/algorithm)
* Download: see [SourceAFIS for Java](https://sourceafis.machinezoo.com/java) page
* Sources: [GitHub](https://github.com/robertvazan/sourceafis-java), [Bitbucket](https://bitbucket.org/robertvazan/sourceafis-java)
* Issues: [GitHub](https://github.com/robertvazan/sourceafis-java/issues), [Bitbucket](https://bitbucket.org/robertvazan/sourceafis-java/issues)
* License: [Apache License 2.0](LICENSE)

```d
auto probe = new FingerprintTemplate(
	new FingerprintImage(
		cast(byte[]) "probe.jpeg".read(),
		new FingerprintImageOptions()
			.dpi(500)));
auto candidate = new FingerprintTemplate(
	new FingerprintImage(
		cast(byte[]) "candidate.jpeg".read()),
		new FingerprintImageOptions()
			.dpi(500)));
double score = new FingerprintMatcher(probe)
	.match(candidate);
boolean matches = score >= 40;
```
