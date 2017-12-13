<?php

/**
 * This file can be included when another script needs to convert a JSON object
 * of a user's $stats to our own naming convention.
 */

function convertStats($stats) {

    $fixed = json_decode("{}");
    $new = json_decode("{}");
    foreach ($stats as $s) { // pack into list to reduce lookup time
        $fixed->{$s->name} = $s->value;
    }
    $new->{"kills"} = $fixed->total_kills;
    $new->{"deaths"} = $fixed->total_deaths;
    $new->{"heads"} = $fixed->total_kills_headshot;
    $new->{"time"} = $fixed->total_time_played;
    $new->{"wins"} = $fixed->total_wins;
    $new->{"rounds"} = $fixed->total_rounds_played;
    $new->{"match_wins"} = $fixed->total_matches_won;
    $new->{"matches"} = $fixed->total_matches_played;
    $new->{"shots"} = $fixed->total_shots_fired;
    $new->{"hit"} = $fixed->total_shots_hit;
    $new->{"damage"} = $fixed->total_damage_done;
    $new->{"plants"} = $fixed->total_planted_bombs;
    $new->{"defuse"} = $fixed->total_defused_bombs;
    $new->{"hostage"} = $fixed->total_rescued_hostages;
    $new->{"contrib"} = $fixed->total_contribution_score;
    $new->{"mvps"} = $fixed->total_mvps;
    $new->{"money"} = $fixed->total_money_earned;
    $new->{"knife"} = $fixed->total_kills_knife;
    $new->{"nades"} = $fixed->total_kills_hegrenade;
    $new->{"fires"} = $fixed->total_kills_molotov;
    $new->{"csnipe"} = $fixed->total_kills_against_zoomed_sniper;
    $new->{"backfire"} = $fixed->total_kills_enemy_weapon;
    $new->{"doms"} = $fixed->total_dominations;
    $new->{"revs"} = $fixed->total_revenges;
    $new->{"overkill"} = $fixed->total_domination_overkills;
    $new->{"pistolwin"} = $fixed->total_wins_pistolround;
    $new->{"donation"} = $fixed->total_weapons_donated;
    $new->{"wins_cbble"} = $fixed->total_wins_map_de_cbble;
    $new->{"wins_dust2"} = $fixed->total_wins_map_de_dust2;
    $new->{"wins_infer"} = $fixed->total_wins_map_de_inferno;
    $new->{"wins_nuke"} = $fixed->total_wins_map_de_nuke;
    $new->{"wins_train"} = $fixed->total_wins_map_de_train;
    $new->{"rnds_cbble"} = $fixed->total_rounds_map_de_cbble;
    $new->{"rnds_dust2"} = $fixed->total_rounds_map_de_dust2;
    $new->{"rnds_infer"} = $fixed->total_rounds_map_de_inferno;
    $new->{"rnds_nuke"} = $fixed->total_rounds_map_de_nuke;
    $new->{"rnds_train"} = $fixed->total_rounds_map_de_train;
    $new->{"g0s"} = $fixed->total_shots_glock;
    $new->{"g0h"} = $fixed->total_hits_glock;
    $new->{"g0k"} = $fixed->total_kills_glock;
    $new->{"g2s"} = $fixed->total_shots_hkp2000;
    $new->{"g2h"} = $fixed->total_hits_hkp2000;
    $new->{"g2k"} = $fixed->total_kills_hkp2000;
    $new->{"g3s"} = $fixed->total_shots_elite;
    $new->{"g3h"} = $fixed->total_hits_elite;
    $new->{"g3k"} = $fixed->total_kills_elite;
    $new->{"g4s"} = $fixed->total_shots_p250;
    $new->{"g4h"} = $fixed->total_hits_p250;
    $new->{"g4k"} = $fixed->total_kills_p250;
    $new->{"g5s"} = $fixed->total_shots_tec9;
    $new->{"g5h"} = $fixed->total_hits_tec9;
    $new->{"g5k"} = $fixed->total_kills_tec9;
    $new->{"g6s"} = $fixed->total_shots_fiveseven;
    $new->{"g6h"} = $fixed->total_hits_fiveseven;
    $new->{"g6k"} = $fixed->total_kills_fiveseven;
    $new->{"g8s"} = $fixed->total_shots_deagle;
    $new->{"g8h"} = $fixed->total_hits_deagle;
    $new->{"g8k"} = $fixed->total_kills_deagle;
    $new->{"g10s"} = $fixed->total_shots_galilar;
    $new->{"g10h"} = $fixed->total_hits_galilar;
    $new->{"g10k"} = $fixed->total_kills_galilar;
    $new->{"g11s"} = $fixed->total_shots_famas;
    $new->{"g11h"} = $fixed->total_hits_famas;
    $new->{"g11k"} = $fixed->total_kills_famas;
    $new->{"g12s"} = $fixed->total_shots_ak47;
    $new->{"g12h"} = $fixed->total_hits_ak47;
    $new->{"g12k"} = $fixed->total_kills_ak47;
    $new->{"g14s"} = $fixed->total_shots_m4a1;
    $new->{"g14h"} = $fixed->total_hits_m4a1;
    $new->{"g14k"} = $fixed->total_kills_m4a1;
    $new->{"g15s"} = $fixed->total_shots_ssg08;
    $new->{"g15h"} = $fixed->total_hits_ssg08;
    $new->{"g15k"} = $fixed->total_kills_ssg08;
    $new->{"g16s"} = $fixed->total_shots_sg556;
    $new->{"g16h"} = $fixed->total_hits_sg556;
    $new->{"g16k"} = $fixed->total_kills_sg556;
    $new->{"g17s"} = $fixed->total_shots_aug;
    $new->{"g17h"} = $fixed->total_hits_aug;
    $new->{"g17k"} = $fixed->total_kills_aug;
    $new->{"g18s"} = $fixed->total_shots_awp;
    $new->{"g18h"} = $fixed->total_hits_awp;
    $new->{"g18k"} = $fixed->total_kills_awp;
    $new->{"g19s"} = $fixed->total_shots_g3sg1;
    $new->{"g19h"} = $fixed->total_hits_g3sg1;
    $new->{"g19k"} = $fixed->total_kills_g3sg1;
    $new->{"g20s"} = $fixed->total_shots_scar20;
    $new->{"g20h"} = $fixed->total_hits_scar20;
    $new->{"g20k"} = $fixed->total_kills_scar20;
    $new->{"g21s"} = $fixed->total_shots_mac10;
    $new->{"g21h"} = $fixed->total_hits_mac10;
    $new->{"g21k"} = $fixed->total_kills_mac10;
    $new->{"g22s"} = $fixed->total_shots_mp9;
    $new->{"g22h"} = $fixed->total_hits_mp9;
    $new->{"g22k"} = $fixed->total_kills_mp9;
    $new->{"g23s"} = $fixed->total_shots_mp7;
    $new->{"g23h"} = $fixed->total_hits_mp7;
    $new->{"g23k"} = $fixed->total_kills_mp7;
    $new->{"g24s"} = $fixed->total_shots_ump45;
    $new->{"g24h"} = $fixed->total_hits_ump45;
    $new->{"g24k"} = $fixed->total_kills_ump45;
    $new->{"g25s"} = $fixed->total_shots_p90;
    $new->{"g25h"} = $fixed->total_hits_p90;
    $new->{"g25k"} = $fixed->total_kills_p90;
    $new->{"g26s"} = $fixed->total_shots_bizon;
    $new->{"g26h"} = $fixed->total_hits_bizon;
    $new->{"g26k"} = $fixed->total_kills_bizon;
    $new->{"g27s"} = $fixed->total_shots_nova;
    $new->{"g27h"} = $fixed->total_hits_nova;
    $new->{"g27k"} = $fixed->total_kills_nova;
    $new->{"g28s"} = $fixed->total_shots_xm1014;
    $new->{"g28h"} = $fixed->total_hits_xm1014;
    $new->{"g28k"} = $fixed->total_kills_xm1014;
    $new->{"g29s"} = $fixed->total_shots_sawedoff;
    $new->{"g29h"} = $fixed->total_hits_sawedoff;
    $new->{"g29k"} = $fixed->total_kills_sawedoff;
    $new->{"g30s"} = $fixed->total_shots_mag7;
    $new->{"g30h"} = $fixed->total_hits_mag7;
    $new->{"g30k"} = $fixed->total_kills_mag7;
    $new->{"g31s"} = $fixed->total_shots_m249;
    $new->{"g31h"} = $fixed->total_hits_m249;
    $new->{"g31k"} = $fixed->total_kills_m249;
    $new->{"g32s"} = $fixed->total_shots_negev;
    $new->{"g32h"} = $fixed->total_hits_negev;
    $new->{"g32k"} = $fixed->total_kills_negev;
    $new->{"g35s"} = $fixed->total_shots_taser;
    $new->{"g35k"} = $fixed->total_kills_taser;

    return $new;

}

?>
