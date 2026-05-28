package org.futo.inputmethod.engine.general

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import org.futo.inputmethod.engine.IMEHelper
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

class ActionInputTransactionIMETest {

    private lateinit var mockHelper: IMEHelper
    private lateinit var mockIc: InputConnection
    private lateinit var mockContext: Context

    @Before
    fun setUp() {
        mockHelper = mock(IMEHelper::class.java)
        mockIc = mock(InputConnection::class.java)
        mockContext = mock(Context::class.java)

        `when`(mockHelper.getCurrentInputConnection()).thenReturn(mockIc)
        `when`(mockHelper.context).thenReturn(mockContext)
    }

    @Test
    fun testUseComposingModeText() {
        val editorInfo = EditorInfo()
        editorInfo.inputType = EditorInfo.TYPE_CLASS_TEXT
        `when`(mockHelper.getCurrentEditorInfo()).thenReturn(editorInfo)

        val transaction = ActionInputTransactionIME(mockHelper)

        assertTrue("Should use composing mode for text fields", transaction.useComposingMode)

        transaction.updatePartial("hello")
        verify(mockHelper).requestCursorUpdate()
        verify(mockIc).setComposingText("hello", 1)

        transaction.commit("hello world")
        verify(mockIc).commitText("hello world", 1)
        verify(mockHelper).endInputTransaction(transaction)

        // Further updates should be ignored
        transaction.updatePartial("ignored")
        verify(mockIc, times(1)).setComposingText(anyString(), anyInt())
    }

    @Test
    fun testUseComposingModeNotText() {
        val editorInfo = EditorInfo()
        editorInfo.inputType = EditorInfo.TYPE_CLASS_NUMBER
        `when`(mockHelper.getCurrentEditorInfo()).thenReturn(editorInfo)

        val transaction = ActionInputTransactionIME(mockHelper)

        assertFalse("Should not use composing mode for non-text fields", transaction.useComposingMode)

        transaction.updatePartial("hello")
        // partial update should be ignored
        verify(mockIc, never()).setComposingText(anyString(), anyInt())

        transaction.commit("hello world")
        verify(mockIc).commitText("hello world", 1)
    }

    @Test
    fun testCancel() {
        val editorInfo = EditorInfo()
        editorInfo.inputType = EditorInfo.TYPE_CLASS_TEXT
        `when`(mockHelper.getCurrentEditorInfo()).thenReturn(editorInfo)

        val transaction = ActionInputTransactionIME(mockHelper)
        transaction.updatePartial("hello")
        verify(mockIc).setComposingText("hello", 1)

        transaction.cancel()
        verify(mockIc).commitText("", 1)
        verify(mockHelper).endInputTransaction(transaction)

        transaction.commit("another")
        // Shouldn't commit again
        verify(mockIc, times(1)).commitText(anyString(), anyInt())
    }

    @Test
    fun testStreamingMultiplePartialUpdates() {
        val editorInfo = EditorInfo()
        editorInfo.inputType = EditorInfo.TYPE_CLASS_TEXT
        `when`(mockHelper.getCurrentEditorInfo()).thenReturn(editorInfo)

        val transaction = ActionInputTransactionIME(mockHelper)

        transaction.updatePartial("h")
        verify(mockIc).setComposingText("h", 1)

        transaction.updatePartial("hello")
        verify(mockIc).setComposingText("hello", 1)

        transaction.updatePartial("hello ")
        verify(mockIc).setComposingText("hello ", 1)

        transaction.commit("hello world")
        verify(mockIc).commitText("hello world", 1)

        verify(mockHelper, times(4)).requestCursorUpdate()
        verify(mockHelper).endInputTransaction(transaction)
    }

    @Test
    fun testCancelClearsComposingSpan() {
        val editorInfo = EditorInfo()
        editorInfo.inputType = EditorInfo.TYPE_CLASS_TEXT
        `when`(mockHelper.getCurrentEditorInfo()).thenReturn(editorInfo)

        val transaction = ActionInputTransactionIME(mockHelper)

        transaction.updatePartial("hello")
        verify(mockIc).setComposingText("hello", 1)

        transaction.cancel()
        verify(mockIc).commitText("", 1)

        transaction.updatePartial("world")
        // Should ignore updatePartial after cancel
        verify(mockIc, never()).setComposingText("world", 1)
    }
}
