// Part of SourceAFIS for Java: https://sourceafis.machinezoo.com/java
package com.machinezoo.sourceafis;

import java.util.*;
import com.machinezoo.sourceafis.matcher.*;
import com.machinezoo.sourceafis.templates.*;

/**
 * Fingerprint template representation optimized for fast 1:N matching.
 * {@code FingerprintMatcher} maintains data structures that improve matching speed at the cost of some RAM.
 * It can efficiently match one probe fingerprint to many candidate fingerprints.
 * <p>
 * New matcher is created by passing probe fingerprint template to {@link #FingerprintMatcher(FingerprintTemplate)} constructor.
 * Candidate fingerprint templates are then passed one by one to {@link #match(FingerprintTemplate)} method.
 * 
 * @see <a href="https://sourceafis.machinezoo.com/java">SourceAFIS for Java tutorial</a>
 * @see FingerprintTemplate
 */
public class FingerprintMatcher {
	/*
	 * API roadmap:
	 * + FingerprintMatcher(FingerprintTemplate, FingerprintMatcherOptions)
	 * + compare(FingerprintTemplate) - returns match log-odds in bits instead of current score, may be negative
	 * - match(FingerprintTemplate)
	 * + maybe features to support 1:N identification (parallelization, score adjustment, person model, ...)
	 * 
	 * FingerprintMatcherOptions:
	 * + matchX(boolean) - enable or disable various parts of the matcher for performance reasons
	 * + processorBudget(double) - automated feature/algorithm selection to target "average" compute cost per candidate
	 * 
	 * FingerprintEvidence:
	 * = calculation of effective score in multi-finger or 1:N matching
	 * + add(double)
	 * + add(FingerprintPosition, double)
	 * + add(FingerprintEvidence)
	 * + top(int subset, int population)
	 * + sum()
	 * + thresholdAtFMR(double) - might have variant, still unclear
	 */
	private volatile ImmutableMatcher immutable = ImmutableMatcher.NULL;
	/**
	 * Creates fingerprint template representation optimized for fast 1:N matching.
	 * Once the probe template is processed, candidate templates can be compared to it
	 * by calling {@link #match(FingerprintTemplate)}.
	 * <p>
	 * This constructor is expensive in terms of RAM footprint and CPU usage.
	 * Initialized {@code FingerprintMatcher} should be reused for multiple {@link #match(FingerprintTemplate)} calls in 1:N matching.
	 * 
	 * @param probe
	 *            probe fingerprint template to be matched to candidate fingerprints
	 * @throws NullPointerException
	 *             if {@code probe} is {@code null}
	 * 
	 * @see #match(FingerprintTemplate)
	 */
	public FingerprintMatcher(FingerprintTemplate probe) {
		Objects.requireNonNull(probe);
		ImmutableTemplate template = probe.immutable;
		immutable = new ImmutableMatcher(template, EdgeHash.build(template));
	}
	/**
	 * @deprecated Use {@link #FingerprintMatcher(FingerprintTemplate)} constructor to fully initialize the matcher.
	 * 
	 * @see #FingerprintMatcher(FingerprintTemplate)
	 */
	@Deprecated
	public FingerprintMatcher() {
	}
	/**
	 * @deprecated Use thread-local instance of {@link FingerprintTransparency} instead.
	 * 
	 * @param transparency
	 *            target {@link FingerprintTransparency} or {@code null} to disable algorithm transparency
	 * @return {@code this} (fluent method)
	 * 
	 * @see FingerprintTransparency
	 */
	@Deprecated
	public FingerprintMatcher transparency(FingerprintTransparency transparency) {
		return this;
	}
	/**
	 * @deprecated Use {@link #FingerprintMatcher(FingerprintTemplate)} constructor to initialize the matcher.
	 * 
	 * @param probe
	 *            probe fingerprint template to be matched to candidate fingerprints
	 * @return {@code this} (fluent method)
	 * @throws NullPointerException
	 *             if {@code probe} is {@code null}
	 * 
	 * @see #FingerprintMatcher(FingerprintTemplate)
	 */
	@Deprecated
	public FingerprintMatcher index(FingerprintTemplate probe) {
		Objects.requireNonNull(probe);
		ImmutableTemplate template = probe.immutable;
		immutable = new ImmutableMatcher(template, EdgeHash.build(template));
		return this;
	}
	/**
	 * Matches candidate fingerprint to probe fingerprint and calculates similarity score.
	 * Candidate fingerprint in {@code candidate} parameter is matched to probe fingerprint
	 * previously passed to {@link #FingerprintMatcher(FingerprintTemplate)} constructor.
	 * <p>
	 * Returned similarity score is a non-negative number that increases with similarity between probe and candidate fingerprints.
	 * Application should compare the score to a threshold with expression {@code (score >= threshold)} to arrive at boolean match/non-match decision.
	 * Threshold 10 corresponds to FMR (False Match Rate, see <a href="https://en.wikipedia.org/wiki/Biometrics#Performance">Biometric Performance</a>
	 * and <a href="https://en.wikipedia.org/wiki/Confusion_matrix">Confusion matrix</a>) of 10%, threshold 20 to FMR 1%, threshold 30 to FMR 0.1%, and so on.
	 * <p>
	 * Recommended threshold is 40, which corresponds to FMR 0.01%.
	 * Correspondence between threshold and FMR is approximate and varies with quality of fingerprints being matched.
	 * Increasing threshold rapidly reduces FMR, but it also slowly increases FNMR (False Non-Match Rate).
	 * Threshold must be tailored to the needs of the application.
	 * <p>
	 * This method is thread-safe. Multiple threads can match candidates against single {@code FingerprintMatcher}.
	 * 
	 * @param candidate
	 *            fingerprint template to be matched with probe fingerprint represented by this {@code FingerprintMatcher}
	 * @return similarity score between probe and candidate fingerprints
	 * @throws NullPointerException
	 *             if {@code candidate} is {@code null}
	 */
	public double match(FingerprintTemplate candidate) {
		Objects.requireNonNull(candidate);
		MatcherThread thread = MatcherThread.current();
		thread.selectMatcher(immutable);
		thread.selectCandidate(candidate.immutable);
		return thread.match();
	}
}
