package com.example.waterpoloapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.waterpoloapp.R;
import com.example.waterpoloapp.TeamDetailActivity;
import com.example.waterpoloapp.TeamListActivity;
import com.example.waterpoloapp.model.Team;

import java.util.List;

public class TeamAdapter extends RecyclerView.Adapter<TeamAdapter.TeamViewHolder> {

    private Context context;
    private List<Team> teams;
    private boolean isAdmin;

    public TeamAdapter(Context context, List<Team> teams, boolean isAdmin) {
        this.context = context;
        this.teams = teams;
        this.isAdmin = isAdmin;
    }

    @NonNull
    @Override
    public TeamViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_team, parent, false);
        return new TeamViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TeamViewHolder holder, int position) {
        Team team = teams.get(position);

        holder.teamNameTextView.setText(team.getName());

        // Logo betöltése Glide-dal
        if (team.getLogoUrl() != null && !team.getLogoUrl().isEmpty()) {
            Glide.with(context)
                    .load(team.getLogoUrl())
                    .placeholder(R.drawable.logo_placeholder)
                    .error(R.drawable.logo_placeholder)
                    .into(holder.teamLogoImageView);
        } else {
            holder.teamLogoImageView.setImageResource(R.drawable.logo_placeholder);
        }

        // Kártya animáció
        Animation animation = AnimationUtils.loadAnimation(context, R.anim.slide_in_right);
        holder.cardView.startAnimation(animation);

        // Kattintás esemény
        holder.cardView.setOnClickListener(v -> {
            Intent intent = new Intent(context, TeamDetailActivity.class);
            intent.putExtra("teamId", team.getId());
            intent.putExtra("isAdmin", isAdmin);
            context.startActivity(intent);

            // Activity átmenet animáció
            if (context instanceof TeamListActivity) {
                ((TeamListActivity) context).overridePendingTransition(
                        R.anim.slide_in_right, R.anim.slide_out_left);
            }
        });
    }

    @Override
    public int getItemCount() {
        return teams.size();
    }

    public void updateTeams(List<Team> teams) {
        this.teams = teams;
        notifyDataSetChanged();
    }

    public static class TeamViewHolder extends RecyclerView.ViewHolder {
        TextView teamNameTextView;
        ImageView teamLogoImageView;
        CardView cardView;

        public TeamViewHolder(@NonNull View itemView) {
            super(itemView);
            teamNameTextView = itemView.findViewById(R.id.teamNameTextView);
            teamLogoImageView = itemView.findViewById(R.id.teamLogoImageView);
            cardView = itemView.findViewById(R.id.cardView);
        }
    }
}
