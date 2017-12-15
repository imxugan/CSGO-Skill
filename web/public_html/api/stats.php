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
// We can send up to 6 months of changes at a time, and with the `offset` we can begin later
if (isset($_GET["offset"])) {
    if (!is_int($_GET["offset"])) {
        die("Offset is not a valid number.");
    }
    $offset = intval($_GET["offset"]);
    if ($offset < 1) {
        die("Minimum offset value is 1");
    }
    if ($offset > 54) {
        die("Maximum offset value is 54");
    }
    $offset--;
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
if (isset($offset)) {
    // Only asking for the monthly stats
    $arr = json_decode($result->history)->entries_year;
    if (count($arr) < $offset) {
        pretty_exit(array());
    }
    pretty_exit(array_slice($arr, $offset, 6));
}

$object = json_decode('{}');
$object->{"id"} = strval($result->id); // Force string just to be safe
$object->{"type"} = $result->type;
if ($result->type === "user") {
    $object->{"global"} = json_decode($result->global);
}
$history = json_decode($result->history);
$object->{"daily"} = $history->entries_month;
$object->{"monthly"} = array_slice($history->entries_year, 0, 6);
$object->{"current"} = json_decode($result->current);

pretty_exit($object);

?>
