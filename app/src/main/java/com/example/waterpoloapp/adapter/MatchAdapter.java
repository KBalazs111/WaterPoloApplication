package com.example.waterpoloapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.example.waterpoloapp.MainActivity;
import com.example.waterpoloapp.MatchDetailActivity;
import com.example.waterpoloapp.R;
import com.example.waterpoloapp.model.Match;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.MatchViewHolder> {

    private Context context;
    private List<Match> matches;
    private boolean isAdmin;
    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd. HH:mm", Locale.getDefault());

    public MatchAdapter(Context context, List<Match> matches, boolean isAdmin) {
        this.context = context;
        this.matches = matches;
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public MatchViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_match, parent, false);
        return new MatchViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchViewHolder holder, int position) {
        Match match = matches.get(position);

        holder.team1TextView.setText(match.getTeam1Name());
        holder.team2TextView.setText(match.getTeam2Name());

        if (match.getMatchDate() != null) {
            holder.dateTextView.setText(dateFormat.format(match.getMatchDate()));
        } else {
            holder.dateTextView.setText(R.string.loading);
        }

        // Kártya animáció
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
        holder.cardView.startAnimation(animation);

        // Kattintás esemény
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, MatchDetailActivity.class);
            intent.putExtra("matchId", match.getId());
            intent.putExtra("isAdmin", isAdmin);
            context.startActivity(intent);

            // Activity átmenet animáció
            if (context instanceof MainActivity) {
                ((MainActivity) context).overridePendingTransition(
                        R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
    }

    @Override
    public int getItemCount() {
        return matches.size();
    }

    public void updateMatches(List<Match> matches) {
        this.matches = matches;
        notifyDataSetChanged();
    }

    public static class MatchViewHolder extends RecyclerView.ViewHolder {
        TextView team1TextView, team2TextView, dateTextView;
        CardView cardView;

        public MatchViewHolder(@NonNull View itemView) {
            super(itemView);
            team1TextView = itemView.findViewById(R.id.team1TextView);
            team2TextView = itemView.findViewById(R.id.team2TextView);
            dateTextView = itemView.findViewById(R.id.dateTextView);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }
}