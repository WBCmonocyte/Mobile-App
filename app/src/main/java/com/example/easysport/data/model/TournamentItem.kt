package com.example.easysport.data.model

data class TournamentItem(
    val id: String,
    val title: String,
    val courtId: String,
    val courtName: String,
    val address: String,
    val startsAt: String,
    val endsAt: String,
    val maxTeams: Int,
    val isDemo: Boolean,
)
