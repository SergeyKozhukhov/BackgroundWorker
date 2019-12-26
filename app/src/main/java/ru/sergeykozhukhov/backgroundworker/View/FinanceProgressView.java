package ru.sergeykozhukhov.backgroundworker.View;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import ru.sergeykozhukhov.backgroundworker.R;

/**
 * @author Andrey Kudryavtsev on 2019-10-08.
 */
public class FinanceProgressView extends View {

    public static final int MAX_PROGRESS = 100;

    private static final String TAG = "FinanceProgressView";

    private static final float START_ANGLE = -90f;
    private static final int MAX_ANGLE = 360;

    private int mProgress;
    private int mInactiveColor;
    private int mColor;
    private int mTextSize;
    private int mStrokeWidth;

    private Paint mInactiveCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mCirclePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint mTextPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private RectF mProgressRect = new RectF();
    private Rect mTextBounds = new Rect();

    public FinanceProgressView(Context context) {
        super(context);
        init(context, null);
    }

    public FinanceProgressView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.translate(mStrokeWidth / 2, mStrokeWidth / 2);
        updateProgressRect();
        canvas.drawArc(mProgressRect, START_ANGLE, MAX_ANGLE, false, mInactiveCirclePaint);
        canvas.drawArc(mProgressRect, START_ANGLE, mProgress * MAX_ANGLE / MAX_PROGRESS, false, mCirclePaint);
        drawText(canvas);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        Log.d(TAG, "onMeasure() called with: widthMeasureSpec = [" + MeasureSpec.toString(widthMeasureSpec) + "]," +
                " heightMeasureSpec = [" + MeasureSpec.toString(heightMeasureSpec) + "]");
        getTextBounds(formatString(MAX_PROGRESS));
        // PI не трогать!!!
        int requestedSize = (int) (Math.max(mTextBounds.width(), mTextBounds.height()) + Math.PI * mStrokeWidth);
        final int suggestedMinimumSize = Math.max(getSuggestedMinimumHeight(), getSuggestedMinimumWidth());
        requestedSize = Math.max(suggestedMinimumSize, requestedSize);
        final int resolvedWidth = resolveSize(requestedSize + getPaddingLeft() + getPaddingRight(), widthMeasureSpec);
        final int resolvedHeight = resolveSize(requestedSize + getPaddingTop() + getPaddingBottom(), heightMeasureSpec);
        final int resolvedSize = Math.min(resolvedHeight, resolvedWidth);
        setMeasuredDimension(resolvedSize, resolvedSize);
    }

    @Nullable
    @Override
    protected Parcelable onSaveInstanceState() {
        final Parcelable superState = super.onSaveInstanceState();
        final SavedState savedState = new SavedState(superState);
        savedState.mProgress = mProgress;
        return savedState;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        final SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        mProgress = savedState.mProgress;
    }

    // @Override
    // protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    //     Log.d(TAG, "onSizeChanged() called with: w = [" + w + "], h = [" + h + "], oldw = [" + oldw + "], oldh = [" + oldh + "]");
    //     mProgressRect.right = w - STROKE_WIDTH;
    //     mProgressRect.bottom = h - STROKE_WIDTH;
    // }

    private void drawText(Canvas canvas) {
        final String progressString = formatString(mProgress);
        getTextBounds(progressString);
        float x = mProgressRect.width() / 2f - mTextBounds.width() / 2f - mTextBounds.left + mProgressRect.left;
        float y = mProgressRect.height() / 2f + mTextBounds.height() / 2f - mTextBounds.bottom + mProgressRect.top;
        canvas.drawText(progressString, x, y, mTextPaint);
    }

    private void updateProgressRect() {
        mProgressRect.left = getPaddingLeft();
        mProgressRect.top = getPaddingTop();
        mProgressRect.right = getWidth() - mStrokeWidth - getPaddingRight();
        mProgressRect.bottom = getHeight() - mStrokeWidth - getPaddingBottom();
    }

    private void getTextBounds(@NonNull String progressString) {
        mTextPaint.getTextBounds(progressString, 0, progressString.length(), mTextBounds);
    }

    private String formatString(int progress) {
        return String.format(getResources().getString(R.string.progress_template), progress);
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        extractAttributes(context, attrs);
        configureInactiveCirclePaint();
        configureCirclePaint();
        configureTextPaint();
    }

    private void configureInactiveCirclePaint() {
        mInactiveCirclePaint.setStrokeWidth(mStrokeWidth);
        mInactiveCirclePaint.setStyle(Paint.Style.STROKE);
        mInactiveCirclePaint.setColor(mInactiveColor);
    }

    private void configureTextPaint() {
        mTextPaint.setTextSize(mTextSize);
        mTextPaint.setColor(mColor);
    }

    private void configureCirclePaint() {
        mCirclePaint.setStrokeWidth(mStrokeWidth);
        mCirclePaint.setStyle(Paint.Style.STROKE);
        mCirclePaint.setColor(mColor);
    }

    private void extractAttributes(@NonNull Context context, @Nullable AttributeSet attrs) {
        final Resources.Theme theme = context.getTheme();
        final TypedArray typedArray = theme.obtainStyledAttributes(attrs, R.styleable.FinanceProgressView,
                R.attr.financeProgressStyle, 0);
        try {
            mProgress = typedArray.getInteger(R.styleable.FinanceProgressView_progress, 0);
            mInactiveColor = typedArray.getColor(R.styleable.FinanceProgressView_inactiveColor,
                    ContextCompat.getColor(getContext(), R.color.colorInactive));
            mColor = typedArray.getColor(R.styleable.FinanceProgressView_color, ContextCompat.getColor(getContext(), R.color.colorAccent));
            mTextSize = typedArray.getDimensionPixelSize(R.styleable.FinanceProgressView_textSize,
                    getResources().getDimensionPixelSize(R.dimen.defaultTextSize));
            mStrokeWidth = typedArray.getDimensionPixelSize(R.styleable.FinanceProgressView_strokeWidth,
                    getResources().getDimensionPixelSize(R.dimen.defaultStrokeWidth));
            Log.d(TAG, "Progress = " + mProgress + ", " + "Color = " + mColor + ", textSize = " + mTextSize + ", strokeWidth = " + mStrokeWidth);
        } finally {
            typedArray.recycle();
        }
    }

    public int getProgress() {
        return mProgress;
    }

    public void setProgress(int progress) {
        Log.d("CUSTOM_VIEW", "progress = " + progress);
        mProgress = progress;
        invalidate();
    }

    public int getColor() {
        return mColor;
    }

    public void setColor(int color) {
        mColor = color;
    }

    public int getTextSize() {
        return mTextSize;
    }

    public void setTextSize(int textSize) {
        mTextSize = textSize;
    }

    static class SavedState extends BaseSavedState {

        private int mProgress;

        SavedState(Parcelable superState) {
            super(superState);
        }

        private SavedState(Parcel source) {
            super(source);
            mProgress = source.readInt();
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            super.writeToParcel(out, flags);
            out.writeInt(mProgress);
        }

        private static final class StateCreator implements Creator<SavedState> {

            @Override
            public SavedState createFromParcel(Parcel source) {
                return new SavedState(source);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        }
    }
}
