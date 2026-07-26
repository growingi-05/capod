package eu.darken.capod.reaction.ui.popup

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.android.qualifiers.ApplicationContext
import eu.darken.capod.common.debug.logging.Logging.Priority.ERROR
import eu.darken.capod.common.debug.logging.Logging.Priority.INFO
import eu.darken.capod.common.debug.logging.asLog
import eu.darken.capod.common.debug.logging.log
import eu.darken.capod.common.debug.logging.logTag
import eu.darken.capod.common.theming.CapodTheme
import eu.darken.capod.main.core.GeneralSettings
import eu.darken.capod.main.core.currentThemeState
import eu.darken.capod.monitor.core.PodDevice
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PopUpWindow @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val generalSettings: GeneralSettings,
) {

    private val windowManager = appContext.getSystemService(WINDOW_SERVICE) as WindowManager
    private val layoutParams = WindowManager.LayoutParams(
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.WRAP_CONTENT,
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        val dm = appContext.resources.displayMetrics
        val margin = (24 * dm.density).toInt()
        width = minOf(dm.widthPixels - margin * 2, (400 * dm.density).toInt())
        y = (8 * dm.density).toInt()
    }

    private var composeView: ComposeView? = null
    private var lifecycleOwner: OverlayLifecycleOwner? = null
    private var deviceState: MutableState<PodDevice?>? = null
    private var isVisibleState: MutableState<Boolean>? = null

    @Volatile var isMainActivityVisible: Boolean = false

    fun show(device: PodDevice) {
        if (isMainActivityVisible) {
            log(TAG) { "Suppressing popup, MainActivity is visible" }
            return
        }
        try {
            log(TAG, INFO) { "open()" }

            if (composeView?.parent != null && deviceState != null) {
                log(TAG) { "Popup already visible, updating device." }
                deviceState?.value = device
                isVisibleState?.value = true
                return
            }

            teardown()

            val state = mutableStateOf<PodDevice?>(device)
            deviceState = state
            
            val visibleState = mutableStateOf(false)
            isVisibleState = visibleState

            val owner = OverlayLifecycleOwner()
            lifecycleOwner = owner

            val view = ComposeView(appContext).apply {
                setViewTreeLifecycleOwner(owner)
                setViewTreeSavedStateRegistryOwner(owner)
                setContent {
                    val currentDevice = state.value
                    if (currentDevice != null) {
                        CapodTheme(state = generalSettings.currentThemeState) {
                            
                            // Compose가 그려질 준비가 되면 애니메이션 시작
                            LaunchedEffect(Unit) {
                                visibleState.value = true
                            }

                            val isVisible = visibleState.value
                            // 0f(제자리/완전보임) <-> 1f(아래로 숨김/투명) 사이를 애니메이션
                            val animatedOffset by animateFloatAsState(
                                targetValue = if (isVisible) 0f else 1f,
                                animationSpec = if (isVisible) {
                                    spring(
                                        dampingRatio = 0.65f, // 애플 특유의 통통 튀는 텐션
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                } else {
                                    tween(250) // 내려갈 땐 0.25초만에 부드럽게
                                },
                                label = "popup_animation"
                            )

                            // WindowManager 크기는 풀사이즈로 유지하되, 내부 내용물만 위아래로 이동시킴
                            Box(
                                modifier = Modifier.graphicsLayer {
                                    translationY = size.height * animatedOffset
                                    alpha = 1f - animatedOffset
                                }
                            ) {
                                PopUpContent(device = currentDevice, onClose = { close() })
                            }
                        }
                    }
                }
            }
            composeView = view

            owner.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_START)
            owner.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)

            windowManager.addView(view, layoutParams)
        } catch (e: Exception) {
            log(TAG, ERROR) { "open() failed: ${e.asLog()}" }
        }
    }

    fun close() = try {
        log(TAG, INFO) { "close()" }
        
        // 1. 퇴장 애니메이션 시작 (아래로 내려감)
        isVisibleState?.value = false 
        
        // 2. 애니메이션이 끝날 때까지 기다렸다가 화면에서 뷰를 제거
        Handler(Looper.getMainLooper()).postDelayed({
            // 그 사이(0.3초)에 팝업이 다시 열렸다면 파괴하지 않음 (안전 장치)
            if (isVisibleState?.value == false) {
                if (composeView?.parent != null) {
                    teardown()
                } else {
                    log(TAG) { "View was not added" }
                }
            }
        }, 300)
    } catch (e: Exception) {
        log(TAG, ERROR) { "close() failed: ${e.asLog()}" }
    }

    private fun teardown() {
        lifecycleOwner?.let { existing ->
            existing.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            existing.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
            existing.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        }
        if (composeView?.parent != null) {
            windowManager.removeView(composeView)
        }
        composeView = null
        lifecycleOwner = null
        deviceState = null
        isVisibleState = null
    }

    private class OverlayLifecycleOwner : SavedStateRegistryOwner {
        private val lifecycleRegistry = LifecycleRegistry(this)
        private val savedStateRegistryController = SavedStateRegistryController.create(this)

        init {
            savedStateRegistryController.performRestore(null)
        }

        override val lifecycle: Lifecycle get() = lifecycleRegistry
        override val savedStateRegistry: SavedStateRegistry get() = savedStateRegistryController.savedStateRegistry

        fun handleLifecycleEvent(event: Lifecycle.Event) {
            lifecycleRegistry.handleLifecycleEvent(event)
        }
    }

    companion object {
        private val TAG = logTag("Reaction", "PopUp", "Window")
    }
}
