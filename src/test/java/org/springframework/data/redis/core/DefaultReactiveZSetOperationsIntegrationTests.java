/*
 * Copyright 2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.redis.core;

import static org.assertj.core.api.Assertions.*;
import static org.junit.Assume.*;

import reactor.test.StepVerifier;

import java.util.Collection;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.ByteBufferObjectFactory;
import org.springframework.data.redis.ConnectionFactoryTracker;
import org.springframework.data.redis.ObjectFactory;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisZSetCommands.Limit;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Integration tests for {@link DefaultReactiveZSetOperations}.
 *
 * @author Mark Paluch
 */
@RunWith(Parameterized.class)
@SuppressWarnings("unchecked")
public class DefaultReactiveZSetOperationsIntegrationTests<K, V> {

	private final ReactiveRedisTemplate<K, V> redisTemplate;
	private final ReactiveZSetOperations<K, V> zSetOperations;

	private final ObjectFactory<K> keyFactory;
	private final ObjectFactory<V> valueFactory;

	@Parameters(name = "{3}")
	public static Collection<Object[]> testParams() {
		return ReactiveOperationsTestParams.testParams();
	}

	@AfterClass
	public static void cleanUp() {
		ConnectionFactoryTracker.cleanUp();
	}

	/**
	 * @param redisTemplate
	 * @param keyFactory
	 * @param valueFactory
	 * @param label parameterized test label, no further use besides that.
	 */
	public DefaultReactiveZSetOperationsIntegrationTests(ReactiveRedisTemplate<K, V> redisTemplate,
			ObjectFactory<K> keyFactory, ObjectFactory<V> valueFactory, String label) {

		this.redisTemplate = redisTemplate;
		this.zSetOperations = redisTemplate.opsForZSet();
		this.keyFactory = keyFactory;
		this.valueFactory = valueFactory;

		ConnectionFactoryTracker.add(redisTemplate.getConnectionFactory());
	}

	@Before
	public void before() {

		RedisConnectionFactory connectionFactory = (RedisConnectionFactory) redisTemplate.getConnectionFactory();
		RedisConnection connection = connectionFactory.getConnection();
		connection.flushAll();
		connection.close();
	}

	@Test // DATAREDIS-602
	public void add() {

		K key = keyFactory.instance();
		V value = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value, 42.1)).expectNext(true).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void remove() {

		K key = keyFactory.instance();
		V value = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value, 42.1)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.remove(key, value)).expectNext(1L).expectComplete().verify();

		StepVerifier.create(zSetOperations.remove(key, value)).expectNext(0L).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void incrementScore() {

		K key = keyFactory.instance();
		V value = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value, 42.1)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.incrementScore(key, value, 1.1)).expectNext(43.2).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void rank() {

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.rank(key, value1)).expectNext(1L).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void reverseRank() {

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.reverseRank(key, value1)).expectNext(0L).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void range() {

		assumeFalse(valueFactory instanceof ByteBufferObjectFactory);

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.range(key, new Range<>(0L, 0L))) //
				.consumeNextWith(actual -> {
					assertThat(actual).hasSize(1).contains(value2);
				}).expectComplete().verify();

	}

	@Test // DATAREDIS-602
	public void rangeWithScores() {

		assumeFalse(valueFactory instanceof ByteBufferObjectFactory);

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.rangeWithScores(key, new Range<>(0L, 0L))) //
				.consumeNextWith(actual -> {
					assertThat(actual).hasSize(1).contains(new DefaultTypedTuple<>(value2, 10d));
				}).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void rangeByScore() {

		assumeFalse(valueFactory instanceof ByteBufferObjectFactory);

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.rangeByScore(key, new Range<>(9d, 11d))) //
				.consumeNextWith(actual -> {
					assertThat(actual).hasSize(1).contains(value2);
				}).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void rangeByScoreWithScores() {

		assumeFalse(valueFactory instanceof ByteBufferObjectFactory);

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.rangeByScoreWithScores(key, new Range<>(9d, 11d))) //
				.consumeNextWith(actual -> {
					assertThat(actual).hasSize(1).contains(new DefaultTypedTuple<>(value2, 10d));
				}).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void rangeByScoreWithLimit() {

		assumeFalse(valueFactory instanceof ByteBufferObjectFactory);

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier
				.create(zSetOperations.rangeByScore(key, new Range<>(0d, 100d), //
						Limit.limit().offset(1).count(10))) //
				.consumeNextWith(actual -> {
					assertThat(actual).hasSize(1).contains(value1);
				}).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void rangeByScoreWithScoresWithLimit() {

		assumeFalse(valueFactory instanceof ByteBufferObjectFactory);

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier
				.create(zSetOperations.rangeByScoreWithScores(key, new Range<>(0d, 100d), //
						Limit.limit().offset(1).count(10))) //
				.consumeNextWith(actual -> {
					assertThat(actual).hasSize(1).contains(new DefaultTypedTuple<>(value1, 42.1));
				}).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void reverseRange() {

		assumeFalse(valueFactory instanceof ByteBufferObjectFactory);

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.reverseRange(key, new Range<>(0L, 0L))) //
				.consumeNextWith(actual -> {
					assertThat(actual).hasSize(1).contains(value1);
				}).expectComplete().verify();

	}

	@Test // DATAREDIS-602
	public void reverseRangeWithScores() {

		assumeFalse(valueFactory instanceof ByteBufferObjectFactory);

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.reverseRangeWithScores(key, new Range<>(0L, 0L))) //
				.consumeNextWith(actual -> {
					assertThat(actual).hasSize(1).contains(new DefaultTypedTuple<>(value1, 42.1));
				}).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void reverseRangeByScore() {

		assumeFalse(valueFactory instanceof ByteBufferObjectFactory);

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.reverseRangeByScore(key, new Range<>(11d, 9d))) //
				.consumeNextWith(actual -> {
					assertThat(actual).hasSize(1).contains(value2);
				}).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void reverseRangeByScoreWithScores() {

		assumeFalse(valueFactory instanceof ByteBufferObjectFactory);

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.reverseRangeByScoreWithScores(key, new Range<>(11d, 9d))) //
				.consumeNextWith(actual -> {
					assertThat(actual).hasSize(1).contains(new DefaultTypedTuple<V>(value2, 10d));
				}).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void reverseRangeByScoreWithLimit() {

		assumeFalse(valueFactory instanceof ByteBufferObjectFactory);

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier
				.create(zSetOperations.reverseRangeByScore(key, new Range<>(100d, 0d), //
						Limit.limit().offset(1).count(10))) //
				.consumeNextWith(actual -> {
					assertThat(actual).hasSize(1).contains(value2);
				}).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void reverseRangeByScoreWithScoresWithLimit() {

		assumeFalse(valueFactory instanceof ByteBufferObjectFactory);

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier
				.create(zSetOperations.reverseRangeByScoreWithScores(key, new Range<>(100d, 0d), //
						Limit.limit().offset(1).count(10))) //
				.consumeNextWith(actual -> {
					assertThat(actual).hasSize(1).contains(new DefaultTypedTuple<>(value2, 10d));
				}).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void count() {

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.count(key, new Range<Double>(0d, 100d))).expectNext(2L).expectComplete()
				.verify();
		StepVerifier.create(zSetOperations.count(key, new Range<Double>(0d, 10d))).expectNext(1L).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void size() {

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.size(key)).expectNext(2L).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void score() {

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.score(key, value1)).expectNext(42.1d).expectComplete().verify();
		StepVerifier.create(zSetOperations.score(key, value2)).expectNext(10d).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void removeRange() {

		assumeFalse(valueFactory instanceof ByteBufferObjectFactory);

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.removeRange(key, new Range<>(0L, 0L))).expectNext(1L).expectComplete().verify();
		StepVerifier.create(zSetOperations.range(key, new Range<>(0L, 5L))).consumeNextWith(actual -> {
			assertThat(actual).hasSize(1).contains(value1);
		}).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void removeRangeByScore() {

		assumeFalse(valueFactory instanceof ByteBufferObjectFactory);

		K key = keyFactory.instance();
		V value1 = valueFactory.instance();
		V value2 = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, value1, 42.1)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, value2, 10)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.removeRangeByScore(key, new Range<>(9d, 11d))).expectNext(1L).expectComplete()
				.verify();
		StepVerifier.create(zSetOperations.range(key, new Range<>(0L, 5L))).consumeNextWith(actual -> {
			assertThat(actual).hasSize(1).contains(value1);
		}).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void unionAndStore() {

		K key = keyFactory.instance();
		K otherKey = keyFactory.instance();
		K destKey = keyFactory.instance();

		V onlyInKey = valueFactory.instance();
		V shared = valueFactory.instance();
		V onlyInOtherKey = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, onlyInKey, 10)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, shared, 11)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.add(otherKey, onlyInOtherKey, 10)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(otherKey, shared, 11)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.unionAndStore(key, otherKey, destKey)).expectNext(3L).expectComplete().verify();
		StepVerifier.create(zSetOperations.range(destKey, new Range<>(0L, 100L))).consumeNextWith(actual -> {
			assertThat(actual).hasSize(3);
		}).expectComplete().verify();
	}

	@Test // DATAREDIS-602
	public void intersectAndStore() {

		K key = keyFactory.instance();
		K otherKey = keyFactory.instance();
		K destKey = keyFactory.instance();

		V onlyInKey = valueFactory.instance();
		V shared = valueFactory.instance();
		V onlyInOtherKey = valueFactory.instance();

		StepVerifier.create(zSetOperations.add(key, onlyInKey, 10)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, shared, 11)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.add(otherKey, onlyInOtherKey, 10)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(otherKey, shared, 11)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.intersectAndStore(key, otherKey, destKey)).expectNext(1L).expectComplete()
				.verify();
		StepVerifier.create(zSetOperations.range(destKey, new Range<>(0L, 5L))).consumeNextWith(actual -> {
			assertThat(actual).hasSize(1);
		}).expectComplete().verify();

	}

	@Test // DATAREDIS-602
	public void rangeByLex() {

		assumeTrue(redisTemplate.getValueSerializer() instanceof StringRedisSerializer);

		K key = keyFactory.instance();
		V a = (V) "a";
		V b = (V) "b";

		StepVerifier.create(zSetOperations.add(key, a, 10)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, b, 11)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.rangeByLex(key, new Range<>("a", "a"))).consumeNextWith(actual -> {
			assertThat(actual).hasSize(1).contains(a);
		}).expectComplete().verify();

	}

	@Test // DATAREDIS-602
	public void rangeByLex1() {

		assumeTrue(redisTemplate.getValueSerializer() instanceof StringRedisSerializer);

		K key = keyFactory.instance();
		V a = (V) "a";
		V b = (V) "b";

		StepVerifier.create(zSetOperations.add(key, a, 10)).expectNext(true).expectComplete().verify();
		StepVerifier.create(zSetOperations.add(key, b, 11)).expectNext(true).expectComplete().verify();

		StepVerifier.create(zSetOperations.rangeByLex(key, new Range<>("a", "z"), Limit.limit().offset(0).count(10)))
				.consumeNextWith(actual -> {
					assertThat(actual).hasSize(2).contains(a, b);
				}).expectComplete().verify();

		StepVerifier.create(zSetOperations.rangeByLex(key, new Range<>("a", "z"), Limit.limit().offset(1).count(10)))
				.consumeNextWith(actual -> {
					assertThat(actual).hasSize(1).contains(b);
				}).expectComplete().verify();
	}
}
