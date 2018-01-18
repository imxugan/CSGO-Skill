<?php

// Our server time is based in Denver, screw UTC!
date_default_timezone_set("America/Denver");

require_once("vars.php");

// Javascript Console logging
// Prints the provided $output to the console and exits immediately.
function consoleExit($output){
    ?>
<!DOCTYPE html>
<html><body><script>console.log('FLARE-ESPORTS:<?=$output?>');</script><span id="out"><?=$output?></span></body></html>
    <?php
    exit();
}

// Convert JSON objects to a string for SQL statments
function jsonToSql($object) {
    // This is the best I got, and seems secure enough.
    return json_encode(json_encode($object));
}

// Download CSGO stats for steamID
function downloadStats($steamID) {
    return json_decode(file_get_contents(
        "http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/" .
        "v2/?appid=730&key=" . STEAMKEY . "&steamid=" . $steamID
    ));
}

// It's a little lonely here :(

?>
