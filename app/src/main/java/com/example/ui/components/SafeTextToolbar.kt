package com.example.ui.components

import android.graphics.Rect as AndroidRect
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.TextToolbar
import androidx.compose.ui.platform.TextToolbarStatus
import kotlin.math.roundToInt

/**
 * Defensive Android [TextToolbar] implementation that prevents `DecorView` errors:
 * `Destroying unexpected ActionMode instance of TYPE_FLOATING; ... Expected null`.
 *
 * Unlike the default Compose 1.7 `AndroidTextToolbar`, this:
 * 1. Clears [actionMode] *before* calling `finish()` to prevent re-entrant double-`finish()`.
 * 2. Ignores `finish()` when the host [View] is no longer attached to a window (e.g., during
 *    `ModalBottomSheet` dialog teardown or rapid navigation transitions).
 * 3. Detaches cleanly via [View.OnAttachStateChangeListener].
 */
class SafeTextToolbar(private val view: View) : TextToolbar, View.OnAttachStateChangeListener {
  private var actionMode: ActionMode? = null
  private var currentRect: Rect = Rect.Zero
  private var isFinishingOrDestroying = false

  private var onCopyRequested: (() -> Unit)? = null
  private var onPasteRequested: (() -> Unit)? = null
  private var onCutRequested: (() -> Unit)? = null
  private var onSelectAllRequested: (() -> Unit)? = null

  override var status: TextToolbarStatus = TextToolbarStatus.Hidden
    private set

  private val callback =
    object : ActionMode.Callback2() {
      override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        if (menu == null || mode == null) return false
        populateMenu(menu)
        return true
      }

      override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        if (menu == null || mode == null) return false
        menu.clear()
        populateMenu(menu)
        return true
      }

      override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        val id = item?.itemId ?: return false
        val action =
          when (id) {
            MENU_ID_COPY -> onCopyRequested
            MENU_ID_PASTE -> onPasteRequested
            MENU_ID_CUT -> onCutRequested
            MENU_ID_SELECT_ALL -> onSelectAllRequested
            else -> null
          } ?: return false
        action.invoke()
        hide()
        return true
      }

      override fun onDestroyActionMode(mode: ActionMode?) {
        isFinishingOrDestroying = true
        try {
          if (actionMode === mode) {
            actionMode = null
          }
          status = TextToolbarStatus.Hidden
          clearCallbacks()
        } finally {
          isFinishingOrDestroying = false
        }
      }

      override fun onGetContentRect(mode: ActionMode?, view: View?, outRect: AndroidRect?) {
        outRect?.set(
          currentRect.left.roundToInt(),
          currentRect.top.roundToInt(),
          currentRect.right.roundToInt(),
          currentRect.bottom.roundToInt(),
        )
      }
    }

  init {
    view.addOnAttachStateChangeListener(this)
  }

  override fun showMenu(
    rect: Rect,
    onCopyRequested: (() -> Unit)?,
    onPasteRequested: (() -> Unit)?,
    onCutRequested: (() -> Unit)?,
    onSelectAllRequested: (() -> Unit)?,
  ) {
    if (!view.isAttachedToWindow) {
      hide()
      return
    }
    currentRect = rect
    this.onCopyRequested = onCopyRequested
    this.onPasteRequested = onPasteRequested
    this.onCutRequested = onCutRequested
    this.onSelectAllRequested = onSelectAllRequested

    val existing = actionMode
    if (existing == null && !isFinishingOrDestroying) {
      status = TextToolbarStatus.Shown
      actionMode = runCatching { view.startActionMode(callback, ActionMode.TYPE_FLOATING) }.getOrNull()
      if (actionMode == null) {
        status = TextToolbarStatus.Hidden
      }
    } else if (existing != null) {
      status = TextToolbarStatus.Shown
      runCatching {
        existing.invalidate()
        existing.invalidateContentRect()
      }
    }
  }

  override fun hide() {
    status = TextToolbarStatus.Hidden
    val mode = actionMode
    actionMode = null
    clearCallbacks()
    if (mode != null && !isFinishingOrDestroying && view.isAttachedToWindow) {
      isFinishingOrDestroying = true
      try {
        runCatching { mode.finish() }
      } finally {
        isFinishingOrDestroying = false
      }
    }
  }

  fun dispose() {
    view.removeOnAttachStateChangeListener(this)
    hide()
  }

  override fun onViewAttachedToWindow(v: View) = Unit

  override fun onViewDetachedFromWindow(v: View) {
    // Window/DecorView is already detaching and tearing down its own floating ActionMode.
    // Just clear our reference without invoking finish() on a detached DecorView.
    actionMode = null
    status = TextToolbarStatus.Hidden
    clearCallbacks()
  }

  private fun populateMenu(menu: Menu) {
    if (onCopyRequested != null) {
      menu.add(0, MENU_ID_COPY, 0, android.R.string.copy)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }
    if (onPasteRequested != null) {
      menu.add(0, MENU_ID_PASTE, 1, android.R.string.paste)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }
    if (onCutRequested != null) {
      menu.add(0, MENU_ID_CUT, 2, android.R.string.cut)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }
    if (onSelectAllRequested != null) {
      menu.add(0, MENU_ID_SELECT_ALL, 3, android.R.string.selectAll)
        .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM)
    }
  }

  private fun clearCallbacks() {
    onCopyRequested = null
    onPasteRequested = null
    onCutRequested = null
    onSelectAllRequested = null
  }

  private companion object {
    const val MENU_ID_COPY = 0
    const val MENU_ID_PASTE = 1
    const val MENU_ID_CUT = 2
    const val MENU_ID_SELECT_ALL = 3
  }
}

@Composable
fun ProvideSafeTextToolbar(content: @Composable () -> Unit) {
  val view = LocalView.current
  val safeToolbar = remember(view) { SafeTextToolbar(view) }
  DisposableEffect(safeToolbar) {
    onDispose { safeToolbar.dispose() }
  }
  CompositionLocalProvider(LocalTextToolbar provides safeToolbar) {
    content()
  }
}
