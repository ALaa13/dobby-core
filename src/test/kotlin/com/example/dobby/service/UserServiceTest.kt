package com.example.dobby.service

import com.example.dobby.dto.UserProfileResponse
import com.example.dobby.exception.DobbyException
import com.example.dobby.repository.UserProfileRepository
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserServiceTest {

    private val userProfileRepository = mockk<UserProfileRepository>()
    private lateinit var userService: UserService
    private val mockSecurityContext = mockk<SecurityContext>()
    private val mockAuthentication = mockk<Authentication>()

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        userService = UserService(userProfileRepository)

        mockkStatic(SecurityContextHolder::class)
        every { SecurityContextHolder.getContext() } returns mockSecurityContext
    }

    @AfterEach
    fun tearDown() {
        // Always unmock static classes to prevent side effects in other tests!
        unmockkStatic(SecurityContextHolder::class)
    }

    @Test
    fun `getCurrentUser should return profile when authenticated user exists in database`() = runTest {
        val expectedDiscordId = "123456789"
        val mockProfileEntity = mockk<UserProfileResponse>()

        every { mockSecurityContext.authentication } returns mockAuthentication
        every { mockAuthentication.name } returns expectedDiscordId

        // Stub the repository lookup
        coEvery { userProfileRepository.findProfile(expectedDiscordId) } returns mockProfileEntity


        val result = userService.getCurrentUser()


        assertEquals(mockProfileEntity, result)
        coVerify(exactly = 1) { userProfileRepository.findProfile(expectedDiscordId) }
    }

    @Test
    fun `getCurrentUser should throw ProfileNotFoundException when authentication context is missing`() = runTest {
        // Simulate no logged-in context user (returns null)
        every { mockSecurityContext.authentication } returns null


        assertFailsWith<DobbyException.ProfileNotFoundException> {
            userService.getCurrentUser()
        }

        coVerify(exactly = 0) { userProfileRepository.findProfile(any()) }
    }

    @Test
    fun `getCurrentUser should throw ProfileNotFoundException when user details missing in database`() = runTest {
        val validDiscordId = "987654321"
        every { mockSecurityContext.authentication } returns mockAuthentication
        every { mockAuthentication.name } returns validDiscordId

        // Database returns null profile match
        coEvery { userProfileRepository.findProfile(validDiscordId) } returns null


        assertFailsWith<DobbyException.ProfileNotFoundException> {
            userService.getCurrentUser()
        }
    }
}