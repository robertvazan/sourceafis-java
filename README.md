# SourceAFIS for Java #

SourceAFIS is a fingerprint recognition engine that takes a pair of human fingerprint images and returns their similarity score.
It can do 1:1 comparisons as well as efficient 1:N search. This is the Java implementation of the SourceAFIS algorithm.

```java
byte[] probeImage = Files.readAllBytes(Paths.get("probe.jpeg"));
byte[] candidateImage = Files.readAllBytes(Paths.get("candidate.jpeg"));
FingerprintTemplate probe = new FingerprintTemplate(probeImage);
FingerprintTemplate candidate = new FingerprintTemplate(candidateImage);
FingerprintMatcher matcher = new FingerprintMatcher(probe);
double score = matcher.match(candidate);
boolean match = score >= 40;
```

* [Homepage](https://sourceafis.machinezoo.com/) - Overview, tutorial, contact.
* [Download](https://sourceafis.machinezoo.com/download) - Maven package, JAR file, sample fingerprints.
* [Javadoc](https://sourceafis.machinezoo.com/javadoc/com/machinezoo/sourceafis/package-summary.html) - API reference documentation.
* [Sources](https://bitbucket.org/robertvazan/sourceafis-java/src) - Primary repository, preferred for pull requests.
* [License](https://www.apache.org/licenses/LICENSE-2.0) - Distributed under Apache License 2.0.

