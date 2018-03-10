# SourceAFIS for Java #

SourceAFIS is a fingerprint recognition engine that takes a pair of human fingerprint images and returns their similarity score.
It can do 1:1 comparisons as well as efficient 1:N search. This is the Java implementation of the SourceAFIS algorithm.

* [SourceAFIS overview](https://sourceafis.machinezoo.com/)
* [SourceAFIS for Java (download, tutorial)](https://sourceafis.machinezoo.com/java)
* [API documentation (javadoc)](https://sourceafis.machinezoo.com/javadoc/com/machinezoo/sourceafis/package-summary.html)
* [Source code (main repository)](https://bitbucket.org/robertvazan/sourceafis-java/src)
* [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0)

```java
byte[] probeImage = Files.readAllBytes(Paths.get("probe.jpeg"));
byte[] candidateImage = Files.readAllBytes(Paths.get("candidate.jpeg"));
FingerprintTemplate probe = new FingerprintTemplate()
	.dpi(500)
	.create(probeImage);
FingerprintTemplate candidate = new FingerprintTemplate()
	.dpi(500)
	.create(candidateImage);
double score = new FingerprintMatcher()
	.index(probe)
	.match(candidate);
boolean matches = score >= 40;
```
