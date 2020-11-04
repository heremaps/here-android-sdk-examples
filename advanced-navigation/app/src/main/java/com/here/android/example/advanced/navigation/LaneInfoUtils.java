/*
 * Copyright (c) 2011-2020 HERE Europe B.V.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.here.android.example.advanced.navigation;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.here.android.mpa.guidance.LaneInformation;

import java.util.EnumSet;
import java.util.List;

public class LaneInfoUtils {

    public static void displayLaneInformation(ViewGroup container, List<LaneInformation> lanes) {
        container.removeAllViews();
        if (lanes.isEmpty()) {
            container.setVisibility(View.GONE);
            return; // nothing to display
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        container.setLayoutParams(params);
        Context context = container.getContext();
        final String packageName = context.getPackageName();

        for (LaneInformation lane : lanes) {
            long directionsBitmask = convertDirectionToBitMask(lane.getDirections());

            FrameLayout laneView = new FrameLayout(context);
            ImageView laneDirectionsView = new ImageView(context);

            int resourceId = context.getResources().getIdentifier(
                    "laneinfo_" + Long.toString(directionsBitmask), "drawable", packageName);
            if (resourceId == 0) {
                continue;
            }

            laneDirectionsView.setImageDrawable(context.getResources()
                    .getDrawable(resourceId, null));
            laneDirectionsView.setColorFilter(Color.parseColor("#FFC4C4C4"));

            switch (lane.getRecommendationState()) {
                case NOT_RECOMMENDED:
                    laneDirectionsView.setBackgroundColor(Color.RED);
                    break;
                case RECOMMENDED:
                    laneDirectionsView.setBackgroundColor(Color.YELLOW);
                    break;
                case HIGHLY_RECOMMENDED:
                    laneDirectionsView.setBackgroundColor(Color.GREEN);
                    break;
                case NOT_AVAILABLE:
                    laneDirectionsView.setBackgroundColor(Color.GRAY);
                    break;
            }

            laneView.addView(laneDirectionsView);
            highlightRequiredDirection(laneView, lane);
            container.addView(laneView);
        }
        container.setVisibility(View.VISIBLE);
    }

    private static void highlightRequiredDirection(FrameLayout laneView, LaneInformation lane) {
        LaneInformation.Direction direction = lane.getMatchedDirection();
        if (direction == LaneInformation.Direction.UNDEFINED) {
            return;
        }

        long directionsBitmask = convertDirectionToBitMask(EnumSet.of(direction));
        int resourceId = laneView.getResources().getIdentifier(
                "laneinfo_" + Long.toString(directionsBitmask), "drawable",
                laneView.getContext().getPackageName());

        ImageView matchedDirection = new ImageView(laneView.getContext());
        try {
            matchedDirection.setImageDrawable(laneView.getResources()
                    .getDrawable(resourceId, null));
        } catch (Resources.NotFoundException e) {
            // Currently, we don't have assets for all possible combinations of lane directions
            return;
        }

        laneView.addView(matchedDirection);
    }

    private static long convertDirectionToBitMask(EnumSet<LaneInformation.Direction> directions) {
        long bitmask = 0;
        for (LaneInformation.Direction dir : directions) {
            bitmask |= dir.value();
        }
        return bitmask;
    }
}
