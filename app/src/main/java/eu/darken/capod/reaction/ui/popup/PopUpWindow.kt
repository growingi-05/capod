package eu.darken.capod.reaction.ui.popup

import android.content.Context
import android.content.Context.WINDOW_SERVICE
import android.graphics.PixelFormat
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
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
    // 애니메이션 트리거를 위한 상태 변수 추가
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
                // 이미 뷰가 있다면 애니메이션 상태만 확실히 켜줌
                isVisibleState?.value = true
                return
            }

            teardown()

            val state = mutableStateOf<PodDevice?>(device)
            deviceState = state
            
            // 처음엔 false로 시작 (그려진 직후에 true로 바꿔서 애니메이션 발생)
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
                            // 🍎 여기서 애플 감성의 스프링 애니메이션 적용!
                            AnimatedVisibility(
                                visible = visibleState.value,
                                enter = slideInVertically(
                                    initialOffsetY = { it }, // 아래에서 출발
                                    animationSpec = spring(
                                        dampingRatio = 0.65f, // 적당히 통통 튀는 탄성
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                ) + fadeIn(),
                                exit = slideOutVertically(
                                    targetOffsetY = { it },
                                    animationSpec = tween(250) // 내려갈 땐 깔끔하고 빠르게
                                ) + fadeOut()
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
            
            // 뷰가 WindowManager에 추가된 직후에 상태를 true로 변경하여 올라오는 애니메이션 실행
            Handler(Looper.getMainLooper()).post {
                visibleState.value = true
            }
        } catch (e: Exception) {
            log(TAG, ERROR) { "open() failed: ${e.asLog()}" }
        }
    }

    fun close() = try {
        log(TAG, INFO) { "close()" }
        
        // 닫기 명령이 오면 뷰를 바로 없애지 않고 퇴장 애니메이션 시작
        isVisibleState?.value = false 
        
        // 애니메이션이 끝날 시간(약 300ms)을 기다렸다가 실제로 뷰를 제거
        Handler(Looper.getMainLooper()).postDelayed({
            if (composeView?.parent != null) {
                teardown()
            } else {
                log(TAG) { "View was not added" }
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
