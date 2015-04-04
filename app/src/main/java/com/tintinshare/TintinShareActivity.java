package com.tintinshare;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.Window;
import com.tintinshare.base.PhotoFragmentActivity;
import com.tintinshare.events.PhotoSelectionAddedEvent;
import com.tintinshare.events.PhotoSelectionErrorEvent;
import com.tintinshare.events.PhotoSelectionRemovedEvent;
import com.tintinshare.events.UploadsModifiedEvent;
import com.tintinshare.fragments.CircleFragment;
import com.tintinshare.fragments.PhotosFragment;
import com.tintinshare.fragments.SelectedPhotosFragment;
import com.tintinshare.views.PagerSlidingTabStrip;
import com.tintinshare.views.ShareActionBarView;

import de.greenrobot.event.EventBus;


public class TintinShareActivity extends PhotoFragmentActivity implements View.OnClickListener {

    private PagerSlidingTabStrip tabs;
    private ViewPager pager;

    private DisplayMetrics dm;                          //获取当前屏幕的密度
    private TabPagerAdapter mTabAdapter;                    //Pager 数据适配器
    private PhotoApplication mPhotoApplication;
    private PhotoController mPhotoController;

//    private String[] mTitles1 = {getString(R.string.tab_photos),
//            getString(R.string.tab_selected),
//            getString(R.string.tab_circle)};

    private String[] mTitles = {"图 片", "分 享", "圈 子"};

    private Fragment[] fragments = new Fragment[]{
            new PhotosFragment(),
            new SelectedPhotosFragment(),
            new CircleFragment()
    };

    private boolean isShowShare;//是否显示分享Menu

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dm = getResources().getDisplayMetrics();        //获取屏幕分辨率
        setContentView(R.layout.activity_tintin_share);
        tabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        pager = (ViewPager) findViewById(R.id.pager);

        tabs.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                if (i == 1) {
                    isShowShare = true;
                } else {
                    isShowShare = false;
                }
                getWindow().invalidatePanelMenu(Window.FEATURE_OPTIONS_PANEL);
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        mTabAdapter = new TabPagerAdapter(getSupportFragmentManager());
        tabsHandler.sendEmptyMessage(0);
        EventBus.getDefault().register(this);

        mPhotoApplication = PhotoApplication.getApplication(this);
        mPhotoController = mPhotoApplication.getPhotoUploadController();

        getSupportActionBar().setDisplayShowHomeEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);

    }

    //tabs handler
    private Handler tabsHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == 0) {
                pager.setAdapter(mTabAdapter);
                tabs.setViewPager(pager);
                setTabsValue();
            }
        }
    };

    /**
     * 对PagerSlidingTabStrip的各项属性进行赋值。
     */
    private void setTabsValue() {
        // 设置Tab是自动填充满屏幕的
        tabs.setShouldExpand(true);
        // 设置Tab的分割线是透明的
        tabs.setDividerColor(getResources().getColor(R.color.translucent_lght_grey));
        //tabs.setDividerColor(getResources().getColor(android.R.color.transparent));
        // 设置Tab底部线的高度
        tabs.setUnderlineHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 2, dm));
        // 设置Tab Indicator的高度
        tabs.setIndicatorHeight((int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 4, dm));
        // 设置Tab标题文字的大小
        tabs.setTextSize((int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP, 16, dm));
        // 设置Tab Indicator的颜色
        tabs.setIndicatorColor(getResources().getColor(R.color.indicatorcolor));
        // 设置选中Tab文字的颜色 (这是我自定义的一个方法)
        tabs.setSelectedTextColor(getResources().getColor(R.color.selectedtextcolor));
        // 取消点击Tab时的背景色
        tabs.setTabBackground(0);
    }

    @Override
    public void onClick(View v) {
        mPhotoController.sharePhotoEvent();
    }


    /**
     * tab adapter
     */
    private class TabPagerAdapter extends FragmentPagerAdapter {

        public TabPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mTitles[position];
        }

        @Override
        public int getCount() {
            return mTitles.length;
        }

        @Override
        public Fragment getItem(int position) {
            return fragments[position];
        }
    }


    /**
     * 添加图片
     *
     * @param event
     */
    public void onEvent(PhotoSelectionAddedEvent event) {
        refreshShareActionBarView();
    }

    /**
     * 移除图片
     *
     * @param event
     */
    public void onEvent(PhotoSelectionRemovedEvent event) {
        refreshShareActionBarView();
    }


    @Override
    public void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }


    private ShareActionBarView mShareActionView;

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        mShareActionView = null;
        getSupportMenuInflater().inflate(R.menu.tintin_share, menu);
        setShareActionBarView(menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * 菜单逻辑处理
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_share);
        if (isShowShare) {
            item.setVisible(true);
        } else {
            item.setVisible(false);
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setShareActionBarView(Menu menu) {
        MenuItem item = menu.findItem(R.id.action_share);
        mShareActionView = (ShareActionBarView) item.getActionView();
        mShareActionView.setOnClickListener(this);
        refreshShareActionBarView();
    }

    private void refreshShareActionBarView() {
        if (null != mShareActionView) {
            if (mPhotoController.hasSelections()) {
                mShareActionView.animateBackground();
            } else {
                mShareActionView.stopAnimatingBackground();
            }
        }
    }

    public void onEventMainThread(UploadsModifiedEvent event) {
        refreshShareActionBarView();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshShareActionBarView();
    }
}
