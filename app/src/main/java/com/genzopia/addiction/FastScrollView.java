package com.genzopia.addiction;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FastScrollView extends View {

    private List<Section> sections = new ArrayList<>();
    private RecyclerView recyclerView;
    private Paint textPaint;
    private boolean isScrolling = false;
    private int lastSectionIndex = -1;

    private int selectedSectionIndex = -1;
    private float touchY = -1;
    private Paint selectedTextPaint;
    private Paint circlePaint;
    private float circleRadius;
    private float selectedTextSize;

    public FastScrollView(Context context) {
        super(context);
        init();
    }
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // Maintain fixed width regardless of keyboard state
        int width = MeasureSpec.makeMeasureSpec((int) TypedValue.applyDimension(
                        TypedValue.COMPLEX_UNIT_DIP, 36, getResources().getDisplayMetrics()),
                MeasureSpec.EXACTLY);

        super.onMeasure(width, heightMeasureSpec);
    }

    public FastScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        textPaint = new Paint();
        textPaint.setAntiAlias(true);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.WHITE);
        textPaint.setTextSize(getTextSizeInPixels());
        textPaint.setTypeface(Typeface.DEFAULT_BOLD);

        // Initialize selected text paint
        selectedTextSize = getTextSizeInPixels() * 1.5f; // 50% larger
        selectedTextPaint = new Paint(textPaint);
        selectedTextPaint.setTextSize(selectedTextSize);

        // Initialize circle paint
        circlePaint = new Paint();
        circlePaint.setColor(Color.argb(128, 0, 0, 0)); // Semi-transparent black
        circlePaint.setAntiAlias(true);
        circleRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
    }
    private float getTextSizeInPixels() {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics());
    }

    private void updateTextColor() {
        TypedValue typedValue = new TypedValue();
        getContext().getTheme().resolveAttribute(android.R.attr.textColorPrimary, typedValue, true);
        textPaint.setColor(typedValue.data);
    }

    public void setSections(List<Section> sections) {
        this.sections = sections;
        invalidate();
    }

    public void setRecyclerView(RecyclerView recyclerView) {
        this.recyclerView = recyclerView;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (sections.isEmpty()) return;

        float height = getHeight() - getPaddingTop() - getPaddingBottom();
        float sectionHeight = height / sections.size();
        float width = getWidth();
        float x = width / 2;

        // Draw all non-selected sections
        for (int i = 0; i < sections.size(); i++) {
            if (i == selectedSectionIndex) continue;

            Section section = sections.get(i);
            float baselineOffset = (textPaint.descent() - textPaint.ascent()) / 2 - textPaint.descent();
            float y = sectionHeight * i + sectionHeight / 2 + getPaddingTop() + baselineOffset;
            canvas.drawText(section.letter, x, y, textPaint);
        }

        // Draw selected section at touch position
        if (selectedSectionIndex != -1 && touchY != -1 && selectedSectionIndex < sections.size()) {
            Section selected = sections.get(selectedSectionIndex);

            // Clamp touchY within view bounds
            float clampedY = Math.max(getPaddingTop() + circleRadius,
                    Math.min(touchY, getHeight() - getPaddingBottom() - circleRadius));

            // Draw background circle
            canvas.drawCircle(x, clampedY, circleRadius, circlePaint);

            // Calculate text position
            float baselineOffset = (selectedTextPaint.descent() - selectedTextPaint.ascent()) / 2 - selectedTextPaint.descent();
            float textY = clampedY + baselineOffset;

            // Draw selected letter
            canvas.drawText(selected.letter, x, textY, selectedTextPaint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (sections.isEmpty() || recyclerView == null) return false;

        float y = event.getY();
        int sectionIndex = Math.max(0, Math.min((int) (y / getHeight() * sections.size()), sections.size() - 1));

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                isScrolling = true;
                touchY = y;
                selectedSectionIndex = sectionIndex;
                scrollToSection(sectionIndex);
                invalidate();
                return true;

            case MotionEvent.ACTION_MOVE:
                if (isScrolling) {
                    touchY = y;
                    selectedSectionIndex = sectionIndex;
                    scrollToSection(sectionIndex);
                    invalidate();
                }
                return true;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                isScrolling = false;
                lastSectionIndex = -1;
                selectedSectionIndex = -1;
                touchY = -1;
                invalidate();
                return true;
        }

        return super.onTouchEvent(event);
    }
    private void scrollToSection(int sectionIndex) {
        if (sectionIndex != lastSectionIndex && sectionIndex < sections.size()) {
            lastSectionIndex = sectionIndex;
            FastScrollView.Section section = sections.get(sectionIndex);

            // Use immediate scroll instead of smooth scroll for better tracking
            RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
            if (layoutManager instanceof LinearLayoutManager) {
                ((LinearLayoutManager) layoutManager).scrollToPositionWithOffset(
                        section.position,
                        0  // Offset from top
                );
            }
        }
    }

    public static class Section {
        public String letter;
        public int position;

        public Section(String letter, int position) {
            this.letter = letter;
            this.position = position;
        }
    }
}