package com.example.dobby.service

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.example.dobby.AppProperties
import com.example.dobby.exception.DobbyException
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class JWTServiceTest {
    private val appProperties = mockk<AppProperties>()
    private lateinit var jwtService: JWTService
    private val testSecret = "super-secret-test-key-that-is-long-enough-for-hmac256"
    private val testExpiration = Duration.ofMinutes(15)

    @BeforeEach
    fun setUp() {
        clearAllMocks()

        val mockJwtProperties = mockk<AppProperties.Jwt>()
        every { appProperties.jwt } returns mockJwtProperties
        every { mockJwtProperties.secret } returns testSecret
        every { mockJwtProperties.expiration } returns testExpiration

        jwtService = JWTService(appProperties)
    }

    @Test
    fun `generateJWTToken should produce a valid, signed compact JWT string`() =
        runTest {
            val subject = "user-123"
            val claim = "SomeUser"

            val token = jwtService.generateJWTToken(subject, claim)

            assertNotNull(token)

            // Let's decode it manually to prove the service packed everything inside correctly
            val verifier =
                JWT
                    .require(Algorithm.HMAC256(testSecret))
                    .withIssuer("dobby-core")
                    .build()
            val decoded = verifier.verify(token)

            assertEquals("user-123", decoded.subject)
            assertEquals("SomeUser", decoded.getClaim("username").asString())
        }

    @Test
    fun `validateTokenAndGetSubject should return correct subject when token is valid`() =
        runTest {
            // Create a real signed token using our test parameters to feed into the validator
            val expectedSubject = "kernel-panic-master"
            val algorithm = Algorithm.HMAC256(testSecret)
            val validToken =
                JWT
                    .create()
                    .withIssuer("dobby-core")
                    .withSubject(expectedSubject)
                    .withClaim("username", "SomeUser")
                    .sign(algorithm)

            val extractedSubject = jwtService.validateTokenAndGetSubject(validToken)

            assertEquals(expectedSubject, extractedSubject)
        }

    @Test
    fun `validateTokenAndGetSubject should throw JWTException when signature is invalid`() =
        runTest {
            // Sign a token using a completely wrong rogue secret key
            val rogueAlgorithm = Algorithm.HMAC256("wrong-and-fraudulent-secret-key-123")
            val maliciousToken =
                JWT
                    .create()
                    .withIssuer("dobby-core")
                    .withSubject("hacker")
                    .sign(rogueAlgorithm)

            assertFailsWith<DobbyException.JWTException> {
                jwtService.validateTokenAndGetSubject(maliciousToken)
            }
        }

    @Test
    fun `validateTokenAndGetSubject should throw JWTException when issuer mismatches`() =
        runTest {
            val algorithm = Algorithm.HMAC256(testSecret)
            val badIssuerToken =
                JWT
                    .create()
                    .withIssuer("imposter-core")
                    .withSubject("user-123")
                    .sign(algorithm)

            assertFailsWith<DobbyException.JWTException> {
                jwtService.validateTokenAndGetSubject(badIssuerToken)
            }
        }
}
