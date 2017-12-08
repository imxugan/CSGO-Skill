<?php

function getDataTemplate() {

    $data = array(
        "recently_changed" => false,
        "kills" => 0,
        "deaths" => 0,
        "heads" => 0,
        "time" => 0,
        "wins" => 0,
        "rounds" => 0,
        "match_wins" => 0,
        "matches" => 0,
        "shots" => 0,
        "hits" => 0,
        "damage" => 0,
        "plants" => 0,
        "defuse" => 0,
        "hostage" => 0,
        "contrib" => 0,
        "money" => 0,
        "knife" => 0,
        "nades" => 0,
        "fires" => 0,
        "csnipe" => 0,
        "doms" => 0,
        "revs" => 0,
        "overkill" => 0,
        "pistolwin" => 0,
        "donation" => 0,
        "wins_cbble" => 0,
        "wins_dust2" => 0,
        "wins_infer" => 0,
        "wins_nuke" => 0,
        "wins_train" => 0,
        "rnds_cbble" => 0,
        "rnds_dust2" => 0,
        "rnds_infer" => 0,
        "rnds_nuke" => 0,
        "rnds_train" => 0,
        "g35s" => 0,
        "g35k" => 0
    );

    // Generate weapon entries. See item_ids.json
    $guns = array("0", "2", "3", "4", "5", "6", "8", "10", "11", "12", "14",
                  "15", "16", "17", "18", "19", "20", "21", "22", "23", "24",
                  "25", "26", "27", "28", "29", "30", "31", "32");

    foreach ($guns as $g) {
        $data["g".$g."s"] = 0; $data["g".$g."h"] = 0; $data["g".$g."k"] = 0;
    }

    return $data;

}

?>
