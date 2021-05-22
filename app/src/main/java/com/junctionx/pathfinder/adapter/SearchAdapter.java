package com.junctionx.pathfinder.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.junctionx.pathfinder.R;
import com.junctionx.pathfinder.model.Address;

import java.util.ArrayList;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {
    ArrayList<Address> items = new ArrayList<Address>();
    private Context context;

    OnClickListener listener;

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(viewGroup.getContext());
        View itemView = inflater.inflate(R.layout.item_search, viewGroup, false);

        return new ViewHolder(itemView);
    }

    // 생성자에서 데이터 리스트 객체를 전달받음.
    public SearchAdapter(Context context, ArrayList<Address> list, OnClickListener listener) {
        this.context = context;
        this.items = list;
        this.listener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Address item = items.get(position);
        holder.setItem(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public void addItem(Address item) {
        items.add(item);
    }

    public void setItems(ArrayList<Address> items) {
        this.items = items;
    }

    public Address getItem(int position) {
        return items.get(position);
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textViewTitle;
        TextView textViewAddress;
        String address;

        public ViewHolder(@NonNull final View itemView) {
            super(itemView);

            textViewTitle = itemView.findViewById(R.id.textViewTitle);
            textViewAddress = itemView.findViewById(R.id.textViewAddress);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onItemClick(address);
                }
            });
        }

        public void setItem(final Address item) {
            textViewTitle.setText(item.getTitle());
            textViewAddress.setText(item.getAddress());
            address = item.getAddress();
        }
    }

    public interface OnClickListener {
        void onItemClick(String address);
    }
}
