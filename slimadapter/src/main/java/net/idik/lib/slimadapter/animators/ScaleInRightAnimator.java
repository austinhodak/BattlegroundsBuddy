package net.idik.lib.slimadapter.animators;

/**
 * Copyright (C) 2018 Wasabeef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.view.animation.Interpolator;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

public class ScaleInRightAnimator extends BaseItemAnimator {

  public ScaleInRightAnimator() {
  }

  public ScaleInRightAnimator(Interpolator interpolator) {
    mInterpolator = interpolator;
  }

  @Override protected void preAnimateRemoveImpl(RecyclerView.ViewHolder holder) {
    ViewCompat.setPivotX(holder.itemView, holder.itemView.getWidth());
  }

  @Override protected void animateRemoveImpl(final RecyclerView.ViewHolder holder) {
    ViewCompat.animate(holder.itemView)
        .scaleX(0)
        .scaleY(0)
        .setDuration(getRemoveDuration())
        .setInterpolator(mInterpolator)
        .setListener(new DefaultRemoveVpaListener(holder))
        .setStartDelay(getRemoveDelay(holder))
        .start();
  }

  @Override protected void preAnimateAddImpl(RecyclerView.ViewHolder holder) {
    ViewCompat.setPivotX(holder.itemView, holder.itemView.getWidth());
    ViewCompat.setScaleX(holder.itemView, 0);
    ViewCompat.setScaleY(holder.itemView, 0);
  }

  @Override protected void animateAddImpl(final RecyclerView.ViewHolder holder) {
    ViewCompat.animate(holder.itemView)
        .scaleX(1)
        .scaleY(1)
        .setDuration(getAddDuration())
        .setInterpolator(mInterpolator)
        .setListener(new DefaultAddVpaListener(holder))
        .setStartDelay(getAddDelay(holder))
        .start();
  }
}
