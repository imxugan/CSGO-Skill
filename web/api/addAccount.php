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
    if (strlen(http_build_query($_GET)) > 40) {
        consoleExit("{\"success\":false,\"error\":\"14282\"}");
    }

    if (!isset($_GET["steamid"]))
        consoleExit("{\"success\":false,\"error\":\"14144\"}");
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
    $steamID = $_GET["steamid"]; // Safe for SQL at this point
    $type = "none";
    $get = true;
}

$query = "SELECT `type` FROM `Stats_01` WHERE `id`=\"" . $steamID . "\"";
$result = $conn->query($query);
if ($result->num_rows !== 0) {
    // Non-user already in Stats table, cool?
    if ($get) { $conn->close(); consoleExit("{\"success\":true}"); }

    // In Stats table, but we may update the Type
    $result = $result->fetch_assoc();
    if ($result["type"] === "none") {
        $query = "UPDATE `Stats_01` SET `type` = \"user\" WHERE `id` = \"" . $steamID . "\"";
        if ($conn->query($query)) {
            $conn->close();
            consoleExit("{\"success\":true,\"secret\":\"" . $secret . "\"}");
        } else {
            error_log("14262 - The SQL query to complete user stats insert failed. Here's what we got: " . print_r($conn->error_list, true));
            $conn->close();
            consoleExit("{\"success\":true,\"error\":\"14262\",\"secret\":\"" . $secret . "\"}");
        }
    } else {
        // In Stats table, and already existing account? idk, return true?
        $conn->close();
        consoleExit("{\"success\":true}");
    }
}

// Not in table, insert a fresh new avocado
require_once("data_template.php");
$global = jsonToSql(getDataTemplate());
$history = jsonToSql(json_decode("{\"today\":{" . json_encode($stats) . "},\"entries_month\":[],\"entries_year\":[]}"));
$stats = jsonToSql($stats);

$query = "INSERT INTO `Stats_01` (`id`, `type`, `current`, global`, `history`) " .
         "VALUES (\"$steamID\", \"$type\", \"$stats\", \"$global\", \"$history\")";

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
        error_log("14263 - The SQL query to complete user stats insert failed. Here's what we got: " . print_r($conn->error_list, true));
        $conn->close();
        consoleExit("{\"success\":true,\"error\":\"14263\",\"secret\":\"" . $secret . "\"}");
    }

}



?>
