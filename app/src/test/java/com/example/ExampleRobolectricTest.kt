package com.example

import android.content.Context
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.TodoRepository
import com.example.ink.InkBitmapRenderer
import com.example.ink.InkPoint
import com.example.ink.InkStroke
import com.example.util.TextDirectionUtil
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExampleRobolectricTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: TodoRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TodoRepository(database.todoDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `app name matches P Ink`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("P Ink", appName)
    }

    @Test
    fun `text direction auto detects Arabic English and mixed tasks`() {
        assertTrue(TextDirectionUtil.isRtl("شراء قهوة وخبز"))
        assertTrue(TextDirectionUtil.isRtl("2 كيلو تفاح"))
        assertTrue(TextDirectionUtil.isRtl("مراجعة ملف PDF"))
        assertFalse(TextDirectionUtil.isRtl("Buy milk and eggs"))
        assertFalse(TextDirectionUtil.isRtl("Call Ahmed اتصل بأحمد"))
        assertFalse(TextDirectionUtil.isRtl(""))
    }

    @Test
    fun `ink bitmap renderer produces white background with black ink`() {
        assertNull(InkBitmapRenderer.renderToBitmap(emptyList(), 800, 400))

        val strokes = listOf(
            InkStroke(
                points = listOf(
                    InkPoint(100f, 100f),
                    InkPoint(220f, 150f),
                    InkPoint(340f, 120f),
                )
            )
        )
        val bitmap = InkBitmapRenderer.renderToBitmap(strokes, 800, 400)
        assertNotNull(bitmap)
        bitmap!!.let {
            // Corner pixel should be solid white for optimal OCR contrast
            assertEquals(Color.WHITE, it.getPixel(0, 0))
            // Ensure at least some dark ink pixels were drawn
            var hasInkPixel = false
            for (y in 0 until it.height step 2) {
                for (x in 0 until it.width step 2) {
                    if (Color.red(it.getPixel(x, y)) < 80) {
                        hasInkPixel = true
                        break
                    }
                }
                if (hasInkPixel) break
            }
            assertTrue("Expected dark ink pixels on rendered bitmap", hasInkPixel)
            it.recycle()
        }
    }

    @Test
    fun `todo repository persists toggles edits and deletes tasks locally`() = runTest {
        repository.addTask("شراء خبز")
        repository.addTask("Review PR #42")

        var tasks = repository.tasks.first()
        assertEquals(2, tasks.size)

        val arabicTask = tasks.first { it.text == "شراء خبز" }
        repository.toggleCompleted(arabicTask)

        tasks = repository.tasks.first()
        assertTrue(tasks.first { it.id == arabicTask.id }.isCompleted)

        val englishTask = tasks.first { it.text == "Review PR #42" }
        repository.updateText(englishTask, "Review PR #43")

        tasks = repository.tasks.first()
        assertTrue(tasks.any { it.text == "Review PR #43" })

        val updatedEnglish = tasks.first { it.id == englishTask.id }
        repository.deleteTask(updatedEnglish)
        tasks = repository.tasks.first()
        assertEquals(1, tasks.size)

        repository.restoreTask(updatedEnglish)
        tasks = repository.tasks.first()
        assertEquals(2, tasks.size)
    }
}
