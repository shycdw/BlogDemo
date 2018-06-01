package com.example.davidchen.blogdemo.adapter;

import android.content.Context;
import android.view.View;
import android.widget.TextView;


import com.example.davidchen.blogdemo.R;
import com.example.davidchen.blogdemo.bean.Inventory;

import java.util.List;

/**
 * 清单列表adapter
 * <p>
 * Created by DavidChen on 2018/5/30.
 */

public class InventoryAdapter extends BaseRecyclerViewAdapter<Inventory> {

    private OnDeleteClickLister mDeleteClickListener;

    public InventoryAdapter(Context context, List<Inventory> data) {
        super(context, data, R.layout.item_inventroy);
    }

    @Override
    protected void onBindData(RecyclerViewHolder holder, Inventory bean, int position) {
        View view = holder.getView(R.id.tv_delete);
        view.setTag(position);
        if (!view.hasOnClickListeners()) {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mDeleteClickListener != null) {
                        mDeleteClickListener.onDeleteClick(v, (Integer) v.getTag());
                    }
                }
            });
        }
        ((TextView) holder.getView(R.id.tv_item_desc)).setText(bean.getItemDesc());
        String quantity = bean.getQuantity() + "箱";
        ((TextView) holder.getView(R.id.tv_quantity)).setText(quantity);
        String detail = bean.getItemCode() + "/" + bean.getDate();
        ((TextView) holder.getView(R.id.tv_detail)).setText(detail);
        String volume = bean.getVolume() + "方";
        ((TextView) holder.getView(R.id.tv_volume)).setText(volume);
    }

    public void setOnDeleteClickListener(OnDeleteClickLister listener) {
        this.mDeleteClickListener = listener;
    }

    public interface OnDeleteClickLister {
        void onDeleteClick(View view, int position);
    }
}
