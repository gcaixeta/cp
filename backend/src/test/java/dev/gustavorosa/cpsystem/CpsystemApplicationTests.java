package dev.gustavorosa.cpsystem;

import org.junit.jupiter.api.Test;

/**
 * Basic smoke test to ensure that the test setup is working.
 * Context loading test is disabled as it requires a database connection.
 * The application is tested manually and through integration tests.
 */
class CpsystemApplicationTests {

	@Test
	void smokeTest() {
		// Basic smoke test - ensures test infrastructure is working
		org.junit.jupiter.api.Assertions.assertTrue(true);
	}

}
