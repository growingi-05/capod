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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
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
        WindowManager.LayoutParams.MATCH_PARENT,
        // 빈 공간 터치 인식을 위해 창 높이를 전체화면으로 확장
        WindowManager.LayoutParams.MATCH_PARENT, 
        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
        WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
        PixelFormat.TRANSLUCENT
    ).apply {
        gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
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
                            
                            LaunchedEffect(Unit) {
                                visibleState.value = true
                            }

                            val isVisible = visibleState.value
                            val animatedOffset by animateFloatAsState(
                                targetValue = if (isVisible) 0f else 1f,
                                animationSpec = if (isVisible) {
                                    spring(
                                        dampingRatio = 0.65f, 
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                } else {
                                    tween(250)
                                },
                                label = "popup_animation"
                            )

                            // 1. 최상단(루트) Box: 화면 전체를 덮으며, 탭(터치)할 경우 창 닫기 수행
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectTapGestures(onTap = { close() })
                                    }
                            ) {
                                // 2. 내부 Box: 하단에 붙어서 애니메이션으로 위아래로 움직이는 컨텐츠
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter) // 카드를 화면 아래로 배치
                                        .fillMaxWidth()
                                        .graphicsLayer {
                                            translationY = size.height * animatedOffset
                                            alpha = (1f - animatedOffset).coerceIn(0f, 1f)
                                        }
                                ) {
                                    PopUpContent(device = currentDevice, onClose = { close() })
                                }
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
        
        isVisibleState?.value = false 
        
        Handler(Looper.getMainLooper()).postDelayed({
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
