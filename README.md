# SourceAFIS for Java #

[![Maven Central](https://img.shields.io/maven-central/v/com.machinezoo.sourceafis/sourceafis)](https://search.maven.org/artifact/com.machinezoo.sourceafis/sourceafis)
[![Build Status](https://travis-ci.com/robertvazan/sourceafis-java.svg?branch=master)](https://travis-ci.com/robertvazan/sourceafis-java)
[![Coverage Status](https://codecov.io/gh/robertvazan/sourceafis-java/branch/master/graph/badge.svg)](https://codecov.io/gh/robertvazan/sourceafis-java)

SourceAFIS is a fingerprint recognition engine that takes a pair of human fingerprint images and returns their similarity score.
It can do 1:1 comparisons as well as efficient 1:N search. This is the Java implementation of the SourceAFIS algorithm.

* Documentation: [SourceAFIS for Java](https://sourceafis.machinezoo.com/java), [Javadoc](https://sourceafis.machinezoo.com/javadoc/com/machinezoo/sourceafis/package-summary.html), [SourceAFIS overview](https://sourceafis.machinezoo.com/), [Algorithm](https://sourceafis.machinezoo.com/algorithm)
* Download: see [SourceAFIS for Java](https://sourceafis.machinezoo.com/java) page
* Sources: [GitHub](https://github.com/robertvazan/sourceafis-java), [Bitbucket](https://bitbucket.org/robertvazan/sourceafis-java)
* Issues: [GitHub](https://github.com/robertvazan/sourceafis-java/issues), [Bitbucket](https://bitbucket.org/robertvazan/sourceafis-java/issues)
* License: [Apache License 2.0](LICENSE)

```java
FingerprintTemplate probe = new FingerprintTemplate(
	new FingerprintImage(
		Files.readAllBytes(Paths.get("probe.jpeg")),
		new FingerprintImageOptions()
			.dpi(500)));
FingerprintTemplate candidate = new FingerprintTemplate(
	new FingerprintImage(
		Files.readAllBytes(Paths.get("candidate.jpeg")),
		new FingerprintImageOptions()
			.dpi(500)));
double score = new FingerprintMatcher(probe)
	.match(candidate);
boolean matches = score >= 40;
```
