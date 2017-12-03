<?php

function getMainTasks() {
    $tasks = array(
        "getting_started" => array(
            "read_me" => false,
            "reqs" => array(
                "time" => 0,
                "rounds" => 0,
                "kills" => 0
            ),
            "play_deathmatch" => false,
            "play_casual" => false,
            "play_comp" => false,
            "take_a_break" => false,
            "completed" => false
        ),
        "practice_time" => array(
            "complete_daily_task" => false,
            "play_custom" => false,
            "download_map" => false,
            "reqs" => array(
                "g0k" => 0,
                "g2k" => 0,
                "g3k" => 0,
                "g4k" => 0,
                "g5k" => 0,
                "g6k" => 0,
                "g8k" => 0
            ),
            "practice_pistol" => false,
            "completed_chapter" => false,
            "completed" => false
        ),
        "training" => array(
            "learn_guns" => false,
            "console" => false,
            "spraying" => false,
            "reqs" => array(
                "g18k" => 0
            ),
            "awp_training" => false,
            "completed_chapter" => false,
            "completed" => false
        ),
        "game_strategy" => array(
            "taking_point" => false,
            "tactical_smokes" => false,
            "communication" => false,
            "reqs" => array(
                "planted" => 0,
                "defused" => 0,
                "kills" => 0,
                "money" => 0
            ),
            "play_comp" => false,
            "completed" => false
        ),
        "completed" => false
    );

    return $tasks;

}

?>
