package com.example.neurozen_front.presentation

import com.example.neurozen_front.R
import com.example.neurozen_front.domain.logic.BotSuggestion
import com.example.neurozen_front.domain.model.AppError
import com.example.neurozen_front.domain.model.LibraryResource
import com.example.neurozen_front.domain.model.Outcome
import com.example.neurozen_front.domain.repository.LibraryRepository
import com.example.neurozen_front.presentation.worker.ZenBotViewModel
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** Z1: welcome, quick options, real resources, typing indicator; crisis replies are immediate and alone. */
@OptIn(ExperimentalCoroutinesApi::class)
class ZenBotViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val library = mockk<LibraryRepository>()
    private val video = LibraryResource(7, "Respira 5 minutos", "", null, "Video", "https://www.youtube.com/watch?v=abcdefghijk", 5, null, listOf("sueño"), null)

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun startsWithWelcome_andTenQuickOptions() {
        val vm = ZenBotViewModel(library)
        assertEquals(1, vm.ui.value.messages.size)
        assertTrue(vm.ui.value.showQuickOptions)
        assertEquals(10, ZenBotViewModel.QUICK_OPTIONS.size)
        assertEquals(10, ZenBotViewModel.QUICK_OPTIONS.map { it.label }.toSet().size)
    }

    @Test
    fun crisis_isAnsweredSynchronously_withoutSearchingOrTyping() = runTest(dispatcher) {
        val vm = ZenBotViewModel(library)
        vm.sendQuick("no puedo dormir, quiero morir")
        val last = vm.ui.value.messages.last()
        assertTrue(last.isCrisis)
        assertFalse(vm.ui.value.typing)
        assertFalse(vm.ui.value.showQuickOptions)
        advanceUntilIdle()
        assertEquals(last, vm.ui.value.messages.last())
        coVerify(exactly = 0) { library.search(any(), any()) }
    }

    @Test
    fun sleep_recommendsRealResources_withTypingIndicator() = runTest(dispatcher) {
        coEvery { library.search("sueño", 3) } returns Outcome.Success(listOf(video))
        val vm = ZenBotViewModel(library)
        vm.onInput("No puedo dormir bien")
        vm.send()
        assertTrue(vm.ui.value.typing)
        assertEquals(BotSuggestion.LIBRARY, vm.ui.value.messages.last().suggestion)
        advanceUntilIdle()
        assertFalse(vm.ui.value.typing)
        val last = vm.ui.value.messages.last()
        assertEquals(listOf(video), last.resources)
        assertEquals(R.string.zenbot_resources_found, last.textRes)
    }

    @Test
    fun noMatches_saysSoNaturally() = runTest(dispatcher) {
        coEvery { library.search(any(), any()) } returns Outcome.Success(emptyList())
        val vm = ZenBotViewModel(library)
        vm.sendQuick("Quiero respirar con calma")
        advanceUntilIdle()
        assertEquals(R.string.zenbot_resources_none, vm.ui.value.messages.last().textRes)
    }

    @Test
    fun searchFails_usesPlanFilteredListLocally_orReportsError() = runTest(dispatcher) {
        coEvery { library.search(any(), any()) } returns Outcome.Failure(AppError.NotFound("No encontrado"))
        coEvery { library.getAll() } returns Outcome.Success(listOf(video))
        val vm = ZenBotViewModel(library)
        vm.sendQuick("No puedo dormir bien")
        advanceUntilIdle()
        assertEquals(listOf(video), vm.ui.value.messages.last().resources)

        coEvery { library.getAll() } returns Outcome.Failure(AppError.Network())
        vm.sendQuick("No puedo dormir bien")
        advanceUntilIdle()
        assertEquals(R.string.zenbot_resources_error, vm.ui.value.messages.last().textRes)
    }

    @Test
    fun topicsWithoutResources_doNotSearch_andBlankInputIsIgnored() = runTest(dispatcher) {
        val vm = ZenBotViewModel(library)
        vm.onInput("   ")
        vm.send()
        vm.sendQuick(" ")
        assertEquals(1, vm.ui.value.messages.size)
        vm.sendQuick("Ver mis citas")
        advanceUntilIdle()
        assertEquals(BotSuggestion.MY_APPOINTMENTS, vm.ui.value.messages.last().suggestion)
        coVerify(exactly = 0) { library.search(any(), any()) }
        vm.onInput("x".repeat(600))
        assertEquals(500, vm.ui.value.input.length)
    }
}
