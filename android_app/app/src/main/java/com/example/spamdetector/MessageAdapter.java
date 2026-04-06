package com.example.spamdetector;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.ViewHolder> {
    private List<MessageModel> messageList;

    public MessageAdapter(List<MessageModel> messageList) {
        this.messageList = messageList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        MessageModel model = messageList.get(position);
        holder.textMessage.setText(model.getMessage());
        holder.textTime.setText(model.getTime());
        holder.textConfidence.setText("Confidence: " + model.getConfidence());
        
        holder.textLabel.setText(model.getLabel().toUpperCase());
        if (model.getLabel().equalsIgnoreCase("Spam")) {
            holder.textLabel.setBackgroundResource(R.drawable.bg_label_spam);
        } else {
            holder.textLabel.setBackgroundResource(R.drawable.bg_label_safe);
        }
    }

    @Override
    public int getItemCount() { return messageList.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textLabel, textTime, textMessage, textConfidence;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textLabel = itemView.findViewById(R.id.text_label);
            textTime = itemView.findViewById(R.id.text_time);
            textMessage = itemView.findViewById(R.id.text_message);
            textConfidence = itemView.findViewById(R.id.text_confidence);
        }
    }
}
