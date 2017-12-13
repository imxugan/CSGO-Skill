<?php

if (isset($_GET["steamid"])) {
    if (is_numeric($_GET["steamid"])) {
        $steamID = $_GET["steamid"];
    } else {
        die("Provided Steam ID is not numeric. Only supports single IDs.");
    }
} else {
    die("Missing required field `steamid`");
}

require_once("dbInf.php");

function pretty_exit($json) {
    $json = json_encode($json, JSON_PRETTY_PRINT);
    exit($json);
}

$conn = mysqli_connect(DB_SERVER, USERNAME, PASSWORD, FLAREDB);
if ($conn->connect_error) {
    die("An unexpected error occurred.");
}

$query = "SELECT `id`, `type`, `global`, `history`, `current` FROM `Stats_01` WHERE `id`=\"" . $conn->real_escape_string($steamID) . "\" LIMIT 1";
$result = $conn->query($query);

if ($result->num_rows === 0) {
    /**
     * Tell the caller that the Steam ID needs to be added. In the case of our
     * webflow, the javascript will call `addAccount.php` and then call this
     * script again to grab the data.
     */
    exit("Add Player");
}

$result = $result->fetch_object();

$object = json_decode('{}');
$object->{"id"} = strval($result->id); // Force string just to be safe
$object->{"type"} = $result->type;
if ($result->type === "user") {
    $object->{"global"} = json_decode($result->global);
}
$object->{"history"} = json_decode($result->history);
$object->{"current"} = json_decode($result->current);

pretty_exit($object);

?>
