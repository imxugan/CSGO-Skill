<?php

/**
 * This file must be included at the time when a new account should be built
 * and only takes two external variables: $steamID, $mode [OPTIONAL]
 *
 * If $mode === "create", we also expect to have an $email variable.
 * Otherwise, we simply check that the account COULD be created.
 */

/**** REQUIREMENTS ****
 * A public profile
 * Owns CS: GO
 * Has 10 hours played (36000 seconds)
 * Scored 200 kills
 * Has the achievement Newb World Order (Win 10 rounds) [WIN_ROUNDS_LOW]
 */

if (!isset($steamID)) {
    error_log("Missing \$steamID for included file \"makeAccount\"!");
    die("Missing steamID!");
}

if (!isset($mode)) { $mode = "check"; }

$url = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?key=" . $STEAMKEY . "&steamids=" . $steamID;
$json= json_decode(file_get_contents($url));

if (!isset($json->repsonse->players[0]->communityvisibilitystate)) {
    // Non-existent profile
    consoleExit("{\"success\":false,\"error\":\"1320\"}");
}

$player = $json->response->players[0];

if ($steamID !== $player->steamid) {
    // Provided steamID is not properly sanitized or contains extra (attack) data, stop now
    error_log("1323 - Possible SQL injection attack detected. \$steamID = \"" . $steamID . "\"");
    consoleExit("{\"success\":false,\"error\":\"1323\"}");
}

if ($player->communityvisibilitystate !== 3) {
    // Profile is not visible
    consoleExit("{\"success\":false,\"error\":\"1321\"}");
}

$url = "http://api.steampowered.com/ISteamUserStats/GetUserStatsForGame/v2/?appid=730&key=" . $STEAMKEY . "&steamids=" . $steamID;
$json = json_decode(file_get_contents($url));

if (!isset($json->playerstats)) {
    // Profile does not have CS: GO
    consoleExit("{\"success\":false,\"error\":\"13221\"}");
}

$stats = $json->playerstats->stats;
$achvs = $json->playerstats->achievements;
$goodPlaytime = false;
$goodKills = false;
$goodAchv = false;
$chkPlaytime = false;
$chkKills = false;

foreach ($stats as $e) {
    if ($e->name === "total_time_played") {
        if ($e->value > 36000) {
            $goodPlaytime = true;
        }
        $chkPlaytime = true;
    } else if ($e->name === "total_kills") {
        if ($e->value > 200) {
            $goodKills = true;
        }
        $chkKills = true;
    }
    if ($chkPlaytime && $chkKills) { break; }
} unset($e);

foreach ($achvs as $a) {
    if ($a->name === "WIN_ROUNDS_LOW") {
        if ($a->value === 1) {
            $goodAchv = true;
            break;
        }
    }
} unset($a);

if (!$goodPlaytime || !$goodAchv || !$goodKills) {
    // Profile does not meet extra requirements
    consoleExit("{\"success\":false,\"error\":\"13222\"}");
}

$conn = mysqli_connect($server, $username, $password, $flaredb);
if ($conn->connect_error) {
    error_log("1312 - Failed to connect to MySQL Database '" . $flaredb . "' with error (" . $conn->connect_errno . "): " . $conn->connect_error);
    consoleExit("{\"success\":false,\"error\":\"1312\"}");
}

$query = "SELECT `id` FROM `Players_01` WHERE `steamid`=\"" . $conn->real_escape_string($steamID) . "\"";
$result = $conn->query($query);

if ($result->num_rows !== 0) {
    // Steam ID is already in use, cannot create profile
    consoleExit("{\"success\":false,\"error\":\"1324\"}");
}

if ($mode !== "create") {
    // Only checking if account COULD be made, say true
    consoleExit("{\"success\":true}");
}

// TODO: Implement account creation method



?>
