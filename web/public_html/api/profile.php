<?php

function pretty_exit($json) {
    $json = json_encode($json, JSON_PRETTY_PRINT);
    exit($json);
}

if (isset($_GET["id"])) {
    $found = false;
    if (is_numeric($_GET["id"])) {
        $steamID = $_GET["id"];
        $url = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/" .
        "v0002/?key=" . STEAMKEY . "&steamids=" . $steamID;
        $json= json_decode(file_get_contents($url));
        if (isset($json->response->players[0]->communityvisibilitystate)) {
            $found = true;
            $player = $json->response->players[0];
            if ($steamID !== $player->steamid) {
                exit("Nothing Found");
            }
            if ($player->communityvisibilitystate !== 3) {
                // Private account, return simple data
                $object = json_decode('{}');
                $object->{"steamname"} = $player->personaname;
                $object->{"visibility"} = "private";
                $object->{"url"} = $player->profileurl;
                $object->{"avatar"} = $player->avatarfull;
                pretty_exit($object);
            }
        }
    }
    if (!$found) {
        // Try vanity url
        unset($steamID);
        $url = "http://api.steampowered.com/ISteamUser/ResolveVanityURL/v0001/" .
               "?key=" . STEAMKEY . "&vanityurl=" . $_GET["id"];
        $json= json_decode(file_get_contents($url));
        if (!isset($json->response->steamid)) {
            exit("Nothing Found");
        }
        $steamID = $json->response->steamid;
        $url = "http://api.steampowered.com/ISteamUser/GetPlayerSummaries/" .
        "v0002/?key=" . STEAMKEY . "&steamids=" . $steamID;
        $json= json_decode(file_get_contents($url));
        if (!isset($json->response->players[0]->communityvisibilitystate)) {
            exit("Nothing Found");
        }
        $player = $json->response->players[0];
        if ($steamID !== $player->steamid) {
            exit("Nothing Found");
        }
        if ($player->communityvisibilitystate !== 3) {
            // Private account, return simple data
            $object = json_decode('{}');
            $object->{"steamname"} = $player->personaname;
            $object->{"visibility"} = "private";
            $object->{"url"} = $player->profileurl;
            $object->{"avatar"} = $player->avatarfull;
            pretty_exit($object);
        }

        /**
         * We won't try to search our DB for an account, simply because it would
         * take up resources for something that would frankly require copy+paste
         * code of everything above. All systems should search via steam id, but
         * we'll allow the vanity url to be an option.
         */

    }
} else {
    die("Missing required field `id`");
}

require_once("dbInf.php");

$conn = mysqli_connect(DB_SERVER, USERNAME, PASSWORD, FLAREDB);
if ($conn->connect_error) {
    die("An unexpected error occurred.");
}

$object = json_decode('{}');
// Base information
$object->{"steamname"} = $player->personaname;
$object->{"steamid"} = $player->steamid;
$object->{"visibility"} = "public";
$object->{"type"} = "none";
$object->{"url"} = $player->profileurl;
$object->{"avatar"} = $player->avatarfull;

$query = "SELECT `steamid`, `username`, `persona`, `bio`, `created`, `status` FROM `Players_01` WHERE `steamid`=\"" . $conn->real_escape_string($player->steamid) . "\" AND `verified`=1 LIMIT 1";

$result = $conn->query($query);

if ($result->num_rows === 0) {
    /**
     * Not in our DB, but that's okay.
     */
    pretty_exit($object);
} else {
    $result = $result->fetch_object();
    $status = json_decode($result->status);
    $object->{"type"} = $status->level;
    unset($status->level);
    $object->{"status"} = $status;
    $object->{"since"} = $result->created;
    $object->{"username"} = $result->username;
    $object->{"persona"} = $result->persona;
    $object->{"bio"} = $result->bio;
    pretty_exit($object);
}

?>
