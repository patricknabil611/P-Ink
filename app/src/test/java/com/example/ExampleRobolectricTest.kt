package com.example

import android.app.Application
import android.content.Context
import android.graphics.Color
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.AppDatabase
import com.example.data.AppSettingsRepository
import com.example.data.NoteRepository
import com.example.data.TodoRepository
import com.example.data.WritingPadSize
import com.example.ink.InkBitmapRenderer
import com.example.ink.InkPoint
import com.example.ink.InkStroke
import com.example.ink.findIntersectedStrokeIndices
import com.example.ink.pressureToStrokeWidth
import com.example.ui.ConversionState
import com.example.ui.TodoViewModel
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
    private lateinit var noteRepository: NoteRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = TodoRepository(database.todoDao())
        noteRepository = NoteRepository(database.noteDao())
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `app name matches P Ink and version matches 26_06_26_4`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("P Ink", appName)
        assertEquals("26.06.26.4", BuildConfig.VERSION_NAME)
        assertEquals(2606264, BuildConfig.VERSION_CODE)
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
    fun `pressure maps smoothly from thin to thick stroke widths`() {
        val light = pressureToStrokeWidth(0.1f, minWidth = 2f, maxWidth = 10f)
        val heavy = pressureToStrokeWidth(0.95f, minWidth = 2f, maxWidth = 10f)
        assertTrue("Expected heavy pressure width ($heavy) > light ($light)", heavy > light)
        assertTrue(light >= 2f)
        assertTrue(heavy <= 10f)
    }

    @Test
    fun `eraser hit testing finds intersected strokes across drag segments`() {
        val strokes = listOf(
            InkStroke(listOf(InkPoint(50f, 50f), InkPoint(150f, 50f))),
            InkStroke(listOf(InkPoint(50f, 220f), InkPoint(150f, 220f))),
            InkStroke(listOf(InkPoint(300f, 300f))),
        )

        val firstHits = findIntersectedStrokeIndices(strokes, 100f, 20f, 100f, 80f, radius = 18f)
        assertEquals(setOf(0), firstHits)

        val dotHits = findIntersectedStrokeIndices(strokes, 308f, 308f, 308f, 308f, radius = 18f)
        assertEquals(setOf(2), dotHits)

        val noHits = findIntersectedStrokeIndices(strokes, 250f, 120f, 260f, 130f, radius = 15f)
        assertTrue(noHits.isEmpty())
    }

    @Test
    fun `view model onEraseStrokes removes targeted strokes and updates conversion state`() {
        val app = ApplicationProvider.getApplicationContext<Application>()
        val vm = TodoViewModel(app)

        val s0 = InkStroke(listOf(InkPoint(10f, 10f, 0.3f), InkPoint(60f, 10f, 0.6f)))
        val s1 = InkStroke(listOf(InkPoint(10f, 90f, 0.8f), InkPoint(60f, 90f, 0.9f)))

        vm.onStrokeCompleted(s0)
        vm.onStrokeCompleted(s1)
        assertEquals(2, vm.strokes.value.size)
        assertEquals(ConversionState.AutoConvertPending, vm.conversionState.value)

        vm.onEraseStrokes(setOf(0))
        assertEquals(listOf(s1), vm.strokes.value)
        assertEquals(ConversionState.AutoConvertPending, vm.conversionState.value)

        vm.onEraseStrokes(setOf(0))
        assertTrue(vm.strokes.value.isEmpty())
        assertEquals(ConversionState.Idle, vm.conversionState.value)
    }

    @Test
    fun `ink bitmap renderer produces higher res white background with pressure ink`() {
        assertNull(InkBitmapRenderer.renderToBitmap(emptyList(), 800, 400))

        val strokes = listOf(
            InkStroke(
                points = listOf(
                    InkPoint(100f, 100f, pressure = 0.15f),
                    InkPoint(220f, 150f, pressure = 0.55f),
                    InkPoint(340f, 120f, pressure = 0.95f),
                )
            )
        )
        val bitmap = InkBitmapRenderer.renderToBitmap(strokes, 800, 400)
        assertNotNull(bitmap)
        bitmap!!.let {
            assertTrue("Expected high-res width >= 600px, got ${it.width}", it.width >= 600)
            assertEquals(Color.WHITE, it.getPixel(0, 0))
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

    @Test
    fun `note repository saves auto-titled notes updates and deletes cleanly`() = runTest {
        val saved = noteRepository.saveNote(content = "First line heading\nSecond line details")
        assertNotNull(saved)
        assertEquals("First line heading", saved!!.title)

        var list = noteRepository.notes.first()
        assertEquals(1, list.size)

        noteRepository.updateNote(list.first(), newTitle = "Custom Title", newContent = "Updated body")
        list = noteRepository.notes.first()
        assertEquals("Custom Title", list.first().title)
        assertEquals("Updated body", list.first().content)

        val target = list.first()
        noteRepository.deleteNote(target)
        assertTrue(noteRepository.notes.first().isEmpty())

        noteRepository.restoreNote(target)
        assertEquals(1, noteRepository.notes.first().size)
    }

    @Test
    fun `settings repository persists color palette and view preferences`() = runTest {
        val repo = AppSettingsRepository(context)
        repo.setPenColor("ice_white")
        repo.setPadBackground("cream")
        repo.setShowGuideLines(false)
        repo.setAutoConvertEnabled(false)
        repo.setWritingPadSize(WritingPadSize.TALL)
        repo.setCompactLists(true)

        val updated = repo.settings.first()
        assertEquals("cream", updated.padBackgroundId)
        // Switching to light cream background auto-adjusts default ice_white pen to charcoal
        assertEquals("charcoal", updated.penColorId)
        assertFalse(updated.showGuideLines)
        assertFalse(updated.autoConvertEnabled)
        assertEquals(WritingPadSize.TALL, updated.writingPadSize)
        assertTrue(updated.compactLists)
    }

    @Test
    fun `room migration 1 to 2 preserves existing tasks and adds notes table`() {
        val dbName = "migration_test.db"
        context.deleteDatabase(dbName)

        // Create v1 schema manually and insert a v1 row
        val openHelper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context)
                .name(dbName)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL(
                            "CREATE TABLE IF NOT EXISTS todo_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, text TEXT NOT NULL, isCompleted INTEGER NOT NULL, createdAt INTEGER NOT NULL)"
                        )
                        db.execSQL(
                            "INSERT INTO todo_tasks (id, text, isCompleted, createdAt) VALUES (7, 'Legacy task', 1, 1700000000000)"
                        )
                    }

                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                })
                .build()
        )
        openHelper.writableDatabase.close()
        openHelper.close()

        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .addMigrations(AppDatabase.MIGRATION_1_2)
            .build()

        runTest {
            val tasks = migratedDb.todoDao().getAllTasksOnce()
            assertEquals(1, tasks.size)
            assertEquals("Legacy task", tasks[0].text)
            assertEquals("task_7", tasks[0].remoteId)
            assertEquals(1700000000000L, tasks[0].updatedAt)

            migratedDb.noteDao().insertNote(
                com.example.data.NoteEntity(title = "Post Migration", content = "Works")
            )
            assertEquals(1, migratedDb.noteDao().getAllNotesOnce().size)
        }
        migratedDb.close()
        context.deleteDatabase(dbName)
    }

    @Test
    fun `app check helper creates stable debug token and supports custom token override`() {
        com.example.ai.configureAppCheck(context, null)
        val initial = com.example.ai.getAppCheckDebugToken(context)
        assertTrue(initial.isNotBlank())
        assertEquals(initial, com.example.ai.getAppCheckDebugToken(context))

        val customUuid = "11111111-2222-3333-4444-555555555555"
        assertTrue(com.example.ai.setCustomAppCheckDebugToken(context, customUuid))
        assertEquals(customUuid, com.example.ai.getAppCheckDebugToken(context))
    }
}
