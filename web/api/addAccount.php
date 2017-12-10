<?php

require_once("setup.php");

if ($_SERVER["REQUEST_METHOD"] === "POST") {

    if (count($_POST) !== 4) {
        consoleExit("{\"success\":false,\"error\":\"14141\"}");
    }
    if (strlen(http_build_query($_POST)) > 200) {
        consoleExit("{\"success\":false,\"error\":\"14281\"}");
    }
    $expected = array("steamid", "email", "name", "verify");
    foreach ($expected as $f) {
        if (!array_key_exists($f, $_POST)) {
            consoleExit("{\"success\":false,\"error\":\"14142\"}");
        }
    }

    $email = $_POST["email"];
    $name = $_POST["name"];
    $steamID = $_POST["steamid"];
    $verify = $_POST["verify"];
    $create = true;

    // This will do the rest for us
    require_once("makeAccount.php");

} else if ($_SERVER["REQUEST_METHOD"] === "GET") {

    if (count($_GET) !== 1) {
        consoleExit("{\"success\":false,\"error\":\"14143\"}");
    }
    if (strlen(http_build_query($_GET)) > 30) {
        consoleExit("{\"success\":false,\"error\":\"14282\"}");
    }
    $expected = array("steamid");
    foreach ($expected as $f) {
        if (!array_key_exists($f, $_GET)) {
            consoleExit("{\"success\":false,\"error\":\"14144\"}");
        }
    }

    require_once("checkSteamProfile.php");
    $stats = check($_GET["steamid"], false, true);
    if ($stats === NULL) {
        consoleExit("{\"success\":false,\"error\":\"1422\"}");
    }

}

/**
 * Rather than create another possibilty for `makeAccount` and waste the
 * resources to require() it, we'll just call the simple SQL here.
 */

$type = "user";
$get = false;

if ($_SERVER["REQUEST_METHOD"] === "GET") {
    require_once("dbInf.php");
    $conn = mysqli_connect(DB_SERVER, USERNAME, PASSWORD, FLAREDB);
    if ($conn->connect_error) {
        error_log("1412 - Failed to connect to MySQL Database '" . FLAREDB .
        "' with error (" . $conn->connect_errno . "): " . $conn->connect_error);
        consoleExit("{\"success\":false,\"error\":\"1412\"}");
    }
    $id = $_GET["steamid"];
    $steamID = $id;
    $type = "none";
    $get = true;
}

$stats = json_decode("{\"today\":{" . json_encode($stats) . "}}");
$stats->{"info"} = json_decode("{\"type\":\"" . $type . "\",\"steamid\":\"" . $steamID . "\"}");
$stats->{"entries_month"} = json_decode("[]");
$stats->{"entries_year"} = json_decode("[]");

$query = "INSERT INTO `Stats_01` (`id`, `type`, `data`) VALUES (\"" . $id . "\", \"" . $type . "\", \"" . jsonToSql($stats) . "\")";

if ($conn->query($query)) {
    $conn->close();
    if ($get) {
        consoleExit("{\"success\":true}");
    } else {
        consoleExit("{\"success\":true,\"secret\":\"" . $secret . "\"}");
    }
} else {
    // Query failed, I cri evrytiem ;(
    if ($get) {
        $conn->close();
        consoleExit("{\"success\":false,\"error\":\"14261\"}");
    } else {
        error_log("14262 - The SQL query to complete user stats insert failed. Here's what we got: " . print_r($conn->error_list, true));
        $conn->close();
        consoleExit("{\"success\":true,\"error\":\"14262\",\"secret\":\"" . $secret . "\"}");
    }

}



?>
