package com.example.bluetooth;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    // Константи за да знаеме чија е пораката
    private static final int VIEW_TYPE_ME = 1;
    private static final int VIEW_TYPE_OTHER = 2;

    private List<Message> messageList = new ArrayList<>();

    // Додај нова порака во листата
    public void addMessage(Message message) {
        messageList.add(message);
        notifyItemInserted(messageList.size() - 1);
    }

    // Оваа функција одлучува кој дизајн да се користи (Син или Сив)
    @Override
    public int getItemViewType(int position) {
        if (messageList.get(position).isFromMe()) {
            return VIEW_TYPE_ME;
        } else {
            return VIEW_TYPE_OTHER;
        }
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_ME) {
            // Вчитај го СИНИОТ дизајн (лево)
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_me, parent, false);
            return new MessageViewHolder(view);
        } else {
            // Вчитај го СИВИОТ дизајн (десно)
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_message_other, parent, false);
            return new MessageViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message message = messageList.get(position);
        holder.bind(message);
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    // Класа што го држи текстот
    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvContent;

        MessageViewHolder(View itemView) {
            super(itemView);
            tvContent = itemView.findViewById(R.id.tvMessageContent);
        }

        void bind(Message message) {
            tvContent.setText(message.getContent());
        }
    }
}