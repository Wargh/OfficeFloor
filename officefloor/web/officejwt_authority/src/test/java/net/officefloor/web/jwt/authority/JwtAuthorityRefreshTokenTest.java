package net.officefloor.web.jwt.authority;

import java.security.Key;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.officefloor.compile.impl.structure.ManagedObjectSourceNodeImpl;
import net.officefloor.compile.test.issues.MockCompilerIssues;
import net.officefloor.web.jwt.key.AesSynchronousKeyFactory;
import net.officefloor.web.jwt.repository.JwtRefreshKey;

/**
 * Tests the {@link JwtAuthority} implementation for refresh tokens.
 * 
 * @author Daniel Sagenschneider
 */
public class JwtAuthorityRefreshTokenTest extends AbstractJwtAuthorityTokenTest {

	/**
	 * Ensure the encrypt/decrypt works as expected.
	 */
	public void testAes() throws Exception {

		ObjectMapper mapper = new ObjectMapper();
		String message = mapper.writeValueAsString(new MockClaims());

		// Generate the random values
		String initVector = JwtAuthorityManagedObjectSource.randomString(16, 16);
		String startSalt = JwtAuthorityManagedObjectSource.randomString(5, 25);
		String endSalt = JwtAuthorityManagedObjectSource.randomString(5, 25);
		String lace = JwtAuthorityManagedObjectSource.randomString(80, 100);
		System.out.println("Init Vector: " + initVector + " (" + initVector.length() + "), Start Salt: " + startSalt
				+ ", Lace: " + lace + " (" + lace.length() + ")," + "(" + startSalt.length() + "), End Salt: " + endSalt
				+ "(" + endSalt.length() + ")");

		// Encrypt and decrypt
		String encrypted = JwtAuthorityManagedObjectSource.encrypt(refreshKey, initVector, startSalt, lace, endSalt,
				message, mockCipherFactory);
		String decrypted = JwtAuthorityManagedObjectSource.decrypt(refreshKey, initVector, startSalt, endSalt,
				encrypted, mockCipherFactory);

		// Indicate values
		System.out.println("encrypted: " + encrypted + "\ndecrypted: " + decrypted);
		assertEquals("Should decrypt to plain text", message, decrypted);
	}

	/**
	 * Ensure able to generate refresh token.
	 */
	public void testCreateRefreshToken() {
		String refreshToken = this.createRefreshToken();
		this.identity.assertRefreshToken(refreshToken, this.mockRefreshKeys.get(0), mockCipherFactory);
	}

	/**
	 * Ensure issue if {@link JwtRefreshKey} expiration period is too short.
	 */
	public void testRefreshKeyExpirationTooShort() throws Exception {

		// Record issue in configuration
		this.compilerIssues = new MockCompilerIssues(this);
		this.compilerIssues.recordCaptureIssues(false);
		this.compilerIssues.recordIssue("JWT_AUTHORITY", ManagedObjectSourceNodeImpl.class, "Failed to init",
				new IllegalArgumentException(
						"JwtRefreshKey expiration period (8 seconds) is below overlap period ((1 seconds period * 4 periods = 4 seconds) * 2 for overlap start/end = 8 seconds)"));

		// Ensure issue if key period too short (no overlap buffer)
		this.replayMockObjects();
		this.loadOfficeFloor(JwtAuthorityManagedObjectSource.PROPERTY_REFRESH_TOKEN_EXPIRATION_PERIOD,
				String.valueOf(1), JwtAuthorityManagedObjectSource.PROPERTY_REFRESH_KEY_OVERLAP_PERIODS,
				String.valueOf(4), JwtAuthorityManagedObjectSource.PROPERTY_REFRESH_KEY_EXPIRATION_PERIOD,
				String.valueOf(8));
		this.verifyMockObjects();
		assertNull("Should not compile OfficeFloor", this.officeFloor);
	}

	/**
	 * Ensure issue if invalid identity object.
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testInvalidIdentity() {
		RefreshTokenException exception = this.doAuthorityTest((authority) -> {
			try {
				((JwtAuthority) authority).createRefreshToken("Invalid identity");
				return null;
			} catch (RefreshTokenException ex) {
				return ex;
			}
		});
		assertNotNull("Should not successfully create access token", exception);
		assertEquals("Incorrect cause", IllegalArgumentException.class.getName() + ": Identity was "
				+ String.class.getName() + " but required to be " + MockIdentity.class.getName(),
				exception.getMessage());
	}

	/**
	 * Ensure issue if refresh token not a JSON object.
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void testInvalidRefreshToken() {
		RefreshTokenException exception = this.doAuthorityTest((authority) -> {
			try {
				JwtAuthority configured = authority;
				configured.createRefreshToken("not an object");
				return null;
			} catch (RefreshTokenException ex) {
				return ex;
			}
		}, JwtAuthorityManagedObjectSource.PROPERTY_IDENTITY_CLASS, String.class.getName());
		assertNotNull("Should not successfully create access token", exception);
		assertEquals("Incorrect cause",
				IllegalArgumentException.class.getName()
						+ ": Must be JSON object (start end with {}) - but was \"not an object\"",
				exception.getMessage());
	}

	/**
	 * Ensure default the exp time.
	 */
	public void testDefaultPeriodFromNow() {
		this.identity.nbf = null;
		this.identity.exp = null;
		String refreshToken = this.createRefreshToken();
		this.identity.exp = mockCurrentTime + JwtAuthorityManagedObjectSource.DEFAULT_REFRESH_TOKEN_EXPIRATION_PERIOD;
		this.identity.assertRefreshToken(refreshToken, mockJwtRefreshKey, mockCipherFactory);
	}

	/**
	 * Ensure nbf is always before exp.
	 */
	public void testInvalidIdentityTimes() {
		this.identity.nbf = this.identity.exp + 1;
		this.assertInvalidRefreshToken(IllegalArgumentException.class.getName() + ": nbf (" + this.identity.nbf
				+ ") must not be after exp (" + this.identity.exp + ")");
	}

	/**
	 * Ensure fails if creating refresh token instances in the past.
	 */
	public void testFailOnAttemptingPastRefreshToken() {
		this.identity.nbf = mockCurrentTime - JwtAuthorityManagedObjectSource.DEFAULT_REFRESH_TOKEN_EXPIRATION_PERIOD;
		this.identity.exp = mockCurrentTime;
		this.assertInvalidRefreshToken(this.identity.getInvalidRefreshTokenCause());
	}

	/**
	 * Ensure fails if creating refresh token too far into future.
	 */
	public void testFailOnAttemptingRefreshTokenTooFarIntoFuture() {
		JwtRefreshKey key = this.mockRefreshKeys.get(1);
		this.identity.exp = key.getExpireTime();
		this.identity.nbf = this.identity.exp - JwtAuthorityManagedObjectSource.DEFAULT_REFRESH_TOKEN_EXPIRATION_PERIOD;
		this.assertInvalidRefreshToken(this.identity.getInvalidRefreshTokenCause());
	}

	/**
	 * Ensure if refresh token spans longer than {@link JwtRefreshKey} period that
	 * invalid.
	 */
	public void testFailOnAttemptingRefreshTokenWithTooLongerPeriod() {
		this.identity.exp = JwtAuthorityManagedObjectSource.DEFAULT_REFRESH_KEY_EXPIRATION_PERIOD + 1;
		this.assertInvalidRefreshToken(this.identity.getInvalidRefreshTokenCause());
	}

	/**
	 * Ensure creates the {@link JwtRefreshKey} instances (should none be available
	 * at start up).
	 */
	public void testNoRefreshKeysOnStartup() {

		// Clear keys and start server (should generate keys)
		this.mockRefreshKeys.clear();
		String refreshToken = this.createRefreshToken();

		// Ensure correct request time loaded
		assertEquals("Incorrect request time for keys",
				mockCurrentTime - JwtAuthorityManagedObjectSource.DEFAULT_REFRESH_TOKEN_EXPIRATION_PERIOD,
				this.retrieveJwtRefreshKeysTime.getEpochSecond());

		// Determine default overlap time
		long overlapTime = JwtAuthorityManagedObjectSource.MINIMUM_REFRESH_KEY_OVERLAP_PERIODS
				* JwtAuthorityManagedObjectSource.DEFAULT_REFRESH_TOKEN_EXPIRATION_PERIOD;

		// Should generate keys
		assertEquals("Should generate new keys", 2, this.mockRefreshKeys.size());
		JwtRefreshKey firstKey = this.mockRefreshKeys.get(0);
		long firstKeyStart = mockCurrentTime - overlapTime;
		assertEquals("Incorrect first key start", firstKeyStart, firstKey.getStartTime());
		assertEquals("Incorrect first key expire",
				firstKeyStart + JwtAuthorityManagedObjectSource.DEFAULT_REFRESH_KEY_EXPIRATION_PERIOD,
				firstKey.getExpireTime());

		// Ensure able to use new key
		this.identity.assertRefreshToken(refreshToken, firstKey, mockCipherFactory);

		// Ensure second key overlaps
		long secondKeyStart = firstKeyStart + JwtAuthorityManagedObjectSource.DEFAULT_REFRESH_KEY_EXPIRATION_PERIOD
				- overlapTime;
		JwtRefreshKey secondKey = this.mockRefreshKeys.get(1);
		assertEquals("Incorrect second key start", secondKeyStart, secondKey.getStartTime());
		assertEquals("Incorrect second key expire",
				secondKeyStart + JwtAuthorityManagedObjectSource.DEFAULT_REFRESH_KEY_EXPIRATION_PERIOD,
				secondKey.getExpireTime());
	}

	/**
	 * Ensure creates new key as required on refresh.
	 */
	public void testCreateNextRefreshKey() {

		// Obtain the refresh token
		String refreshToken = this.createRefreshToken();
		this.identity.assertRefreshToken(refreshToken, mockJwtRefreshKey, mockCipherFactory);
		assertEquals("Should just be the two setup keys", 2, this.mockRefreshKeys.size());

		// Move time forward and refresh (so loads new key)
		long renewKeyTime = mockCurrentTime + JwtAuthorityManagedObjectSource.DEFAULT_REFRESH_KEY_EXPIRATION_PERIOD;
		this.clockFactory.setCurrentTimeSeconds(renewKeyTime);
		this.doAuthorityTest((authority) -> {
			authority.reloadRefreshKeys();

			// Create refresh token (waits for keys to be reloaded)
			return authority.createRefreshToken(this.identity);
		});

		// Ensure now have three keys
		assertEquals("Should create new key (as old about to expire)", 3, this.mockRefreshKeys.size());

		// Third key should appropriately overlap second key
		JwtRefreshKey secondKey = this.mockRefreshKeys.get(1);
		long expectedStartTime = secondKey.getExpireTime()
				- (JwtAuthorityManagedObjectSource.MINIMUM_REFRESH_KEY_OVERLAP_PERIODS
						* JwtAuthorityManagedObjectSource.DEFAULT_REFRESH_TOKEN_EXPIRATION_PERIOD);
		JwtRefreshKey newKey = this.mockRefreshKeys.get(2);
		assertEquals("Incorrect new key start", expectedStartTime, newKey.getStartTime());
		assertEquals("Incorrect new key expire",
				expectedStartTime + JwtAuthorityManagedObjectSource.DEFAULT_REFRESH_KEY_EXPIRATION_PERIOD,
				newKey.getExpireTime());

		// Ensure as time moved forward (that uses second key to encode)
		this.identity.nbf = null;
		this.identity.exp = null;
		refreshToken = this.createRefreshToken();
		this.identity.exp = renewKeyTime + JwtAuthorityManagedObjectSource.DEFAULT_REFRESH_TOKEN_EXPIRATION_PERIOD;
		this.identity.assertRefreshToken(refreshToken, secondKey, mockCipherFactory);
	}

	/**
	 * Ensure can decode refresh token.
	 */
	public void testDecodeRefreshToken() {
		MockJwtRefreshKey key = new MockJwtRefreshKey(mockJwtRefreshKey);
		String refreshToken = key.createRefreshToken(this.identity, mockCipherFactory);
		MockIdentity decoded = this.decodeRefreshToken(refreshToken);
		this.identity.assertEquals(decoded);
	}

	/**
	 * Ensure can round trip the identity via refresh token.
	 */
	public void testRefreshTokenRoundTrip() {
		String refreshToken = this.createRefreshToken();
		MockIdentity decoded = this.decodeRefreshToken(refreshToken);
		this.identity.assertEquals(decoded);
	}

	/**
	 * Ensure handle non-base64 refresh token.
	 */
	public void testNonBase64RefreshToken() {
		this.assertInvalidDecodeRefreshToken("NotBase64 - !@#$%^");
	}

	/**
	 * Ensure handle invalid refresh token.
	 */
	public void testInvalidDecodeRefreshToken() {
		this.assertInvalidDecodeRefreshToken("invalid");
	}

	/**
	 * Ensure not match if wrong start salt.
	 */
	public void testWrongStartSalt() {
		this.assertInvalidDecodeRefreshToken((key) -> key.startSalt = "wrong");
	}

	/**
	 * Ensure not match if wrong lace.
	 */
	public void testWrongLace() {
		this.assertInvalidDecodeRefreshToken((key) -> key.lace = "wrong");
	}

	/**
	 * Ensure not match if wrong end salt.
	 */
	public void testWrongEndSalt() {
		this.assertInvalidDecodeRefreshToken((key) -> key.endSalt = "wrong");
	}

	/**
	 * Ensure not match if wrong {@link Key}.
	 */
	public void testWrongKey() {
		this.assertInvalidDecodeRefreshToken((key) -> {
			try {
				key.key = new AesSynchronousKeyFactory().createSynchronousKey();
			} catch (Exception ex) {
				throw fail(ex);
			}
		});
	}

}