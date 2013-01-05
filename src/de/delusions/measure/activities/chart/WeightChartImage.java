/**
 * 
 */
package de.delusions.measure.activities.chart;

import java.text.SimpleDateFormat;
import java.util.Date;

import android.content.Context;
import android.graphics.*;
import android.graphics.Paint.Style;
import android.util.DisplayMetrics;
import android.util.Log;
import de.delusions.measure.MeasureActivity;
import de.delusions.measure.R;
import de.delusions.measure.activities.bmi.BMI;
import de.delusions.measure.activities.bmi.StatisticsFactory;
import de.delusions.measure.activities.prefs.UserPreferences;
import de.delusions.measure.ment.MeasureType;
import de.delusions.measure.ment.Measurement;

/**
 * 
 * @author sonja.pieper@workreloaded.com, 2013
 */
public class WeightChartImage {

    private static final String TAG = "WeightChartImage";

    private static final int PADDING = 30;
    private static final int MIN_IMAGE_WIDTH = 400;
    private static final int MIN_IMAGE_HEIGHT = 400;
    private static final int TEXTSIZE = 20;
    private static Paint BACKGROUND = createPaint(Color.WHITE, Paint.Style.FILL);
    private static Paint GRID = createPaint(Color.GRAY, Paint.Style.STROKE);
    private static final int SEGMENTS = 5;
    private static final SimpleDateFormat DATE_LABEL_FORMAT = new SimpleDateFormat("dd/MM");

    private int imageWidth;
    private int imageHeight;
    private ChartCoordinates coords;
    private int days;

    private MeasureType displayField;
    private MeasurePath trackedValuePath;
    private boolean showAll;

    private Context context;

    public WeightChartImage(final Context context, final int days) {
        this.context = context;
        this.days = days;
    }

    public int getDays() {
        return this.days;
    }

    public void setDays(final int days) {
        this.days = days;
    }

    public MeasureType getDisplayField() {
        return this.displayField;
    }

    public void setDisplayField(final MeasureType displayField) {
        this.displayField = displayField;
    }

    public MeasurePath getTrackedValuePath() {
        return this.trackedValuePath;
    }

    private void setTrackedValuePath(final MeasurePath trackedValuePath) {
        this.trackedValuePath = trackedValuePath;
    }

    public boolean isShowAll() {
        return this.showAll;
    }

    public void setShowAll(final boolean showAll) {
        this.showAll = showAll;
    }

    public int getImageWidth() {
        return this.imageWidth;
    }

    private void setImageWidth(final int imageWidth) {
        this.imageWidth = imageWidth;
    }

    public int getImageHeight() {
        return this.imageHeight;
    }

    private void setImageHeight(final int imageHeight) {
        this.imageHeight = imageHeight;
    }

    public void calculateImageDimensions(final DisplayMetrics metrics, final int orientation) {

        final int tabHostAndButtonsSpace = orientation == 0 ? 400 : 135;
        final int metricWidth = metrics.widthPixels - PADDING;
        final int metricHeight = metrics.heightPixels - tabHostAndButtonsSpace;
        setImageWidth(Math.max(MIN_IMAGE_WIDTH, metricWidth));
        setImageHeight(Math.max(MIN_IMAGE_HEIGHT, metricHeight));
        Log.d(MeasureActivity.TAG, "calculateImageDimensions " + " orientation:" + orientation + " width:" + getImageWidth() + " height:"
                + getImageHeight());
    }

    public Bitmap refreshGraph() {
        final Bitmap charty = createBitmap();
        final Canvas canvas = new Canvas(charty);
        drawBackgroundAndGrid(canvas);

        if (getDisplayField() == MeasureType.WEIGHT && !isShowAll()) {
            drawBmiLines(canvas);
            drawGoalLine(canvas);
        }
        getTrackedValuePath().fillPath(this.coords);
        canvas.drawPath(getTrackedValuePath(), createGraphPaint(getDisplayField(), 4));
        if (isShowAll()) {
            for (final MeasureType type : MeasureType.getEnabledTypes(this.context)) {
                if (type != getDisplayField()) {
                    Log.d(MeasureActivity.TAG, "Adding line for " + type);
                    final MeasurePath path = new MeasurePath(this.context, type, getDays());
                    path.fillPath(this.coords);
                    canvas.drawPath(path, createGraphPaint(type, 4));
                }
            }
            drawLegend(canvas);
        }
        return charty;
    }

    public void refreshData() {
        setTrackedValuePath(new MeasurePath(this.context, getDisplayField(), getDays()));
        if (this.coords == null) {
            final String label = formatHorizontalLabel(0);
            final int[] drawSizes = getDrawSizes(label);
            this.coords = new ChartCoordinates(getDays(), drawSizes);
        }
        this.coords.setDays(getDays());
    }

    private Bitmap createBitmap() {
        return Bitmap.createBitmap(getImageWidth(), getImageHeight(), Bitmap.Config.ARGB_8888);
    }

    private int[] getDrawSizes(final String label) {
        return createCoords(getImageWidth() - PADDING, getImageHeight() - PADDING, GRID, label);
    }

    /**
     * Rounded_max is the max value we want to plot and therefore the longest label on the y-axis
     * 
     * @param paint
     * @param rounded_max
     * 
     * @return
     */
    private static int[] createCoords(final int width, final int height, final Paint paint, final String maxLabel) {
        final int leftMargin = calculateTextWidth(paint, maxLabel);
        final int bottomMargin = calculateTextWidth(paint, "01/01");
        final int left = 2 + leftMargin;
        final int top = 25;
        final int right = width - 2 - leftMargin;
        final int bottom = height - top - bottomMargin;
        final int[] result = { left, top, right, bottom };
        return result;
    }

    private static Paint createPaint(final int color, final Paint.Style style) {
        final Paint paint = new Paint();
        paint.setTextSize(TEXTSIZE);
        paint.setStyle(style);
        paint.setColor(color);
        paint.setAntiAlias(true);
        return paint;
    }

    private static int calculateTextWidth(final Paint paint, final String label) {
        final float[] widths = new float[label.length()];
        paint.getTextWidths(label, widths);
        float sum = 0;
        for (final float w : widths) {
            sum += w;
        }
        return (int) sum;
    }

    private void drawLegend(final Canvas canvas) {
        final float x = this.coords.getLeft() + 10;
        float y = this.coords.getTop() + 10 + TEXTSIZE;
        for (final MeasureType type : MeasureType.getEnabledTypes(this.context)) {
            final Paint paint = WeightChartImage.createPaint(type.getColor(), Style.FILL);
            canvas.drawText(this.context.getResources().getString(type.getLabelId()), x, y, paint);
            y = y + TEXTSIZE + 5;
        }
    }

    private void drawBmiLines(final Canvas canvas) {
        final Measurement height = UserPreferences.getHeight(this.context);
        final Paint paint = WeightChartImage.createPaint(Color.WHITE, Paint.Style.STROKE);
        for (final BMI bmi : BMI.values()) {
            final int bmiValue = bmi.getCeiling(this.context);
            final Measurement bmiWeight = StatisticsFactory.calculateBmiWeight(bmiValue, height);
            final float value = bmiWeight.getValue(UserPreferences.isMetric(this.context));
            if (value < getTrackedValuePath().getCeiling() && value > getTrackedValuePath().getFloor()) {
                paint.setColor(bmi.getColor(this.context));

                final Path bmiPath = createHorizontalPath(value);
                canvas.drawPath(bmiPath, paint);

                final String labelStr = "BMI " + bmiValue;
                final Point label = createStartPointForHorizontalLabel(paint, labelStr, value);
                canvas.drawText(labelStr, label.x, label.y, paint);
            }
        }
    }

    private void drawGoalLine(final Canvas canvas) {
        final String labelStr = "Goal";
        final float value = UserPreferences.getGoal(this.context).getValue(UserPreferences.isMetric(this.context));
        final Paint paint = WeightChartImage.createPaint(this.context.getResources().getColor(R.color.yourgoal), Paint.Style.STROKE);
        final Path goalPath = createHorizontalPath(value);
        final Point label = createStartPointForHorizontalLabel(paint, labelStr, value);
        canvas.drawPath(goalPath, paint);
        canvas.drawText(labelStr, label.x, label.y, paint);
    }

    private Path createHorizontalPath(final float value) {
        final Point start = calculatePoint(0, value);
        final Point end = calculatePoint(getDays(), value);
        final Path bmiLine = new Path();
        bmiLine.moveTo(start.x, start.y);
        bmiLine.lineTo(end.x, end.y);
        return bmiLine;
    }

    private Point calculatePoint(final double x, final double y) {
        return this.coords.calculatePoint(x, y, getTrackedValuePath().getCeiling(), getTrackedValuePath().getFloor());
    }

    private Point createStartPointForHorizontalLabel(final Paint paint, final String label, final float value) {
        final int paddingRight = 5;
        final int paddingBottom = 3;
        final int textWidth = Math.round(WeightChartImage.calculateTextWidth(paint, label));
        final Point lineEnd = calculatePoint(getDays(), value);
        final Point result = new Point();
        result.x = lineEnd.x - textWidth - paddingRight;
        result.y = lineEnd.y - paddingBottom;
        return result;
    }

    private void drawBackgroundAndGrid(final Canvas canvas) {
        canvas.drawRect(this.coords.getLeft(), this.coords.getTop(), this.coords.getLeft() + this.coords.getRight(), this.coords.getTop()
                + this.coords.getBottom(), BACKGROUND);
        for (int segment = 0; segment <= SEGMENTS; segment++) {
            drawGridLine(canvas, GRID, segment, false);
            drawGridLine(canvas, GRID, segment, true);
            if (!isShowAll()) {
                drawGridLabel(canvas, GRID, segment, false, formatVerticalLabel(segment));
            }
            drawGridLabel(canvas, GRID, segment, true, formatHorizontalLabel(segment));
        }
    }

    private String formatVerticalLabel(final int segment) {
        final int displayed = getTrackedValuePath().labelVerticalMeasureValue(segment, SEGMENTS);
        return String.format("%d %s", displayed, getDisplayField().getUnit().retrieveUnitName(this.context));
    }

    private String formatHorizontalLabel(final int segment) {
        final Date date = getTrackedValuePath().labelHorizontalDate(getDays(), segment, SEGMENTS);
        return DATE_LABEL_FORMAT.format(date);
    }

    private Paint createGraphPaint(final MeasureType type, final int strokeWidth) {
        final Paint paint = WeightChartImage.createPaint(type.getColor(), Paint.Style.STROKE);
        paint.setStrokeWidth(strokeWidth);
        return paint;
    }

    private void drawGridLine(final Canvas canvas, final Paint paint, final int segment, final boolean vertical) {
        final float startX = this.coords.getLeft() + (vertical ? segment * this.coords.getRight() / SEGMENTS : 0);
        final float startY = this.coords.getTop() + (vertical ? 0 : segment * this.coords.getBottom() / SEGMENTS);
        final float stopX = this.coords.getLeft() + (vertical ? segment * this.coords.getRight() / SEGMENTS : this.coords.getRight());
        final float stopY = this.coords.getTop() + (vertical ? this.coords.getBottom() : segment * this.coords.getBottom() / SEGMENTS);
        canvas.drawLine(startX, startY, stopX, stopY, paint);
    }

    private void drawGridLabel(final Canvas canvas, final Paint paint, final int segment, final boolean vertical, final String label) {
        final float textSize = paint.getTextSize();
        final float textWidth = WeightChartImage.calculateTextWidth(paint, label);
        final float x = this.coords.getLeft() + (vertical ? segment * this.coords.getRight() / SEGMENTS : -textWidth - 2);
        final float y = this.coords.getTop() + (vertical ? this.coords.getBottom() + textSize : segment * this.coords.getBottom() / SEGMENTS);

        if (vertical) {
            canvas.rotate(60, x, y);
            canvas.drawText(label, x, y, paint);
            canvas.rotate(-60, x, y);
        } else {
            canvas.drawText(label, x, y, paint);
        }

    }

}
