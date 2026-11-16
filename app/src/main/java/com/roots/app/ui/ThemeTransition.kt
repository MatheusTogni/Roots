package com.roots.app.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import android.view.View
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.FrameLayout
import kotlin.math.hypot
import kotlin.math.max

/**
 * Transição da troca de tema: a "foto" da tela antiga fica por cima da tela já montada no tema novo, e um
 * círculo com a borda suave abre a partir do botão, revelando o tema novo. Em vez de uma troca seca,
 * o tema novo "escorre" pela tela inteira.
 */
object ThemeTransition {

    /** Tira uma foto da view (incluindo as barras do sistema, no caso da janela). Null se faltar memória. */
    fun capture(view: View): Bitmap? {
        if (view.width <= 0 || view.height <= 0) return null
        return try {
            val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
            bitmap
        } catch (e: OutOfMemoryError) {
            null
        }
    }

    /**
     * Cobre a janela com [snapshot] e abre um círculo a partir de ([cx], [cy]) que revela o que está por baixo.
     * [onProgress] recebe 0..1 a cada quadro; [onEnd] roda quando o efeito termina (a camada já foi removida).
     */
    fun reveal(
        activity: Activity,
        snapshot: Bitmap,
        cx: Float,
        cy: Float,
        onProgress: (Float) -> Unit,
        onEnd: () -> Unit
    ) {
        val decor = activity.window.decorView as ViewGroup
        val overlay = RevealOverlay(activity, snapshot, cx, cy)
        decor.addView(overlay, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))

        val animator = ValueAnimator.ofFloat(0f, 1f)
        animator.duration = 720
        // Sai devagar, acelera no meio e assenta no fim.
        animator.interpolator = PathInterpolator(0.35f, 0f, 0.15f, 1f)
        animator.addUpdateListener {
            val p = it.animatedValue as Float
            overlay.setProgress(p)
            onProgress(p)
        }
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                decor.removeView(overlay)
                overlay.release()
                onEnd()
            }
        })
        animator.start()
    }

    /** Foto da tela antiga com um buraco circular crescente (borda suave) por onde aparece a tela nova. */
    private class RevealOverlay(
        context: Context,
        private val snapshot: Bitmap,
        private val cx: Float,
        private val cy: Float
    ) : View(context) {

        private val feather = context.dpf(56f)
        private val maxRadius: Float
        private var progress = 0f

        // DST_OUT apaga da foto o que estiver sob o círculo; o degradê deixa a borda do buraco suave.
        private val holePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_OUT)
        }

        init {
            val w = snapshot.width.toFloat()
            val h = snapshot.height.toFloat()
            // Raio até o canto mais distante da origem: ao terminar, o círculo cobre a tela toda.
            maxRadius = hypot(max(cx, w - cx), max(cy, h - cy))
            // Camada própria: o recorte (DST_OUT) só pode agir sobre a foto, não sobre a tela de baixo.
            setLayerType(LAYER_TYPE_HARDWARE, null)
            // Engole os toques enquanto o efeito roda.
            isClickable = true
        }

        fun setProgress(value: Float) {
            progress = value
            invalidate()
        }

        fun release() {
            if (!snapshot.isRecycled) snapshot.recycle()
        }

        override fun onDraw(canvas: Canvas) {
            if (snapshot.isRecycled) return
            canvas.drawBitmap(snapshot, 0f, 0f, null)
            // O raio total inclui a faixa da borda suave, para o buraco ficar 100% aberto no fim.
            val r = (maxRadius + feather) * progress
            if (r <= 0f) return
            val solidUntil = ((r - feather) / r).coerceIn(0.01f, 0.99f)
            holePaint.shader = RadialGradient(
                cx,
                cy,
                r,
                intArrayOf(0xFF000000.toInt(), 0xFF000000.toInt(), 0x00000000),
                floatArrayOf(0f, solidUntil, 1f),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, r, holePaint)
        }
    }
}
