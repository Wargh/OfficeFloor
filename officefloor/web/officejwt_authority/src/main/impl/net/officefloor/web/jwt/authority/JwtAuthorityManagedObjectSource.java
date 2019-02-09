package net.officefloor.web.jwt.authority;

import java.nio.charset.Charset;
import java.security.Key;
import java.security.KeyPair;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.jsonwebtoken.Jwts;
import net.officefloor.compile.properties.Property;
import net.officefloor.frame.api.build.None;
import net.officefloor.frame.api.clock.Clock;
import net.officefloor.frame.api.function.ManagedFunction;
import net.officefloor.frame.api.managedobject.ManagedObject;
import net.officefloor.frame.api.managedobject.source.ManagedObjectExecuteContext;
import net.officefloor.frame.api.managedobject.source.ManagedObjectFunctionBuilder;
import net.officefloor.frame.api.managedobject.source.ManagedObjectFunctionDependency;
import net.officefloor.frame.api.managedobject.source.ManagedObjectSource;
import net.officefloor.frame.api.managedobject.source.ManagedObjectSourceContext;
import net.officefloor.frame.api.managedobject.source.impl.AbstractManagedObjectSource;
import net.officefloor.frame.internal.structure.Flow;
import net.officefloor.plugin.managedobject.poll.StatePollContext;
import net.officefloor.plugin.managedobject.poll.StatePoller;
import net.officefloor.server.http.HttpStatus;
import net.officefloor.web.jwt.key.AesCipherFactory;
import net.officefloor.web.jwt.key.AesSynchronousKeyFactory;
import net.officefloor.web.jwt.key.AsynchronousKeyFactory;
import net.officefloor.web.jwt.key.CipherFactory;
import net.officefloor.web.jwt.key.Rsa256AynchronousKeyFactory;
import net.officefloor.web.jwt.key.SynchronousKeyFactory;
import net.officefloor.web.jwt.repository.JwtAccessKey;
import net.officefloor.web.jwt.repository.JwtAuthorityKey;
import net.officefloor.web.jwt.repository.JwtAuthorityRepository;
import net.officefloor.web.jwt.repository.JwtRefreshKey;
import net.officefloor.web.jwt.validate.JwtValidateKey;

/**
 * <p>
 * {@link JwtAuthority} {@link ManagedObjectSource}.
 * <p>
 * Key activation period is as follows:
 * <ol>
 * <li>Access/Refresh token - expire period</li>
 * <li>Encode access/refresh token keys - refreshed every expire period and key
 * must be active for minimum number of expire periods</li>
 * </ol>
 * 
 * @author Daniel Sagenschneider
 */
public class JwtAuthorityManagedObjectSource
		extends AbstractManagedObjectSource<None, JwtAuthorityManagedObjectSource.Flows> {

	/**
	 * {@link Flow} keys.
	 */
	public static enum Flows {
		RETRIEVE_ENCODE_KEYS, RETRIEVE_REFRESH_KEYS
	}

	/**
	 * Dependencies for {@link ManagedFunction} to retrieve the {@link JwtAccessKey}
	 * instances.
	 */
	private static enum RetrieveKeysDependencies {
		COLLECTOR, JWT_AUTHORITY_REPOSITORY
	}

	/**
	 * {@link Property} name for the expiration period for access token. Period
	 * measured in seconds.
	 */
	public static final String PROPERTY_ACCESS_TOKEN_EXPIRATION_PERIOD = "access.token.expiration.period";

	/**
	 * Default expiration period for access tokens.
	 */
	public static final long DEFAULT_ACCESS_TOKEN_EXPIRATION_PERIOD = TimeUnit.MINUTES.toSeconds(20);

	/**
	 * {@link Property} name for number of overlap access token periods for the
	 * {@link JwtAccessKey} instances.
	 */
	public static final String PROPERTY_ACCESS_KEY_OVERLAP_PERIODS = "access.key.token.overlap.periods";

	/**
	 * Minimum number of overlap access token periods for he {@link JwtAccessKey}
	 * instances.
	 */
	public static final int MINIMUM_ACCESS_KEY_OVERLAP_PERIODS = 3;

	/**
	 * {@link Property} name for the expiration period for the {@link JwtAccessKey}.
	 * Period measured in seconds.
	 */
	public static final String PROPERTY_ACCESS_KEY_EXPIRATION_PERIOD = "access.key.expiration.period";

	/**
	 * Default expiration period for {@link JwtAccessKey}.
	 */
	public static final long DEFAULT_ACCESS_KEY_EXPIRATION_PERIOD = TimeUnit.DAYS.toSeconds(7);

	/**
	 * {@link Property} for the {@link AsynchronousKeyFactory} {@link Class} for the
	 * {@link JwtAccessKey}.
	 */
	public static final String PROPERTY_ACCESS_TOKEN_KEY_FACTORY = "access.token.key.factory";

	/**
	 * Default {@link JwtAccessKey} {@link AsynchronousKeyFactory}.
	 */
	public static final String DEFAULT_ACCESS_TOKEN_KEY_FACTORY = Rsa256AynchronousKeyFactory.class.getName();

	/**
	 * {@link Property} name for the expiration period for refresh token. Period
	 * measured in seconds.
	 */
	public static final String PROPERTY_REFRESH_TOKEN_EXPIRATION_PERIOD = "refresh.token.expiration.period";

	/**
	 * Default expiration period for refresh tokens.
	 */
	public static final long DEFAULT_REFRESH_TOKEN_EXPIRATION_PERIOD = TimeUnit.HOURS.toSeconds(8);

	/**
	 * {@link Property} name for number of overlap refresh token periods for the
	 * {@link JwtRefreshKey} instances.
	 */
	public static final String PROPERTY_REFRESH_KEY_OVERLAP_PERIODS = "refresh.key.token.overlap.periods";

	/**
	 * Minimum number of overlap refresh token periods for he {@link JwtRefreshKey}
	 * instances.
	 */
	public static final int MINIMUM_REFRESH_KEY_OVERLAP_PERIODS = 3;

	/**
	 * {@link Property} name for the expiration period for the
	 * {@link JwtRefreshKey}. Period measured in seconds.
	 */
	public static final String PROPERTY_REFRESH_KEY_EXPIRATION_PERIOD = "refresh.key.expiration.period";

	/**
	 * Default expiration period for {@link JwtRefreshKey}.
	 */
	public static final long DEFAULT_REFRESH_KEY_EXPIRATION_PERIOD = TimeUnit.DAYS.toSeconds(28);

	/**
	 * {@link Property} for the {@link JwtRefreshKey} {@link CipherFactory}.
	 */
	public static final String PROPERTY_REFRESH_TOKEN_CIPHER_FACTORY = "refresh.token.cipher.factory";

	/**
	 * Default {@link JwtRefreshKey} {@link CipherFactory}.
	 */
	public static final String DEFAULT_REFRESH_TOKEN_CIPHER_FACTORY = AesCipherFactory.class.getName();

	/**
	 * {@link Property} for the {@link SynchronousKeyFactory} {@link Class} for the
	 * {@link JwtRefreshKey}.
	 */
	public static final String PROPERTY_REFRESH_TOKEN_KEY_FACTORY = "refresh.token.key.factory";

	/**
	 * Default {@link JwtRefreshKey} {@link SynchronousKeyFactory}.
	 */
	public static final String DEFAULT_REFRESH_TOKEN_KEY_FACTORY = AesSynchronousKeyFactory.class.getName();

	/**
	 * {@link Charset}.
	 */
	private static final Charset UTF8 = Charset.forName("UTF-8");

	/**
	 * {@link DateTimeFormatter} for writing out times.
	 */
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.RFC_1123_DATE_TIME
			.withZone(ZoneId.systemDefault());

	/**
	 * {@link ObjectMapper}.
	 */
	private static final ObjectMapper mapper = new ObjectMapper();

	/**
	 * {@link TimeWindow} {@link JavaType}.
	 */
	private static final JavaType timeWindowJavaType = mapper.constructType(TimeWindow.class);

	static {
		// Ensure JSON deserialising is valid
		if (!mapper.canDeserialize(timeWindowJavaType)) {
			throw new IllegalStateException("Unable to deserialize " + TimeWindow.class.getSimpleName());
		}
	}

	/**
	 * Generates a random string.
	 * 
	 * @param minLength Minimum length of the string.
	 * @param maxLength Maximum length of the string.
	 * @return Random string.
	 */
	public static String randomString(int minLength, int maxLength) {

		// Obtain the random
		Random random = ThreadLocalRandom.current();

		// Generate random length
		int length;
		if (minLength == maxLength) {
			length = maxLength;
		} else {
			do {
				length = random.nextInt(maxLength);
			} while (length < minLength);
		}

		// Generate the random string
		int increase = 1;
		for (;;) {
			byte[] bytes = new byte[length * increase];
			random.nextBytes(bytes);
			String value = Base64.getEncoder().encodeToString(bytes);
			if (value.length() >= length) {
				return value.substring(0, length);
			}
			increase++;
		}
	}

	/**
	 * Encrypts the value.
	 * 
	 * @param key           {@link Key}.
	 * @param initVector    Initialise vector.
	 * @param startSalt     Start salt.
	 * @param lace          Lace.
	 * @param endSalt       End salt.
	 * @param value         Value.
	 * @param cipherFactory {@link CipherFactory}.
	 * @return Encrypted value.
	 * @throws Exception If fails to encrypt value.
	 */
	public static String encrypt(Key key, String initVector, String startSalt, String lace, String endSalt,
			String value, CipherFactory cipherFactory) throws Exception {
		IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(UTF8));
		Cipher cipher = cipherFactory.createCipher();
		cipher.init(Cipher.ENCRYPT_MODE, key, iv);
		byte[] encrypted = cipher.doFinal((startSalt + laceString(value, lace) + endSalt).getBytes());
		return Base64.getUrlEncoder().encodeToString(encrypted);
	}

	/**
	 * Decrypts the value.
	 * 
	 * @param key           {@link Key}.
	 * @param initVector    Initialise vector.
	 * @param startSalt     Start salt.
	 * @param endSalt       End salt.
	 * @param encrypted     Encrypted value.
	 * @param cipherFactory {@link CipherFactory}.
	 * @return Plaintext value.
	 * @throws Exception If fails to decrypt value.
	 */
	public static String decrypt(Key key, String initVector, String startSalt, String endSalt, String encrypted,
			CipherFactory cipherFactory) throws Exception {
		IvParameterSpec iv = new IvParameterSpec(initVector.getBytes(UTF8));
		Cipher cipher = cipherFactory.createCipher();
		cipher.init(Cipher.DECRYPT_MODE, key, iv);
		byte[] original = cipher.doFinal(Base64.getUrlDecoder().decode(encrypted));
		String value = new String(original);
		value = value.substring(startSalt.length());
		value = value.substring(0, value.length() - endSalt.length());
		return unlaceString(value);
	}

	/**
	 * As the JWT claims is known string, this can reduce effectiveness of cipher.
	 * Therefore, randomly insert values so each section of the AES encryption is
	 * not derivable.
	 * 
	 * @param value Value.
	 * @param lace  Random data to lace the value.
	 * @return Laced value.
	 */
	public static String laceString(String value, String lace) {
		byte[] valueBytes = value.getBytes(UTF8);
		byte[] laceBytes = lace.getBytes(UTF8);
		byte[] laced = new byte[valueBytes.length * 2];
		for (int i = 0; i < valueBytes.length; i++) {
			int laceIndex = i % lace.length();
			byte laceByte = laceBytes[laceIndex];

			// Load the values
			int pairIndex = i * 2;
			laced[pairIndex] = laceByte;
			laced[pairIndex + 1] = valueBytes[i];
		}
		return Base64.getEncoder().encodeToString(laced);
	}

	/**
	 * Unlaces the laced value.
	 * 
	 * @param laced Laced value.
	 * @return Unlaced value.
	 */
	public static String unlaceString(String laced) {
		byte[] lacedBytes = Base64.getDecoder().decode(laced);
		byte[] value = new byte[lacedBytes.length / 2];
		for (int i = 0; i < value.length; i++) {
			value[i] = lacedBytes[(i * 2) + 1];
		}
		return new String(value, Charset.forName("UTF-8"));
	}

	/**
	 * Default time in seconds expire the access token.
	 */
	private long accessTokenExpirationPeriod;

	/**
	 * Time in seconds to expire the {@link JwtAccessKey}.
	 */
	private long accessKeyExpirationPeriod;

	/**
	 * Number of overlap access token periods for the {@link JwtAccessKey}
	 * instances.
	 */
	private int accessKeyOverlapPeriods;

	/**
	 * {@link JwtAccessKey} {@link AsynchronousKeyFactory}.
	 */
	private AsynchronousKeyFactory accessTokenKeyFactory;

	/**
	 * Default time in seconds expire the refresh token.
	 */
	private long refreshTokenExpirationPeriod;

	/**
	 * Time in seconds to expire the {@link JwtRefreshKey}.
	 */
	private long refreshKeyExpirationPeriod;

	/**
	 * Number of overlap refresh token periods for the {@link JwtRefreshKey}
	 * instances.
	 */
	private int refreshKeyOverlapPeriods;

	/**
	 * {@link JwtRefreshKey} {@link CipherFactory}.
	 */
	private CipherFactory refreshTokenCipherFactory;

	/**
	 * {@link JwtRefreshKey} {@link SynchronousKeyFactory}.
	 */
	private SynchronousKeyFactory refreshTokenKeyFactory;

	/**
	 * {@link Clock} to obtain time in seconds.
	 */
	private Clock<Long> timeInSeconds;

	/**
	 * {@link StatePoller} to keep the {@link JwtAccessKey} instances up to date
	 * with appropriate keys.
	 */
	private StatePoller<JwtAccessKey[], Flows> jwtEncodeKeys;

	/**
	 * {@link StatePoller} to keep the {@link JwtRefreshKey} instances up to date
	 * with appropriate keys.
	 */
	private StatePoller<JwtRefreshKey[], Flows> jwtRefreshKeys;

	/**
	 * {@link JwtAuthorityKey} retriever.
	 */
	@FunctionalInterface
	private static interface JwtAuthorityKeyRetriever<K extends JwtAuthorityKey> {

		/**
		 * Retrieve the {@link JwtAuthorityKey} instances.
		 * 
		 * @param loadTime   Load time.
		 * @param repository {@link JwtAuthorityRepository}.
		 * @return {@link JwtAuthorityKey} instances.
		 * @throws Exception If fails to retrieve the {@link JwtAuthorityKey} instances.
		 */
		K[] retrieveKeys(long loadTime, JwtAuthorityRepository repository) throws Exception;
	}

	/**
	 * {@link JwtAuthorityKey} saver.
	 */
	@FunctionalInterface
	private static interface JwtAuthorityKeySaver<K extends JwtAuthorityKey> {

		/**
		 * Saves the {@link JwtAuthorityKey} instances.
		 * 
		 * @param keys       {@link JwtAuthorityKey} instances to save.
		 * @param repository {@link JwtAuthorityRepository}.
		 * @throws Exception If fails to save the {@link JwtAuthorityKey} instances.
		 */
		void saveKeys(List<K> keys, JwtAuthorityRepository repository) throws Exception;
	}

	/**
	 * Creates a new {@link JwtAuthorityKey}.
	 */
	@FunctionalInterface
	private static interface JwtAuthorityKeyFactory<K extends JwtAuthorityKey> {

		/**
		 * Creates a new {@link JwtAuthorityKey}.
		 * 
		 * @param startTime Start time for the {@link JwtAuthorityKey}.
		 * @return New {@link JwtAuthorityKey}.
		 * @throws Exception If fails to create new {@link JwtAuthorityKey}.
		 */
		K createJwtAuthorityKey(long startTime) throws Exception;
	}

	/**
	 * Loads the {@link JwtAuthorityKey} instances to cover the up coming time
	 * period.
	 * 
	 * @param repository            {@link JwtAuthorityRepository}.
	 * @param tokenExpirationPeriod Token expiration period.
	 * @param overlapPeriods        Number of token expiration periods to overlap
	 *                              key period.
	 * @param keyExpirationPeriod   Key expiration period.
	 * @param keyRetriever          {@link JwtAuthorityKeyRetriever}.
	 * @param keySaver              {@link JwtAuthorityKeySaver}.
	 * @param keyFactory            {@link JwtAuthorityKeyFactory}.
	 * @return {@link JwtAuthorityKey} instances to cover the up coming time period.
	 * @throws Exception If fails to load the {@link JwtAuthorityKey} instances.
	 */
	private <K extends JwtAuthorityKey> K[] loadKeysForCoverage(JwtAuthorityRepository repository,
			long tokenExpirationPeriod, int overlapPeriods, long keyExpirationPeriod,
			JwtAuthorityKeyRetriever<K> keyRetriever, JwtAuthorityKeySaver<K> keySaver,
			JwtAuthorityKeyFactory<K> keyFactory) throws Exception {

		// Obtain time
		long currentTimeSeconds = this.timeInSeconds.getTime();

		// Obtain the reload time
		long reloadTime = currentTimeSeconds - tokenExpirationPeriod;

		// Obtain the active keys
		K[] activeKeys = keyRetriever.retrieveKeys(reloadTime, repository);

		// Determine minimum period to have active keys
		long overlapTime = tokenExpirationPeriod * overlapPeriods;
		long activeUntilTime = currentTimeSeconds - overlapTime + keyExpirationPeriod - overlapTime
				+ keyExpirationPeriod;

		// Determine if have coverage
		long coverageTime = this.calculateKeyCoverageUntil(currentTimeSeconds, activeUntilTime, activeKeys);
		if (coverageTime <= activeUntilTime) {

			// Keep track of the new keys (likely only one)
			List<K> newKeys = new ArrayList<>(1);

			// Period is not covered, so must create new keys
			repository.doClusterCriticalSection((contextRepository) -> {

				// As potential cluster lock, reload state
				K[] coverageKeys = keyRetriever.retrieveKeys(reloadTime, contextRepository);

				// Determine coverage (as may have changed keys)
				long coverage = this.calculateKeyCoverageUntil(currentTimeSeconds, activeUntilTime, coverageKeys);

				// Ensure full coverage (creating keys as necessary)
				while (coverage < activeUntilTime) {

					// Create the key
					long startTime = coverage - overlapTime;
					K newKey = keyFactory.createJwtAuthorityKey(startTime);

					// Include key
					newKeys.add(newKey);
					coverageKeys = Arrays.copyOf(coverageKeys, coverageKeys.length + 1);
					coverageKeys[coverageKeys.length - 1] = newKey;

					// Determine the new coverage
					coverage = this.calculateKeyCoverageUntil(coverage, activeUntilTime, coverageKeys);
				}

				// Save the new keys
				keySaver.saveKeys(newKeys, contextRepository);
			});

			// As keys saved, include in encode keys
			activeKeys = Arrays.copyOf(activeKeys, activeKeys.length + newKeys.size());
			for (int i = 0; i < newKeys.size(); i++) {
				activeKeys[activeKeys.length - newKeys.size() + i] = newKeys.get(i);
			}
		}

		// Return the keys
		return activeKeys;
	}

	/**
	 * Calculates the key coverage from start time until end time.
	 * 
	 * @param startTimeSeconds Start time in seconds since Epoch.
	 * @param endTimeSeconds   End time in seconds since Epoch.
	 * @param authorityKeys    Available {@link JwtAuthorityKey} instances.
	 * @return Time in seconds from start time to coverage by
	 *         {@link JwtAuthorityKey} instances.
	 */
	private long calculateKeyCoverageUntil(long startTimeSeconds, long endTimeSeconds,
			JwtAuthorityKey[] authorityKeys) {

		// Determine coverage
		long coverageTime = startTimeSeconds;
		long lastRunTime;
		do {
			long startTime = coverageTime;
			lastRunTime = coverageTime;
			NEXT_KEY: for (JwtAuthorityKey authorityKey : authorityKeys) {

				// Ignore if not overlaps at start
				if (authorityKey.getStartTime() > startTime) {
					continue NEXT_KEY; // does not cover start
				}

				// Determine if covers greater time
				if (authorityKey.getExpireTime() > coverageTime) {
					coverageTime = authorityKey.getExpireTime();
				}
			}

			// Loop until coverage (or no further coverage)
		} while ((coverageTime <= endTimeSeconds) && (lastRunTime != coverageTime));

		// Return the time covered until
		return coverageTime;
	}

	/**
	 * Tokenises the payload with the key.
	 */
	@FunctionalInterface
	private static interface Tokeniser<K extends JwtAuthorityKey> {

		/**
		 * Tokenises the payload with the key.
		 * 
		 * @param payload Payload.
		 * @param key     {@link JwtAuthorityKey}.
		 * @return Tokenised payload.
		 * @throws Exception If fails to tokenise the payload.
		 */
		String tokenise(String payload, K key) throws Exception;
	}

	/**
	 * Creates the token.
	 * 
	 * @param content               Content for the token.
	 * @param tokenExpirationPeriod Token expiration period.
	 * @param poller                {@link StatePoller} for the
	 *                              {@link JwtAuthorityKey} instances.
	 * @param exceptionFactory      Factory for {@link Exception}.
	 * @param tokeniser             {@link Tokeniser}.
	 * @return Token.
	 * @throws T If fails to create the token.
	 */
	private <K extends JwtAuthorityKey, T extends Exception> String createToken(Object content,
			long tokenExpirationPeriod, StatePoller<K[], ?> poller,
			BiFunction<HttpStatus, Exception, T> exceptionFactory, Tokeniser<K> tokeniser) throws T {

		// Easy access to source
		JwtAuthorityManagedObjectSource source = JwtAuthorityManagedObjectSource.this;

		// Obtain the payload
		String payload;
		try {
			payload = mapper.writeValueAsString(content).trim();
		} catch (Exception ex) {
			throw new AccessTokenException(ex);
		}

		// Determine if JSON object (and where last bracket)
		int lastBracketIndex = payload.lastIndexOf('}');
		if (lastBracketIndex != (payload.length() - 1)) {
			throw exceptionFactory.apply(null,
					new IllegalArgumentException("Must be JSON object (start end with {}) - but was " + payload));
		}

		// Determine time window for token
		TimeWindow timeWindow;
		try {
			timeWindow = mapper.readValue(payload, timeWindowJavaType);
		} catch (Exception ex) {
			throw exceptionFactory.apply(null, ex);
		}

		// Obtain the current time
		long currentTime = source.timeInSeconds.getTime();

		// Obtain the not before time
		long notBeforeTime = currentTime;
		if (timeWindow.nbf != null) {
			notBeforeTime = timeWindow.nbf;
		}

		// Obtain the expire time (and ensure token expires)
		long expireTime;
		if (timeWindow.exp != null) {
			expireTime = timeWindow.exp;

		} else {
			// No expire, so calculate the default expire time
			expireTime = currentTime + tokenExpirationPeriod;

			// Append the expire time
			payload = payload.substring(0, lastBracketIndex) + ",\"exp\":" + expireTime + "}";
		}

		// Ensure valid time period
		if (notBeforeTime > expireTime) {
			throw exceptionFactory.apply(null, new IllegalArgumentException(
					"nbf (" + notBeforeTime + ") must not be after exp (" + expireTime + ")"));
		}

		// Obtain the keys
		K[] keys;
		try {
			keys = poller.getState(1, TimeUnit.SECONDS);
		} catch (TimeoutException ex) {
			throw exceptionFactory.apply(HttpStatus.SERVICE_UNAVAILABLE, ex);
		}

		/*
		 * Find the most appropriate key:
		 * 
		 * - key must be active
		 * 
		 * - key must be active for so many default refreshes from now (allows key to be
		 * superseded by another).
		 * 
		 * - key is shortest time to expire (least risk if compromised)
		 */
		K selectedKey = null;
		long minimumExpireTime = expireTime + (2 * (expireTime - notBeforeTime));
		NEXT_KEY: for (K candidateKey : keys) {

			// Ensure key is active
			if (candidateKey.getStartTime() > notBeforeTime) {
				continue NEXT_KEY; // key not active
			}

			// Ensure key will not expire too early
			if (candidateKey.getExpireTime() < minimumExpireTime) {
				continue NEXT_KEY; // key expires too early
			}

			// Determine if shortest time
			if ((selectedKey != null) && (selectedKey.getExpireTime() < candidateKey.getExpireTime())) {
				continue NEXT_KEY; // expires later
			}

			// Use the key
			selectedKey = candidateKey;
		}

		// Ensure have key
		if (selectedKey == null) {

			// Trigger loading keys (to possibly generate new key)
			poller.poll();

			// Indicate no keys
			String notBeforeDateTime = dateTimeFormatter
					.format(Instant.ofEpochSecond(notBeforeTime).atZone(ZoneId.systemDefault()));
			String expireDateTime = dateTimeFormatter
					.format(Instant.ofEpochSecond(expireTime).atZone(ZoneId.systemDefault()));
			throw exceptionFactory.apply(null, new IllegalStateException(
					"No key available for encoding (nbf: " + notBeforeDateTime + ", exp: " + expireDateTime + ")"));
		}

		// Generate the token
		try {
			return tokeniser.tokenise(payload, selectedKey);
		} catch (Exception ex) {
			throw exceptionFactory.apply(null, ex);
		}
	}

	/*
	 * =================== ManagedObjectSource ====================
	 */

	@Override
	protected void loadSpecification(SpecificationContext context) {
		// No specification
	}

	@Override
	protected void loadMetaData(MetaDataContext<None, Flows> context) throws Exception {
		ManagedObjectSourceContext<Flows> sourceContext = context.getManagedObjectSourceContext();

		// Obtain access token properties
		this.accessTokenExpirationPeriod = Long.parseLong(sourceContext.getProperty(
				PROPERTY_ACCESS_TOKEN_EXPIRATION_PERIOD, String.valueOf(DEFAULT_ACCESS_TOKEN_EXPIRATION_PERIOD)));
		this.accessKeyExpirationPeriod = Long.parseLong(sourceContext.getProperty(PROPERTY_ACCESS_KEY_EXPIRATION_PERIOD,
				String.valueOf(DEFAULT_ACCESS_KEY_EXPIRATION_PERIOD)));
		this.accessKeyOverlapPeriods = Integer.parseInt(sourceContext.getProperty(PROPERTY_ACCESS_KEY_OVERLAP_PERIODS,
				String.valueOf(MINIMUM_ACCESS_KEY_OVERLAP_PERIODS)));

		// Obtain the refresh token properties
		this.refreshTokenExpirationPeriod = Long.parseLong(sourceContext.getProperty(
				PROPERTY_REFRESH_TOKEN_EXPIRATION_PERIOD, String.valueOf(DEFAULT_REFRESH_TOKEN_EXPIRATION_PERIOD)));
		this.refreshKeyExpirationPeriod = Long.parseLong(sourceContext.getProperty(
				PROPERTY_REFRESH_KEY_EXPIRATION_PERIOD, String.valueOf(DEFAULT_REFRESH_KEY_EXPIRATION_PERIOD)));
		this.refreshKeyOverlapPeriods = Integer.parseInt(sourceContext.getProperty(PROPERTY_REFRESH_KEY_OVERLAP_PERIODS,
				String.valueOf(MINIMUM_REFRESH_KEY_OVERLAP_PERIODS)));

		// Load the access key factory
		String asynchronousKeyFactoryClassName = sourceContext.getProperty(PROPERTY_ACCESS_TOKEN_KEY_FACTORY,
				DEFAULT_ACCESS_TOKEN_KEY_FACTORY);
		this.accessTokenKeyFactory = (AsynchronousKeyFactory) sourceContext.loadClass(asynchronousKeyFactoryClassName)
				.getDeclaredConstructor().newInstance();

		// Load the refresh cipher factory
		String cipherFactoryClassName = sourceContext.getProperty(PROPERTY_REFRESH_TOKEN_CIPHER_FACTORY,
				DEFAULT_REFRESH_TOKEN_CIPHER_FACTORY);
		this.refreshTokenCipherFactory = (CipherFactory) sourceContext.loadClass(cipherFactoryClassName)
				.getDeclaredConstructor().newInstance();

		// Load the refresh key factory
		String synchronousKeyFactoryClassName = sourceContext.getProperty(PROPERTY_REFRESH_TOKEN_KEY_FACTORY,
				DEFAULT_REFRESH_TOKEN_KEY_FACTORY);
		this.refreshTokenKeyFactory = (SynchronousKeyFactory) sourceContext.loadClass(synchronousKeyFactoryClassName)
				.getDeclaredConstructor().newInstance();

		// Ensure appropriate timings
		long overlapPeriod = this.accessKeyOverlapPeriods * this.accessTokenExpirationPeriod;
		long minimumEncodeKeyPeriod = 2 * overlapPeriod;
		if (this.accessKeyExpirationPeriod <= minimumEncodeKeyPeriod) {
			throw new IllegalArgumentException(
					JwtAccessKey.class.getSimpleName() + " expiration period (" + this.accessKeyExpirationPeriod
							+ " seconds) is below overlap period ((" + this.accessTokenExpirationPeriod
							+ " seconds period * " + this.accessKeyOverlapPeriods + " periods = " + overlapPeriod
							+ " seconds) * 2 for overlap start/end = " + minimumEncodeKeyPeriod + " seconds)");
		}

		// Load meta-data
		context.setObjectClass(JwtAuthority.class);
		context.setManagedObjectClass(JwtAuthorityManagedObject.class);

		// Configure flows
		context.addFlow(Flows.RETRIEVE_ENCODE_KEYS, JwtAccessKeyCollector.class);
		context.addFlow(Flows.RETRIEVE_REFRESH_KEYS, JwtRefreshKeyCollector.class);

		// Obtain the clock
		this.timeInSeconds = sourceContext.getClock((time) -> time);

		// Obtain the JWT Authority Repository dependency
		ManagedObjectFunctionDependency jwtAuthorityRepository = sourceContext
				.addFunctionDependency(JwtAuthorityRepository.class.getSimpleName(), JwtAuthorityRepository.class);

		// Function to handle retrieving encode keys
		ManagedObjectFunctionBuilder<RetrieveKeysDependencies, None> retrieveEncodeKeys = sourceContext
				.addManagedFunction(Flows.RETRIEVE_ENCODE_KEYS.name(), () -> (functionContext) -> {

					// Obtain the JWT authority repository
					JwtAccessKeyCollector collector = (JwtAccessKeyCollector) functionContext
							.getObject(RetrieveKeysDependencies.COLLECTOR);
					JwtAuthorityRepository repository = (JwtAuthorityRepository) functionContext
							.getObject(RetrieveKeysDependencies.JWT_AUTHORITY_REPOSITORY);

					// Obtain the keys
					JwtAccessKey[] encodeKeys = this.loadKeysForCoverage(repository, this.accessTokenExpirationPeriod,
							this.accessKeyOverlapPeriods, this.accessKeyExpirationPeriod, (loadTime, repo) -> {

								// Load keys from repository
								List<JwtAccessKey> keys = repo.retrieveJwtAccessKeys(Instant.ofEpochSecond(loadTime));

								// Keep only the valid keys
								JwtAccessKey[] validKeys = keys.stream().map((key) -> new JwtEncodeKeyImpl(key))
										.filter((key) -> key.isValid()).toArray(JwtAccessKey[]::new);

								// Return the valid keys
								return validKeys;

							},
							(newKeys, repo) -> {

								// Save the new keys
								repo.saveJwtEncodeKeys(newKeys.toArray(new JwtAccessKey[newKeys.size()]));

							}, (startTime) -> {

								// Create the JWT encode key
								long expireTime = startTime + this.accessKeyExpirationPeriod;
								KeyPair keyPair = this.accessTokenKeyFactory.createAsynchronousKeyPair();
								JwtEncodeKeyImpl newEncodeKey = new JwtEncodeKeyImpl(startTime, expireTime,
										keyPair.getPrivate(), keyPair.getPrivate());

								// Return the JWT encode key
								return newEncodeKey;
							});

					// Load the keys
					collector.setKeys(encodeKeys);

					// Nothing further
					return null;
				});
		retrieveEncodeKeys.linkParameter(RetrieveKeysDependencies.COLLECTOR, JwtAccessKeyCollector.class);
		retrieveEncodeKeys.linkObject(RetrieveKeysDependencies.JWT_AUTHORITY_REPOSITORY, jwtAuthorityRepository);
		sourceContext.getFlow(Flows.RETRIEVE_ENCODE_KEYS).linkFunction(Flows.RETRIEVE_ENCODE_KEYS.name());

		// Function to handle retrieving refresh keys
		ManagedObjectFunctionBuilder<RetrieveKeysDependencies, None> retrieveRefreshKeys = sourceContext
				.addManagedFunction(Flows.RETRIEVE_REFRESH_KEYS.name(), () -> (functionContext) -> {

					// Obtain the JWT authority repository
					JwtRefreshKeyCollector collector = (JwtRefreshKeyCollector) functionContext
							.getObject(RetrieveKeysDependencies.COLLECTOR);
					JwtAuthorityRepository repository = (JwtAuthorityRepository) functionContext
							.getObject(RetrieveKeysDependencies.JWT_AUTHORITY_REPOSITORY);

					// Obtain the keys
					JwtRefreshKey[] refreshKeys = this.loadKeysForCoverage(repository,
							this.refreshTokenExpirationPeriod, this.refreshKeyOverlapPeriods,
							this.refreshKeyExpirationPeriod, (loadTime, repo) -> {

								// Load keys from repository
								List<JwtRefreshKey> keys = repo.retrieveJwtRefreshKeys(Instant.ofEpochSecond(loadTime));

								// Keep only the valid keys
								JwtRefreshKey[] validKeys = keys.stream().map((key) -> new JwtRefreshKeyImpl(key))
										.filter((key) -> key.isValid()).toArray(JwtRefreshKey[]::new);

								// Return the valid keys
								return validKeys;

							},
							(newKeys, repo) -> {

								// Save the new keys
								repo.saveJwtRefreshKeys(newKeys.toArray(new JwtRefreshKey[newKeys.size()]));

							},
							(startTime) -> {

								// TODO create new key
								throw new UnsupportedOperationException("TODO implement create JwtRefreshKey");
							});

					// Load the keys
					collector.setKeys(refreshKeys);

					// Nothing further
					return null;
				});
		retrieveRefreshKeys.linkParameter(RetrieveKeysDependencies.COLLECTOR, JwtRefreshKeyCollector.class);
		retrieveRefreshKeys.linkObject(RetrieveKeysDependencies.JWT_AUTHORITY_REPOSITORY, jwtAuthorityRepository);
		sourceContext.getFlow(Flows.RETRIEVE_REFRESH_KEYS).linkFunction(Flows.RETRIEVE_REFRESH_KEYS.name());
	}

	@Override
	public void start(ManagedObjectExecuteContext<Flows> context) throws Exception {

		// Keep JWT encoding keys up to date
		this.jwtEncodeKeys = StatePoller
				.builder(JwtAccessKey[].class, Flows.RETRIEVE_ENCODE_KEYS, context,
						(pollContext) -> new JwtAuthorityManagedObject<>())
				.parameter((pollContext) -> new JwtEncodeCollectorImpl(pollContext)).identifier("JWT Encode Keys")
				.defaultPollInterval(this.accessTokenExpirationPeriod, TimeUnit.SECONDS).build();

		// Keep JWT refresh keys up to date
		this.jwtRefreshKeys = StatePoller
				.builder(JwtRefreshKey[].class, Flows.RETRIEVE_REFRESH_KEYS, context,
						(pollContext) -> new JwtAuthorityManagedObject<>())
				.parameter((pollContext) -> new JwtRefreshCollectorImpl(pollContext)).identifier("JWT Refresh Keys")
				.defaultPollInterval(this.refreshTokenExpirationPeriod, TimeUnit.SECONDS).build();

	}

	@Override
	protected ManagedObject getManagedObject() throws Throwable {
		return new JwtAuthorityManagedObject<>();
	}

	/**
	 * {@link JwtAuthority} {@link ManagedObject}.
	 */
	private class JwtAuthorityManagedObject<I> implements ManagedObject, JwtAuthority<I> {

		/*
		 * ==================== ManagedObject ======================
		 */

		@Override
		public Object getObject() throws Throwable {
			return this;
		}

		/*
		 * ==================== JwtAuthority =======================
		 */

		@Override
		public String createRefreshToken(I identity) throws RefreshTokenException {

			// Easy access to source
			JwtAuthorityManagedObjectSource source = JwtAuthorityManagedObjectSource.this;

			// Create the refresh token
			return source.createToken(identity, source.refreshTokenExpirationPeriod, source.jwtRefreshKeys,
					(status, ex) -> status != null ? new RefreshTokenException(status, ex)
							: new RefreshTokenException(ex),
					(payload, key) -> JwtAuthorityManagedObjectSource.encrypt(key.getKey(), key.getInitVector(),
							key.getStartSalt(), key.getLace(), key.getEndSalt(), payload,
							source.refreshTokenCipherFactory));
		}

		@Override
		public I decodeRefreshToken(String refreshToken) {
			// TODO implement JwtAuthority<C>.decodeRefreshToken(...)
			throw new UnsupportedOperationException("TODO implement JwtAuthority<C>.decodeRefreshToken(...)");
		}

		@Override
		public void reloadRefreshKeys() {
			// TODO implement JwtAuthority<I>.reloadRefreshKeys(...)
			throw new UnsupportedOperationException("TODO implement JwtAuthority<I>.reloadRefreshKeys(...)");
		}

		@Override
		public <C> String createAccessToken(C claims) {

			// Easy access to source
			JwtAuthorityManagedObjectSource source = JwtAuthorityManagedObjectSource.this;

			// Create the access token
			return source.createToken(claims, source.accessTokenExpirationPeriod, source.jwtEncodeKeys,
					(status, ex) -> status != null ? new AccessTokenException(status, ex)
							: new AccessTokenException(ex),
					(payload, key) -> Jwts.builder().signWith(key.getPrivateKey()).setPayload(payload).compact());
		}

		@Override
		public void reloadAccessKeys() {
			JwtAuthorityManagedObjectSource.this.jwtEncodeKeys.clear();
			JwtAuthorityManagedObjectSource.this.jwtEncodeKeys.poll();
		}

		@Override
		public JwtValidateKey[] getActiveJwtValidateKeys() {
			// TODO implement JwtAuthority<C>.getActiveJwtDecodeKeys(...)
			throw new UnsupportedOperationException("TODO implement JwtAuthority<C>.getActiveJwtDecodeKeys(...)");
		}
	}

	/**
	 * Time window to determine appropriate token.
	 */
	@JsonIgnoreProperties(ignoreUnknown = true)
	protected static class TimeWindow {

		/**
		 * Not before.
		 */
		private Long nbf;

		/**
		 * Expiry.
		 */
		private Long exp;

		/**
		 * Specifies the not before.
		 * 
		 * @param nbf Not before.
		 */
		public void setNbf(Long nbf) {
			this.nbf = nbf;
		}

		/**
		 * Specifies the expire.
		 * 
		 * @param exp Expires.
		 */
		public void setExp(Long exp) {
			this.exp = exp;
		}
	}

	/**
	 * {@link JwtRefreshKeyCollector} implementation.
	 */
	private class JwtRefreshCollectorImpl implements JwtRefreshKeyCollector {

		/**
		 * {@link StatePollContext}.
		 */
		private final StatePollContext<JwtRefreshKey[]> pollContext;

		/**
		 * Instantiate.
		 * 
		 * @param pollContext {@link StatePollContext}.
		 */
		public JwtRefreshCollectorImpl(StatePollContext<JwtRefreshKey[]> pollContext) {
			this.pollContext = pollContext;
		}

		/*
		 * ==================== JwtRefreshCollector ==================
		 */

		@Override
		public void setKeys(JwtRefreshKey... keys) {
			this.pollContext.setNextState(keys, -1, null);
		}
	}

	/**
	 * {@link JwtRefreshKey} implementation that ensure the data is available.
	 */
	private static class JwtRefreshKeyImpl implements JwtRefreshKey {

		/**
		 * Start time.
		 */
		private final long startTime;

		/**
		 * Expire time.
		 */
		private final long expireTime;

		/**
		 * Init vector.
		 */
		private final String initVector;

		/**
		 * Start salt.
		 */
		private final String startSalt;

		/**
		 * Lace.
		 */
		private final String lace;

		/**
		 * End salt.
		 */
		private final String endSalt;

		/**
		 * {@link Key}.
		 */
		private final Key key;

		/**
		 * Instantiate from {@link JwtRefreshKey}.
		 * 
		 * @param key {@link JwtRefreshKey}.
		 */
		private JwtRefreshKeyImpl(JwtRefreshKey key) {
			this.startTime = key.getStartTime();
			this.expireTime = key.getExpireTime();
			this.initVector = key.getInitVector();
			this.startSalt = key.getStartSalt();
			this.lace = key.getLace();
			this.endSalt = key.getEndSalt();
			this.key = key.getKey();
		}

		/**
		 * Indicates if the {@link JwtAccessKey} is valid to use.
		 * 
		 * @return <code>true</code> if valid to use.
		 */
		public boolean isValid() {
			boolean isValid = this.startTime > 0;
			isValid &= this.expireTime > this.startTime;
			isValid &= (this.initVector != null) & (this.initVector.length() > 0);
			isValid &= (this.startSalt != null) & (this.startSalt.length() > 0);
			isValid &= (this.lace != null) & (this.lace.length() > 0);
			isValid &= (this.endSalt != null) & (this.endSalt.length() > 0);
			isValid &= this.key != null;
			return isValid;
		}

		/*
		 * ==================== JwtEncodeKey =====================
		 */

		@Override
		public long getStartTime() {
			return this.startTime;
		}

		@Override
		public long getExpireTime() {
			return this.expireTime;
		}

		@Override
		public String getInitVector() {
			return this.initVector;
		}

		@Override
		public String getStartSalt() {
			return this.startSalt;
		}

		@Override
		public String getLace() {
			return this.lace;
		}

		@Override
		public String getEndSalt() {
			return this.endSalt;
		}

		@Override
		public Key getKey() {
			return this.key;
		}
	}

	/**
	 * {@link JwtAccessKeyCollector} implementation.
	 */
	private class JwtEncodeCollectorImpl implements JwtAccessKeyCollector {

		/**
		 * {@link StatePollContext}.
		 */
		private final StatePollContext<JwtAccessKey[]> pollContext;

		/**
		 * Instantiate.
		 * 
		 * @param pollContext {@link StatePollContext}.
		 */
		public JwtEncodeCollectorImpl(StatePollContext<JwtAccessKey[]> pollContext) {
			this.pollContext = pollContext;
		}

		/*
		 * ==================== JwtEncodeCollector ==================
		 */

		@Override
		public void setKeys(JwtAccessKey[] keys) {
			this.pollContext.setNextState(keys, -1, null);
		}
	}

	/**
	 * {@link JwtAccessKey} implementation that ensure the data is available.
	 */
	private static class JwtEncodeKeyImpl implements JwtAccessKey {

		/**
		 * Start time.
		 */
		private final long startTime;

		/**
		 * Expire time.
		 */
		private final long expireTime;

		/**
		 * Private {@link Key}.
		 */
		private final Key privateKey;

		/**
		 * Public {@link Key}.
		 */
		private final Key publicKey;

		/**
		 * Instantiate from {@link JwtAccessKey}.
		 * 
		 * @param key {@link JwtAccessKey}.
		 */
		private JwtEncodeKeyImpl(JwtAccessKey key) {
			this.startTime = key.getStartTime();
			this.expireTime = key.getExpireTime();
			this.privateKey = key.getPrivateKey();
			this.publicKey = key.getPublicKey();
		}

		/**
		 * Instantiate.
		 * 
		 * @param startTime  Start time.
		 * @param expireTime Expire time.
		 * @param privateKey Private {@link Key}.
		 * @param publicKey  Public {@link Key}.
		 */
		private JwtEncodeKeyImpl(long startTime, long expireTime, Key privateKey, Key publicKey) {
			this.startTime = startTime;
			this.expireTime = expireTime;
			this.privateKey = privateKey;
			this.publicKey = publicKey;
		}

		/**
		 * Indicates if the {@link JwtAccessKey} is valid to use.
		 * 
		 * @return <code>true</code> if valid to use.
		 */
		public boolean isValid() {
			boolean isValid = this.startTime > 0;
			isValid &= this.expireTime > this.startTime;
			isValid &= this.privateKey != null;
			isValid &= this.publicKey != null;
			return isValid;
		}

		/*
		 * ==================== JwtEncodeKey =====================
		 */

		@Override
		public long getStartTime() {
			return this.startTime;
		}

		@Override
		public long getExpireTime() {
			return this.expireTime;
		}

		@Override
		public Key getPrivateKey() {
			return this.privateKey;
		}

		@Override
		public Key getPublicKey() {
			return this.publicKey;
		}
	}

}