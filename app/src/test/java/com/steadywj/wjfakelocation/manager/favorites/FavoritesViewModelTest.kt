// FavoritesViewModelTest.kt
package com.steadywj.wjfakelocation.manager.favorites

import app.cash.turbine.test
import com.steadywj.wjfakelocation.data.model.FavoriteLocation
import com.steadywj.wjfakelocation.data.repository.FavoritesRepository
import com.steadywj.wjfakelocation.manager.favorites.viewmodel.FavoritesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * FavoritesViewModel 单元测试
 * 测试收藏管理功能的业务逻辑
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FavoritesViewModelTest {
    private lateinit var viewModel: FavoritesViewModel
    private lateinit var mockRepository: FavoritesRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    // 模拟数据
    private val testFavorites =
        listOf(
            FavoriteLocation(id = 1, name = "家", latitude = 39.9042, longitude = 116.4074, category = "home"),
            FavoriteLocation(id = 2, name = "公司", latitude = 39.9087, longitude = 116.3975, category = "work"),
        )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // 创建 Mock Repository
        mockRepository = mock()
        val favoritesFlow = MutableStateFlow(testFavorites)
        whenever(mockRepository.allFavorites).thenReturn(favoritesFlow)

        // 创建 ViewModel
        viewModel = FavoritesViewModel(mockRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `test initial state`() {
        // 验证初始状态
        val initialState = viewModel.uiState.value
        assertFalse(initialState.isLoading)
        assertEquals("", initialState.searchQuery)
        assertNull(initialState.showSuccessMessage)
        assertFalse(initialState.showEditDialog)
    }

    @Test
    fun `test addFavorite adds location successfully`() =
        runTest {
            // Given
            val name = "测试地点"
            val latitude = 39.9042
            val longitude = 116.4074
            val address = "北京市朝阳区"
            val category = "custom"

            // When
            viewModel.addFavorite(name, latitude, longitude, address, category)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            verify(mockRepository).insertFavorite(any<FavoriteLocation>())
            assertEquals("已添加到收藏", viewModel.uiState.value.showSuccessMessage)
        }

    @Test
    fun `test updateFavorite updates location successfully`() =
        runTest {
            // Given
            val favorite = testFavorites[0].copy(name = "更新后的名字")

            // When
            viewModel.updateFavorite(favorite)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            verify(mockRepository).updateFavorite(any())
            assertEquals("已更新收藏", viewModel.uiState.value.showSuccessMessage)
        }

    @Test
    fun `test deleteFavorite removes location successfully`() =
        runTest {
            // Given
            val favorite = testFavorites[0]

            // When
            viewModel.deleteFavorite(favorite)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            verify(mockRepository).deleteFavorite(favorite)
            assertEquals("已删除收藏", viewModel.uiState.value.showSuccessMessage)
        }

    @Test
    fun `test clearMessage clears success message`() =
        runTest {
            // Given - 先触发一个成功消息
            viewModel.addFavorite("测试", 39.9, 116.4, "地址", "类别")
            testDispatcher.scheduler.advanceUntilIdle()

            assertNotNull(viewModel.uiState.value.showSuccessMessage)

            // When
            viewModel.clearMessage()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then
            assertNull(viewModel.uiState.value.showSuccessMessage)
        }

    @Test
    fun `test allFavorites emits data from repository`() =
        runTest {
            // When & Then
            viewModel.allFavorites.test {
                val emittedList = awaitItem()
                assertEquals(2, emittedList.size)
                assertEquals("家", emittedList[0].name)
                assertEquals("公司", emittedList[1].name)
            }
        }
}
