package com.example.spamdetector;

import android.transition.AutoTransition;
import android.transition.TransitionManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public interface FeedbackListener {
        void onMarkSpam(MessageModel model);
        void onMarkSafe(MessageModel model);
    }

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_MESSAGE = 1;

    private final List<MessageModel> messageList;
    private final FeedbackListener feedbackListener;
    private final Set<String> expandedItems = new HashSet<>();
    private final List<Object> displayItems = new ArrayList<>();

    public MessageAdapter(List<MessageModel> messageList, FeedbackListener feedbackListener) {
        this.messageList = messageList;
        this.feedbackListener = feedbackListener;
        rebuildDisplayItems();
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position) instanceof SectionHeader ? TYPE_HEADER : TYPE_MESSAGE;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_HEADER) {
            View view = inflater.inflate(R.layout.item_timeline_header, parent, false);
            return new HeaderViewHolder(view);
        }
        View view = inflater.inflate(R.layout.item_message, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Object item = displayItems.get(position);
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).title.setText(((SectionHeader) item).title);
            return;
        }

        ViewHolder messageHolder = (ViewHolder) holder;
        MessageModel model = (MessageModel) item;
        String displayLabel = model.getDisplayLabel();
        boolean isSpam = displayLabel.equalsIgnoreCase("Spam");
        boolean isReview = displayLabel.equalsIgnoreCase("Needs Review");
        int confidence = parseConfidence(model.getConfidence());
        String itemKey = buildItemKey(model);

        messageHolder.textSender.setText(model.getSender());
        messageHolder.textTime.setText(model.getTime());
        messageHolder.textMessage.setText(model.getMessage());
        messageHolder.textConfidence.setText(String.format(Locale.getDefault(), "ML confidence %s", model.getConfidence()));
        messageHolder.textCategory.setText(model.getCategory());
        messageHolder.textReasons.setText(model.getReasons());
        messageHolder.textRiskFlag.setVisibility(model.hasLink() ? View.VISIBLE : View.GONE);
        messageHolder.textRiskFlag.setText(model.hasLink() ? "LINK" : "");
        messageHolder.textLearnedBadge.setVisibility(model.isLearned() ? View.VISIBLE : View.GONE);

        messageHolder.textLabel.setText(displayLabel.toUpperCase(Locale.getDefault()));
        if (isSpam) {
            messageHolder.textLabel.setBackgroundResource(R.drawable.bg_label_spam);
            messageHolder.timelineDot.setBackgroundResource(R.drawable.bg_timeline_dot_spam);
            messageHolder.messageCard.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.spam_red));
            messageHolder.confidenceBar.setIndicatorColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.spam_red));
        } else if (isReview) {
            messageHolder.textLabel.setBackgroundResource(R.drawable.bg_label_review);
            messageHolder.timelineDot.setBackgroundResource(R.drawable.bg_timeline_dot_review);
            messageHolder.messageCard.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.review_amber));
            messageHolder.confidenceBar.setIndicatorColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.review_amber));
        } else {
            messageHolder.textLabel.setBackgroundResource(R.drawable.bg_label_safe);
            messageHolder.timelineDot.setBackgroundResource(R.drawable.bg_timeline_dot_safe);
            messageHolder.messageCard.setStrokeColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.safe_green));
            messageHolder.confidenceBar.setIndicatorColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.safe_green));
        }
        messageHolder.confidenceBar.setProgressCompat(confidence, false);

        boolean expanded = expandedItems.contains(itemKey);
        applyExpandedState(messageHolder, expanded);

        messageHolder.messageCard.setOnClickListener(v -> {
            boolean nextExpanded = !expandedItems.contains(itemKey);
            if (nextExpanded) {
                expandedItems.add(itemKey);
            } else {
                expandedItems.remove(itemKey);
            }
            TransitionManager.beginDelayedTransition(messageHolder.messageCard, new AutoTransition());
            applyExpandedState(messageHolder, nextExpanded);
        });

        messageHolder.markSpamButton.setEnabled(!isSpam);
        messageHolder.markSafeButton.setEnabled(!displayLabel.equalsIgnoreCase("Safe"));
        messageHolder.markSpamButton.setAlpha(isSpam ? 0.55f : 1f);
        messageHolder.markSafeButton.setAlpha(displayLabel.equalsIgnoreCase("Safe") ? 0.55f : 1f);

        messageHolder.markSpamButton.setOnClickListener(v -> feedbackListener.onMarkSpam(model));
        messageHolder.markSafeButton.setOnClickListener(v -> feedbackListener.onMarkSafe(model));
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    public void refreshTimeline() {
        rebuildDisplayItems();
        notifyDataSetChanged();
    }

    public MessageModel getMessageAtAdapterPosition(int position) {
        if (position < 0 || position >= displayItems.size()) {
            return null;
        }
        Object item = displayItems.get(position);
        return item instanceof MessageModel ? (MessageModel) item : null;
    }

    private void rebuildDisplayItems() {
        displayItems.clear();
        String lastSection = null;
        for (MessageModel model : messageList) {
            String section = resolveSectionTitle(model.getTime());
            if (!section.equals(lastSection)) {
                displayItems.add(new SectionHeader(section));
                lastSection = section;
            }
            displayItems.add(model);
        }
    }

    private String resolveSectionTitle(String rawTime) {
        try {
            Date parsed = DateFormat.getDateTimeInstance().parse(rawTime);
            if (parsed == null) {
                return "Earlier";
            }
            Calendar messageCalendar = Calendar.getInstance();
            messageCalendar.setTime(parsed);

            Calendar today = Calendar.getInstance();
            if (isSameDay(messageCalendar, today)) {
                return "Today";
            }

            today.add(Calendar.DAY_OF_YEAR, -1);
            if (isSameDay(messageCalendar, today)) {
                return "Yesterday";
            }
            return "Earlier";
        } catch (Exception ignored) {
            return "Earlier";
        }
    }

    private boolean isSameDay(Calendar left, Calendar right) {
        return left.get(Calendar.YEAR) == right.get(Calendar.YEAR)
                && left.get(Calendar.DAY_OF_YEAR) == right.get(Calendar.DAY_OF_YEAR);
    }

    private void applyExpandedState(ViewHolder holder, boolean expanded) {
        holder.detailsContainer.setVisibility(expanded ? View.VISIBLE : View.GONE);
        holder.textMessage.setMaxLines(expanded ? Integer.MAX_VALUE : 3);
    }

    private int parseConfidence(String rawConfidence) {
        if (rawConfidence == null) {
            return 0;
        }
        try {
            String cleaned = rawConfidence.replaceAll("[^0-9]", "");
            if (cleaned.isEmpty()) {
                return 0;
            }
            int value = Integer.parseInt(cleaned);
            return Math.max(0, Math.min(100, value));
        } catch (Exception ignored) {
            return 0;
        }
    }

    private String buildItemKey(MessageModel model) {
        return model.getSender() + "|" + model.getTime() + "|" + model.getMessage().hashCode();
    }

    private static class SectionHeader {
        final String title;

        SectionHeader(String title) {
            this.title = title;
        }
    }

    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        final TextView title;

        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_section_title);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        final MaterialCardView messageCard;
        final View timelineDot;
        final TextView textLabel;
        final TextView textTime;
        final TextView textMessage;
        final TextView textConfidence;
        final TextView textSender;
        final TextView textCategory;
        final TextView textReasons;
        final TextView textRiskFlag;
        final TextView textLearnedBadge;
        final LinearProgressIndicator confidenceBar;
        final View detailsContainer;
        final Button markSpamButton;
        final Button markSafeButton;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            messageCard = itemView.findViewById(R.id.message_card);
            timelineDot = itemView.findViewById(R.id.timeline_dot);
            textLabel = itemView.findViewById(R.id.text_label);
            textTime = itemView.findViewById(R.id.text_time);
            textMessage = itemView.findViewById(R.id.text_message);
            textConfidence = itemView.findViewById(R.id.text_confidence);
            textSender = itemView.findViewById(R.id.text_sender);
            textCategory = itemView.findViewById(R.id.text_category);
            textReasons = itemView.findViewById(R.id.text_reasons);
            textRiskFlag = itemView.findViewById(R.id.text_risk_flag);
            textLearnedBadge = itemView.findViewById(R.id.text_learned_badge);
            confidenceBar = itemView.findViewById(R.id.confidence_bar);
            detailsContainer = itemView.findViewById(R.id.details_container);
            markSpamButton = itemView.findViewById(R.id.button_mark_spam);
            markSafeButton = itemView.findViewById(R.id.button_mark_safe);
        }
    }
}
