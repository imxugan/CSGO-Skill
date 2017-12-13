<?php

require_once("setup.php");

/**
 * Include this to check if a Steam Profile meets the minimum requirements
 * to be an account on our system.
 *
 * $throwError === FALSE
 *   Return NULL for invisble account or no CSGO stats
 *   Return FALSE for visible but not suitable account
 *   Return $stats for suitable account
 *
 * $throwError === TRUE
 *   Throw error and stop execution if not suitable account
 *   Return TRUE for suitable account
 */

 /**** REQUIREMENTS ****
  * A public profile
  * Owns CS: GO
  * Has 5 hours played (18000 seconds), OR both of the following
  * Scored 200 kills
  * Has the achievement Newb World Order (Win 10 rounds) [WIN_ROUNDS_LOW]
  */

function check($steamID, $throwError = false, $getStats = false) {
    $url = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/" .
    "v0002/?key=" . STEAMKEY . "&steamids=" . $steamID;
    $json= json_decode(file_get_contents($url));

    if (!isset($json->response->players[0]->communityvisibilitystate)) {
        // Non-existent profile
        if (!$throwError) { return NULL; }
        consoleExit("{\"success\":false,\"error\":\"1320\"}");
    }

    $player = $json->response->players[0];

    if ($steamID !== $player->steamid) {
        // Provided steamID is not properly sanitized or contains extra (attack) data, stop now
        error_log("1323 - Possible SQL injection attack detected. \$steamID = \"" . $steamID . "\"");
        if (!$throwError) { return NULL; }
        consoleExit("{\"success\":false,\"error\":\"1323\"}");
    }

    if ($player->communityvisibilitystate !== 3) {
        // Profile is not visible
        if (!$throwError) { return NULL; }
        consoleExit("{\"success\":false,\"error\":\"1321\"}");
    }

    $json = downloadStats($steamID);

    if (!isset($json->playerstats)) {
        // Profile does not have CS: GO
        if (!$throwError) { return NULL; }
        consoleExit("{\"success\":false,\"error\":\"13221\"}");
    }

    require_once("convertSteamAPI.php");

    $stats = convertStats($json->playerstats->stats);
    if ($getStats) { return $stats; }
    $achvs = $json->playerstats->achievements;
    $goodPlaytime = false;
    $goodKills = false;
    $goodAchv = false;

    if ($stats->time >= 18000) {
        $goodPlaytime = true;
    }

    if ($stats->kills >= 200) {
        $goodKills = true;
    }

    foreach ($achvs as $a) {
        if ($a->name === "WIN_ROUNDS_LOW") {
            if ($a->achieved === 1) {
                $goodAchv = true;
                break;
            }
        }
    } unset($a);

    if ( !($goodPlaytime || ($goodAchv && $goodKills)) ) {
        // Profile does not meet extra requirements
        if (!$throwError) { return false; }
        consoleExit("{\"success\":false,\"error\":\"13222\"}");
    }

    if (!$throwError) { return $stats; }
    return true;
}
