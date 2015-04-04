/*
 * Copyright 2013 Chris Banes
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
package com.tintinshare.views;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsoluteLayout;
import android.widget.FrameLayout;
import android.widget.Toast;

import com.tintinshare.Flags;
import com.tintinshare.PhotoController;
import com.tintinshare.R;
import com.tintinshare.listeners.OnFaceDetectionListener;
import com.tintinshare.listeners.OnPhotoTagTapListener;
import com.tintinshare.listeners.OnPhotoTagsChangedListener;
import com.tintinshare.model.Photo;
import com.tintinshare.model.PhotoTag;

import uk.co.senab.photoview.PhotoViewAttacher;

@SuppressLint("ViewConstructor")
@SuppressWarnings("deprecation")
public class PhotoTagItemLayout extends FrameLayout
        implements OnPhotoTagsChangedListener, View.OnClickListener,
        OnPhotoTagTapListener, OnFaceDetectionListener,
        PhotoViewAttacher.OnMatrixChangedListener {

    static final String LOG_TAG = "PhotoTagItemLayout";

    private PhotoTag mFriendRequestTag;
    private final MultiTouchImageView mImageView;
    private final CheckableImageView mButton;

    private final LayoutInflater mLayoutInflater;

    private final Animation mPhotoTagInAnimation, mPhotoTagOutAnimation;

    private final AbsoluteLayout mTagLayout;

    private int mPosition;
    private final Photo mUpload;
    private final PhotoController mController;
    private final View mFaceDetectIndicator;

    public PhotoTagItemLayout(Context context, PhotoController controller, Photo upload) {
        super(context);

        mController = controller;

        mLayoutInflater = LayoutInflater.from(context);

        mImageView = new MultiTouchImageView(context);
        mImageView.setOnMatrixChangeListener(this);
        mImageView.setPhotoTapListener(this);
        addView(mImageView, LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);

        mTagLayout = new AbsoluteLayout(context);
        addView(mTagLayout, LayoutParams.MATCH_PARENT,
                LayoutParams.MATCH_PARENT);

        mPhotoTagInAnimation = AnimationUtils.loadAnimation(context, R.anim.tag_fade_in);
        mPhotoTagOutAnimation = AnimationUtils.loadAnimation(context, R.anim.tag_fade_out);

        LayoutInflater inflater = LayoutInflater.from(context);

        mButton = (CheckableImageView) inflater
                .inflate(R.layout.layout_check_button_lrg, this, false);
        mButton.setOnClickListener(this);
        addView(mButton);

        mFaceDetectIndicator = inflater.inflate(R.layout.layout_face_detect, this, false);
        addView(mFaceDetectIndicator);

        if (null != upload) {
            upload.setTagChangedListener(this);
            mButton.setChecked(mController.isSelected(upload));
        }
        mUpload = upload;

        addPhotoTags();
    }

    public MultiTouchImageView getImageView() {
        return mImageView;
    }

    public Photo getPhotoSelection() {
        return mUpload;
    }

    public int getPosition() {
        return mPosition;
    }

    public void setPosition(int position) {
        mPosition = position;
    }

    public void onClick(View v) {
        final PhotoTag tag = (PhotoTag) v.getTag();

        switch (v.getId()) {
            case R.id.btn_remove_tag:
                mUpload.removePhotoTag(tag);
                break;
            case R.id.tv_tag_label:
                mFriendRequestTag = tag;
                break;
            case R.id.iv_large_selection_btn:
                mButton.toggle();
                // Update the controller

                updateController();

                Animation anim = AnimationUtils.loadAnimation(getContext(),
                        mButton.isChecked() ? R.anim.photo_selection_added
                                : R.anim.photo_selection_removed);
                v.startAnimation(anim);

                break;
        }
    }


    public void onMatrixChanged(RectF rect) {
        layoutTags(rect);
    }

    public void onNewPhotoTagTap(PhotoTag newTag) {
        if (Flags.DEBUG) {
            Log.d(LOG_TAG, "onPhotoTap");
        }
        mUpload.addPhotoTag(newTag);
    }

    public void onPhotoTagsChanged(final PhotoTag tag, final boolean added) {
        post(new Runnable() {
            public void run() {
                //longjianlin 屏蔽点击图片
                //onPhotoTagsChangedImp(tag, added);
            }
        });
    }


    private void addPhotoTags() {
        mTagLayout.removeAllViews();

        if (null != mUpload && mUpload.getPhotoTagsCount() > 0) {
            layoutTags(mImageView.getDisplayRect());
        }
    }


    private View getTagLayout(PhotoTag tag) {
        for (int i = 0, z = mTagLayout.getChildCount(); i < z; i++) {
            View tagLayout = mTagLayout.getChildAt(i);
            if (tag == tagLayout.getTag()) {
                return tagLayout;
            }
        }
        return null;
    }

    private void layoutTags(final RectF rect) {
        if (null == rect) {
            return;
        }

        if (Flags.DEBUG) {
            Log.d(LOG_TAG, "layoutTags. Rect: " + rect.toString());
        }

        final Rect viewRect = new Rect();
        getDrawingRect(viewRect);

        for (int i = 0, z = mTagLayout.getChildCount(); i < z; i++) {
            layoutTag(mTagLayout.getChildAt(i), rect, viewRect, false);
        }
    }

    private void layoutTag(final View tagLayout, final RectF rect, final Rect parentRect,
                           final boolean animate) {
        PhotoTag tag = (PhotoTag) tagLayout.getTag();

        int tagWidth = tagLayout.getWidth();
        // Measure View if we need to
        if (tagWidth < 1) {
            measureView(tagLayout);
            tagWidth = tagLayout.getMeasuredWidth();
        }

        AbsoluteLayout.LayoutParams lp = (AbsoluteLayout.LayoutParams) tagLayout.getLayoutParams();
        lp.x = Math.round((rect.width() * tag.getX() / 100f) + rect.left) - (tagWidth / 2);
        lp.y = Math.round((rect.height() * tag.getY() / 100f) + rect.top);
        tagLayout.setLayoutParams(lp);

        if (parentRect.contains(lp.x, lp.y)) {
            if (animate) {
                tagLayout.startAnimation(mPhotoTagInAnimation);
            }
            tagLayout.setVisibility(View.VISIBLE);
        } else {
            tagLayout.setVisibility(View.GONE);
        }
    }

    private void measureView(View child) {
        ViewGroup.LayoutParams p = child.getLayoutParams();
        if (p == null) {
            p = new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        int childWidthSpec = ViewGroup.getChildMeasureSpec(0, 0, p.width);
        int lpHeight = p.height;
        int childHeightSpec;
        if (lpHeight > 0) {
            childHeightSpec = MeasureSpec.makeMeasureSpec(lpHeight, MeasureSpec.EXACTLY);
        } else {
            childHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        }
        child.measure(childWidthSpec, childHeightSpec);
    }

    void updateController() {
        if (mButton.isChecked()) {
            mController.addSelection(mUpload);
        } else {
            mController.removeSelection(mUpload);
        }
    }

    /**
     * More than likely on another thread
     */
    public void onFaceDetectionStarted(Photo selection) {
        mFaceDetectIndicator.post(new Runnable() {
            public void run() {
                Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.fade_in);
                mFaceDetectIndicator.startAnimation(anim);
                mFaceDetectIndicator.setVisibility(View.VISIBLE);
            }
        });
    }

    public void onFaceDetectionFinished(Photo selection) {
        mFaceDetectIndicator.post(new Runnable() {
            public void run() {
                Animation anim = AnimationUtils.loadAnimation(getContext(), R.anim.fade_out);
                mFaceDetectIndicator.startAnimation(anim);
                mFaceDetectIndicator.setVisibility(View.GONE);
            }
        });
    }
}
