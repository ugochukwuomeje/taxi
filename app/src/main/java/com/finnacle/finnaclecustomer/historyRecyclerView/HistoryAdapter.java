package com.finnacle.finnaclecustomer.historyRecyclerView;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import com.finnacle.finnaclecustomer.R;

import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryViewHolder> {
    private List<HistoryObject> itemList;
    private Context context;

    public HistoryAdapter(List<HistoryObject> itemList, Context context){

        this.itemList = itemList;
        this.context = context;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int i) {
        View layoutView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history, null, false);
        RecyclerView.LayoutParams lp = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutView.setLayoutParams(lp);
        HistoryViewHolder rcv = new HistoryViewHolder(layoutView);
        return rcv;
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder historyViewHolder, int i) {
        historyViewHolder.rideId.setText(itemList.get(i).getRideId());
        historyViewHolder.time.setText(itemList.get(i).getTime());

    }

    @Override
    public int getItemCount() {
        return 0;
    }
}
