package ruolan.renren.com.sample;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import ruolan.renren.com.wrecyclerview.adapter.LRecyclerViewAdapter;
import ruolan.renren.com.wrecyclerview.base.BaseViewHolder;
import ruolan.renren.com.wrecyclerview.base.SimpleAdapter;
import ruolan.renren.com.wrecyclerview.interfaces.OnItemClickListener;
import ruolan.renren.com.wrecyclerview.interfaces.OnItemLongClickListener;
import ruolan.renren.com.wrecyclerview.interfaces.OnLoadMoreListener;
import ruolan.renren.com.wrecyclerview.interfaces.OnRefreshListener;
import ruolan.renren.com.wrecyclerview.utils.RecyclerViewStateUtils;
import ruolan.renren.com.wrecyclerview.view.LRecyclerView;
import ruolan.renren.com.wrecyclerview.view.LoadingFooter;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "lzx";

    /**
     * 服务器端一共多少条数据
     */
    private static final int TOTAL_COUNTER = 64;

    /**
     * 每一页展示多少条数据
     */
    private static final int REQUEST_COUNT = 10;

    /**
     * 已经获取到多少条数据了
     */
    private static int mCurrentCounter = 0;
    private static ArrayList<ItemModel> mItems;

    private LRecyclerView mRecyclerView = null;

    private DataAdapter mDataAdapter = null;

    private PreviewHandler mHandler = new PreviewHandler(this);
    private LRecyclerViewAdapter mLRecyclerViewAdapter = null;

    private boolean isRefresh = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRecyclerView = (LRecyclerView) findViewById(R.id.list);


        mDataAdapter = new DataAdapter(this, mItems);
        /**
         * 子view的点击事件，这个需要在adapter里面进行设置，设置步奏和item的点击事件一样
         * <p>
         *     1、写接口
         *     2、写变量
         *     3、设置
         * </p>
         */
        mDataAdapter.setOnItemChildClick(new DataAdapter.OnItemChildClick() {
            @Override
            public void OnItemChildClickListener(View view, ItemModel itemModel) {
                Toast.makeText(MainActivity.this, "子view的点击事件:" + itemModel, Toast.LENGTH_SHORT).show();
            }
        });

        mLRecyclerViewAdapter = new LRecyclerViewAdapter(mDataAdapter);
        mRecyclerView.setAdapter(mLRecyclerViewAdapter);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //设置图片(下拉刷新的时候出现的箭头)
        mRecyclerView.setArrowImageView(R.drawable.ic_pulltorefresh_arrow);

        //根据自己需要可以插入headerView
        // mLRecyclerViewAdapter.addHeaderView(new SampleHeader(this));

        //下拉刷新的时候调用
        mRecyclerView.setOnRefreshListener(new OnRefreshListener() {
            @Override
            public void onRefresh() {
                RecyclerViewStateUtils.setFooterViewState(mRecyclerView, LoadingFooter.State.Normal);
                mDataAdapter.clear();  //清除数据
                mLRecyclerViewAdapter.notifyDataSetChanged();//fix bug:crapped or attached views may not be recycled. isScrap:false isAttached:true
                mCurrentCounter = 0;  //当前页
                isRefresh = true;  //刷新标识
                requestData();  //请求数据
            }
        });

        //上拉加载更多的时候调用
        mRecyclerView.setOnLoadMoreListener(new OnLoadMoreListener() {
            @Override
            public void onLoadMore() {
                LoadingFooter.State state = RecyclerViewStateUtils.getFooterViewState(mRecyclerView);
                if (state == LoadingFooter.State.Loading) {
                    Log.d(TAG, "the state is Loading, just wait..");
                    return;
                }

                if (mCurrentCounter < TOTAL_COUNTER) {
                    // loading more
                    RecyclerViewStateUtils.setFooterViewState(MainActivity.this, mRecyclerView, REQUEST_COUNT, LoadingFooter.State.Loading, null);
                    requestData();
                } else {
                    //the end
                    RecyclerViewStateUtils.setFooterViewState(MainActivity.this, mRecyclerView, REQUEST_COUNT, LoadingFooter.State.TheEnd, null);

                }
            }
        });


        mRecyclerView.setRefreshing(true);

        //item的点击事件    也就是说item的点击事件要通过mLRecyclerViewAdapter设置，
        // 不是通过自己写的adapter设置   对于item里面的子view的点击事件，可以通过自己的adapter进行设置
        mLRecyclerViewAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                ItemModel item = mDataAdapter.getDatas().get(position);
                Toast.makeText(MainActivity.this, "item:" + item, Toast.LENGTH_SHORT).show();
                //AppToast.showShortText(MainActivity.this, item.title);
            }

        });

        //itme的长按点击事件
        mLRecyclerViewAdapter.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View view, int position) {
                ItemModel item = mDataAdapter.getDatas().get(position);
                Toast.makeText(MainActivity.this, "onItemLongClick -  item:" + item, Toast.LENGTH_SHORT).show();
                // AppToast.showShortText(MainActivity.this, "onItemLongClick - " + item.title);
            }
        });

    }

    private void notifyDataSetChanged() {
        mLRecyclerViewAdapter.notifyDataSetChanged();
    }

    private void addItems(ArrayList<ItemModel> list) {

        mDataAdapter.addAll(list);
        mDataAdapter.notifyDataSetChanged();
        mCurrentCounter += list.size();

    }

    private static class PreviewHandler extends Handler {

        private WeakReference<MainActivity> ref;

        PreviewHandler(MainActivity activity) {
            ref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(Message msg) {
            final MainActivity activity = ref.get();
            if (activity == null || activity.isFinishing()) {
                return;
            }
            switch (msg.what) {

                case -1:
                    if (activity.isRefresh) {
                        activity.mDataAdapter.clear();
                        mCurrentCounter = 0;
                    }

                    int currentSize = activity.mDataAdapter.getItemCount();

                    //模拟组装10个数据
                    mItems = new ArrayList<>();
                    for (int i = 0; i < 10; i++) {
                        if (mItems.size() + currentSize >= TOTAL_COUNTER) {
                            break;
                        }

                        ItemModel item = new ItemModel();
                        item.id = currentSize + i;
                        item.title = "item" + (item.id);

                        mItems.add(item);
                    }

                    activity.addItems(mItems);

                    if (activity.isRefresh) {
                        activity.isRefresh = false;
                        activity.mRecyclerView.refreshComplete();
                    }

                    RecyclerViewStateUtils.setFooterViewState(activity.mRecyclerView,
                            LoadingFooter.State.Normal);
                    activity.notifyDataSetChanged();
                    break;
                case -2:
                    activity.notifyDataSetChanged();
                    break;
                case -3:
                    if (activity.isRefresh) {
                        activity.isRefresh = false;
                        activity.mRecyclerView.refreshComplete();
                    }
                    activity.notifyDataSetChanged();
                    RecyclerViewStateUtils.setFooterViewState(activity,
                            activity.mRecyclerView, REQUEST_COUNT,
                            LoadingFooter.State.NetWorkError, activity.mFooterClick);
                    break;
                default:
                    break;
            }
        }
    }

    private View.OnClickListener mFooterClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            RecyclerViewStateUtils.setFooterViewState(MainActivity.this,
                    mRecyclerView, REQUEST_COUNT,
                    LoadingFooter.State.Loading, null);
            requestData();
        }
    };

    /**
     * 模拟请求网络
     */
    private void requestData() {
        Log.d(TAG, "requestData");
        new Thread() {

            @Override
            public void run() {
                super.run();

                try {
                    Thread.sleep(800);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                mHandler.sendEmptyMessage(-1);
            }
        }.start();
    }

    private static class DataAdapter extends SimpleAdapter<ItemModel> {


        public DataAdapter(Context context, List<ItemModel> datas) {
            super(context, R.layout.list_item_text, datas);
        }

        @Override
        protected void convert(BaseViewHolder viewHoder, final ItemModel item) {
            viewHoder.getTextView(R.id.info_text).setText(item.title);
            viewHoder.getTextView(R.id.btn)
                    .setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (mOnItemChildClick != null) {
                                mOnItemChildClick.OnItemChildClickListener(view, item);
                            }
                        }
                    });
        }

        /**item的子控件的点击事件，一般用在评论、点赞、转发等等。。。。。。*/
        //--------------------------如果想要对item里面的某个控件进行
        //--------------------------设置点击事件，那么就参考下面的写
        //--------------------------法，然后在上面通过getView(id)获取
        //--------------------------然后去设置
        private OnItemChildClick mOnItemChildClick;

        public void setOnItemChildClick(OnItemChildClick onItemChildClick) {
            mOnItemChildClick = onItemChildClick;
        }

        public interface OnItemChildClick {
            void OnItemChildClickListener(View view, ItemModel itemModel);
        }
    }

}
