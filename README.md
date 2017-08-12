# SourceAFIS for Java #

SourceAFIS is a fingerprint recognition engine that takes a pair of human fingerprint images and returns their match score.
It can do 1:1 comparisons as well as efficient 1:N search. This is the Java implementation of the SourceAFIS algorithm.

```java
FingerprintTemplate probe = new FingerprintTemplate(probeImage);
FingerprintTemplate candidate = new FingerprintTemplate(candidateImage);
FingerprintMatcher matcher = new FingerprintMatcher(probe);
double score = matcher.match(candidate);
```

* [Homepage](https://sourceafis.machinezoo.com/) - Overview, contact.
* [Sources](https://bitbucket.org/robertvazan/sourceafis-java/src) - Primary repository, preferred for pull requests.
