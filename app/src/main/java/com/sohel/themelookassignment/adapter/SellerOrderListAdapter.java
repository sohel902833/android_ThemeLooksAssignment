package com.sohel.themelookassignment.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.sohel.themelookassignment.R;
import com.sohel.themelookassignment.api.ApiRef;
import com.sohel.themelookassignment.model.ImageModel;
import com.sohel.themelookassignment.model.Order;
import com.sohel.themelookassignment.model.Product;
import com.squareup.picasso.Picasso;

import java.util.List;

public class SellerOrderListAdapter extends RecyclerView.Adapter<SellerOrderListAdapter.MyViewHolder> {

    private Context context;
    private List<Order> orderList;
    private  OnItemClickListner listner;


    public SellerOrderListAdapter(Context context, List<Order> orderList) {
        this.context = context;
        this.orderList = orderList;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view= LayoutInflater.from(parent.getContext()).inflate(R.layout.order_item_layout,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        Order order=orderList.get(position);

        ApiRef.getProductRef().child(order.getProductId())
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if(snapshot.exists()){
                                Product product=snapshot.getValue(Product.class);
                                holder.productNameTv.setText(""+product.getProductName());
                                List<ImageModel> imageList=product.getProductImages();
                                if(imageList!=null){
                                    Picasso.get().load(imageList.get(0).getImageUrl()).placeholder(R.drawable.sari).into(holder.productImageView);
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });
        holder.productPriceTv.setText("Total Amount: "+order.getTotalAmount());
        holder.productQuantityTv.setText("Q: "+order.getTotalProduct());
        holder.orderStatusTv.setText("Status: "+order.getState());
        holder.dateTv.setText(""+order.getDate());
        holder.viewUserDetailsButton.setVisibility(View.VISIBLE);
        if(order.getState().equals(Order.ORDER_STEP_1)){
            holder.actionButton.setText("Accept");
        }else if(order.getState().equals(Order.ORDER_STEP_2)){
            holder.actionButton.setText("Delivered");
        }else{
            holder.actionButton.setVisibility(View.GONE);
        }

        holder.actionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(order.getState().equals(Order.ORDER_STEP_1)){
                    if(listner!=null){
                        listner.onAccept(holder.getAdapterPosition());
                    }

                }else if(order.getState().equals(Order.ORDER_STEP_2)){
                    if(listner!=null){
                        listner.onDelivered(holder.getAdapterPosition());
                    }

                }
            }
        });

        holder.viewUserDetailsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(listner!=null){
                    listner.onViewUserDetails(holder.getAdapterPosition());
                }
            }
        });




    }

    @Override
    public int getItemCount() {
        return orderList.size();
    }

    public class MyViewHolder extends  RecyclerView.ViewHolder implements View.OnClickListener {
        ImageView productImageView;
        TextView productNameTv,productPriceTv,productQuantityTv,orderStatusTv,dateTv;
        Button actionButton,viewUserDetailsButton;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            productImageView=itemView.findViewById(R.id.o_productImageView);
            productNameTv=itemView.findViewById(R.id.o_productNameTV);
            productPriceTv=itemView.findViewById(R.id.o_productPriceTv);
            productQuantityTv=itemView.findViewById(R.id.o_productQuantity);
            orderStatusTv=itemView.findViewById(R.id.o_productStatusTv);
            actionButton=itemView.findViewById(R.id.o_actionButton);
            dateTv=itemView.findViewById(R.id.o_dateTv);
            viewUserDetailsButton=itemView.findViewById(R.id.o_userDetailsButton);

            itemView.setOnClickListener(this);


        }

        @Override
        public void onClick(View v) {
            if(listner!=null){
                int position=getAdapterPosition();
                if(position!= RecyclerView.NO_POSITION){
                    listner.onItemClick(position);
                }
            }
        }
    }
    public interface  OnItemClickListner{
        void onItemClick(int position);
        void onAccept(int position);
        void onDelivered(int position);
        void onViewUserDetails(int position);
    }

    public void setOnItemClickListener(OnItemClickListner listner){
        this.listner=listner;
    }
}