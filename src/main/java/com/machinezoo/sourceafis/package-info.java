// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
/**
 * This package contains classes implementing SourceAFIS fingerprint recognition algorithm in Java.
 * Fingerprint images are processed into {@link com.machinezoo.sourceafis.FingerprintTemplate} objects.
 * Probe template is then converted to {@link com.machinezoo.sourceafis.FingerprintMatcher}
 * and one or more candidate templates are fed to its {@link com.machinezoo.sourceafis.FingerprintMatcher#match(FingerprintTemplate)} method
 * to obtain similarity scores.
 * 
 * @see <a href="https://sourceafis.machinezoo.com/java">SourceAFIS for Java tutorial</a>
 */
package com.machinezoo.sourceafis;
