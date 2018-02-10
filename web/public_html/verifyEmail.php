<?php

// TODO: This is NOT user-friendly at all. Rework before release!

require_once("setup.php");

if (count($_GET) !== 2) {
    die("{\"success\":false,\"error\":\"1514\"}");
}
if (strlen(http_build_query($_GET)) > 50) {
    die("{\"success\":false,\"error\":\"1528\"}");
}
$expected = array("u", "k");
foreach ($expected as $f) {
    if (!array_key_exists($f, $_GET)) {
        die("{\"success\":false,\"error\":\"1514\"}");
    }
}

$conn = mysqli_connect(DB_SERVER, USERNAME, PASSWORD, FLAREDB);
if ($conn->connect_error) {
    error_log("1512 - Failed to connect to MySQL Database '" . FLAREDB .
    "' with error (" . $conn->connect_errno . "): " . $conn->connect_error);
    die("{\"success\":false,\"error\":\"1512\"}");
}

$steamID = $conn->real_escape_string($_GET["u"]);
$verify = $conn->real_escape_string($_GET["k"]);

$query = "SELECT `status`, `verified` FROM `Players_01` WHERE `steamid`=\"" . $steamID . "\"";
$result = $conn->query($query);

if ($result->num_rows !== 1) {
    $conn->close();
    die("{\"success\":false,\"error\":\"1527\"}");
}

$result = $result->fetch_assoc();
if ($result["verified"] === 1) {
    // Already verified!
    $conn->close();
    exit("{\"success\":true}");
}
$status = json_decode($result["status"]);
if ($status->link->action === "verify" && $verify === $status->link->secret) {
    unset($status->link);
    $query = "UPDATE `Players_01` SET `verified` = 1, " .
    "`status`=" . json_encode(json_encode($status)) . " WHERE `steamid`=\"" . $steamID . "\"";
    if (!$conn->query($query)) {
        $conn->close();
        die("{\"success\":false,\"error\":\"Something weird happened. Try refreshing the page.\"}");
    }
    $conn->close();
    exit("{\"success\":true}");
}

?>
