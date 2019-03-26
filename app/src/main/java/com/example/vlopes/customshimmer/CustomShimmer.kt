package com.example.vlopes.customshimmer

import android.animation.ValueAnimator
import android.annotation.TargetApi
import android.content.Context
import android.content.res.TypedArray
import android.graphics.*
import android.os.Build
import android.support.annotation.ColorInt
import android.support.annotation.Nullable
import android.support.v4.view.animation.PathInterpolatorCompat
import android.util.AttributeSet
import android.view.View
import android.view.animation.Interpolator
import android.widget.RelativeLayout

class CustomShimmer : RelativeLayout {

    private val mUpdateListener = ValueAnimator.AnimatorUpdateListener { postInvalidate() }
    private val mShimmerPaint = Paint()
    private val mContentPaint = Paint()
    private val mDrawRect = RectF()
    private var colors: IntArray? = null
    @ColorInt
    private var highlightColor: Int = 0
    @ColorInt
    private var baseColor: Int = 0
    @Nullable
    private var mValueAnimator: ValueAnimator? = null

    private val isShimmerStarted: Boolean
        get() = mValueAnimator != null && mValueAnimator!!.isStarted

    private val easeOutInterpolator: Interpolator
        get() = PathInterpolatorCompat.create(
            EASE_OUT_BEZIER[0], EASE_OUT_BEZIER[1],
            EASE_OUT_BEZIER[2], EASE_OUT_BEZIER[3]
        )

    constructor(context: Context) : super(context) {
        init(context, null)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    fun setHighlightColor(@ColorInt color: Int) {
        highlightColor = color
    }

    fun setBaseColor(@ColorInt color: Int) {
        baseColor = color
    }

    private fun init(context: Context, attrs: AttributeSet?) {

        setWillNotDraw(false)
        mShimmerPaint.isAntiAlias = true
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            setLayerType(View.LAYER_TYPE_HARDWARE, mContentPaint)
        } else {
            setLayerType(View.LAYER_TYPE_SOFTWARE, mContentPaint)
        }
        mShimmerPaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        updateShader()
        updateValueAnimator()
        postInvalidate()
        val array =
            context.obtainStyledAttributes(attrs, R.styleable.CustomShimmerFrameLayout, 0, 0)
        setColors(array)
    }

    private fun setColors(array: TypedArray?) {
        if (array != null) {
            if (array.hasValue(R.styleable.CustomShimmerFrameLayout_baseColor)) {
                setBaseColor(array.getColor(R.styleable.CustomShimmerFrameLayout_baseColor, 0))
            }

            if (array.hasValue(R.styleable.CustomShimmerFrameLayout_highlightColor)) {
                setHighlightColor(
                    array.getColor(
                        R.styleable.CustomShimmerFrameLayout_highlightColor,
                        0
                    )
                )
            }
        }

        colors = intArrayOf(baseColor, highlightColor, baseColor)
    }

    fun startShimmer() {
        if (mValueAnimator != null && !isShimmerStarted) {
            mValueAnimator?.start()
        }
    }

    fun stopShimmer() {
        if (mValueAnimator != null && isShimmerStarted) {
            mValueAnimator?.cancel()
        }
    }

    public override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        val width = width
        val height = height
        mDrawRect.set(
            (2 * -width).toFloat(),
            (2 * -height).toFloat(),
            (4 * width).toFloat(),
            (4 * height).toFloat()
        )
        updateShader()
    }

    public override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startShimmer()
    }

    public override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        stopShimmer()
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        drawShimmer(canvas)
    }

    private fun drawShimmer(canvas: Canvas) {
        val width = width.toFloat()
        val animatedValue = if (mValueAnimator != null) mValueAnimator?.animatedFraction else 0f
        // Vertical coordinate
        val dx = offset(-width, width, animatedValue!!)
        // Horizontal coordinate
        val dy = 0f
        val saveCount = canvas.save()
        canvas.translate(dx, dy)
        canvas.drawRect(mDrawRect, mShimmerPaint)
        canvas.restoreToCount(saveCount)
    }

    private fun updateShader() {
        val viewWidth = width
        val viewHeight = height

        if (viewWidth != 0 && viewHeight != 0) {
            val shader = LinearGradient(
                0f, 0f, (viewWidth / 4).toFloat(), 0f,
                colors!!, colorPositions, Shader.TileMode.CLAMP
            )
            mShimmerPaint.shader = shader
        }
    }

    private fun updateValueAnimator() {
        val started: Boolean
        if (mValueAnimator != null) {
            started = mValueAnimator!!.isStarted
            mValueAnimator?.cancel()
            mValueAnimator?.removeAllUpdateListeners()
        } else {
            started = false
        }

        mValueAnimator =
                ValueAnimator.ofFloat(0f, 1f + (mRepeatDelay / mAnimationDuration).toFloat())
        mValueAnimator?.repeatMode = ValueAnimator.RESTART
        mValueAnimator?.repeatCount = ValueAnimator.INFINITE
        mValueAnimator?.duration = mAnimationDuration + mRepeatDelay
        mValueAnimator?.addUpdateListener(mUpdateListener)
        mValueAnimator?.interpolator = easeOutInterpolator
        if (started) {
            mValueAnimator?.start()
        }
    }

    companion object {
        private val EASE_OUT_BEZIER = floatArrayOf(0f, 0f, 0.58f, 1f)
        private const val mAnimationDuration = 1000L
        private const val mRepeatDelay = 100L
        private val colorPositions = floatArrayOf(0f, 0.5f, 1f)

        private fun offset(start: Float, end: Float, percent: Float): Float {
            return start + (end - start) * percent
        }
    }

}
