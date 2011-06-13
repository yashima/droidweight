/*
   Copyright 2011 Sonja Pieper

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

     http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package de.delusions.measure.activities.chart;

import android.graphics.Point;

public class ChartCoordinates {

    private static final int LEFT = 0;
    private static final int TOP = 1;
    private static final int RIGHT = 2;
    private static final int BOTTOM = 3;

    private final int[] drawSizes;

    private int maxX;
    private final int minX = 0;

    public ChartCoordinates(int days, int[] drawSizes) {
        this.maxX = days;
        this.drawSizes = drawSizes;
    }

    public void setDays(int days) {
        this.maxX = days;
    }

    public Point calculatePoint(double x, double y, int ceiling, int floor) {
        final int maxY = ceiling;
        final int minY = floor;
        final Point result = new Point();
        result.x = this.drawSizes[LEFT] + (int) ((x - this.minX) * this.drawSizes[RIGHT] / (this.maxX - this.minX));
        result.y = this.drawSizes[TOP] + (int) ((maxY - y) * this.drawSizes[BOTTOM] / (maxY - minY));
        return result;

    }

    public int getTop() {
        return this.drawSizes[TOP];
    }

    public int getBottom() {
        return this.drawSizes[BOTTOM];
    }

    public int getLeft() {
        return this.drawSizes[LEFT];
    }

    public int getRight() {
        return this.drawSizes[RIGHT];
    }
}
