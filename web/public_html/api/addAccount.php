<?php

require_once("setup.php");

if ($_SERVER["REQUEST_METHOD"] === "POST") {

    if (count($_POST) !== 4) {
        exit("{\"success\":false,\"error\":\"14141\"}");
    }
    if (strlen(http_build_query($_POST)) > 250) {
        exit("{\"success\":false,\"error\":\"14281\"}");
    }
    $expected = array("steamid", "email", "name", "verify");
    foreach ($expected as $f) {
        if (!array_key_exists($f, $_POST)) {
            exit("{\"success\":false,\"error\":\"14142\"}");
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

    // Because this should only be run by our system, don't report errors.
    if (count($_GET) !== 1) {
        die("An unexpected error occurred.");
    }
    if (strlen(http_build_query($_GET)) > 40) {
        die("An unexpected error occurred.");
    }

    if (!isset($_GET["steamid"])) {
        die("An unexpected error occurred.");
    }

    require_once("checkSteamProfile.php");
    $stats = check($_GET["steamid"], false, true);
    if ($stats === NULL) {
        exit("{\"success\":false,\"error\":\"1422\"}");
    }

} else {
    // Incase none of the above happened, just quit
    exit();
}

/**
 * Rather than create another possibilty for `makeAccount` and waste the
 * resources to require() it, we'll just call the simple SQL here.
 */

$type = "user";
$get = false;
$conn = mysqli_connect(DB_SERVER, USERNAME, PASSWORD, FLAREDB);
if ($conn->connect_error) {
    error_log("1412 - Failed to connect to MySQL Database '" . FLAREDB .
    "' with error (" . $conn->connect_errno . "): " . $conn->connect_error);
    exit("{\"success\":false,\"error\":\"1412\"}");
}

if ($_SERVER["REQUEST_METHOD"] === "GET") {
    $steamID = $_GET["steamid"]; // Safe for SQL at this point
    $type = "none";
    $get = true;

    // Checking if account already exists, if it does, then the app is likely
    // trying to repair the account because the stats insert failed.
    $query = "SELECT `id`, `steamid` FROM `Players_01` WHERE `steamid`=\"" . $steamID . "\"";
    $result = $conn->query($query);

    // Account may need to be repaired!
    if ($result->num_rows !== 0) {
        $query = "SELECT `type` FROM `Stats_01` WHERE `id`=\"" . $steamID . "\"";
        $result = $conn->query($query);
        if ($result->num_rows !== 0) {
            // Try to update the Type
            $result = $result->fetch_assoc();
            if ($result["type"] === "none") {
                $query = "UPDATE `Stats_01` SET `type` = \"user\" WHERE `id` = \"" . $steamID . "\"";
                if (!$conn->query($query)) {
                    error_log("!!! - The SQL query to repair user stats TYPE failed. Here's what we got: " . print_r($conn->error_list, true));
                    $conn->close();
                    exit("{\"success\":false}");
                }
            }
            $conn->close();
            exit("{\"success\":true}");
        } else {
            // The whole row needs to be inserted
            require_once("data_template.php");
            $global = jsonToSql(getDataTemplate());
            $history = jsonToSql(json_decode("{\"today\":{" . json_encode($stats) . "},\"entries_month\":[],\"entries_year\":[]}"));
            $stats = jsonToSql($stats);

            $query = "INSERT INTO `Stats_01` (`id`, `type`, `current`, global`, `history`) " .
                     "VALUES (\"$steamID\", \"$type\", \"$stats\", \"$global\", \"$history\")";

            if (!$conn->query($query)) {
                error_log("!!! - The SQL query to repair user stats INSERT failed. Here's what we got: " . print_r($conn->error_list, true));
                $conn->close();
                exit("{\"success\":false}");
            }
            $conn->close();
            exit("{\"success\":true}");
        }
    }
}

$query = "SELECT `type` FROM `Stats_01` WHERE `id`=\"" . $steamID . "\"";
$result = $conn->query($query);
if ($result->num_rows !== 0) {
    // Non-user already in Stats table, cool?
    if ($get) { $conn->close(); exit("{\"success\":true}"); }

    // In Stats table, but we may update the Type
    $result = $result->fetch_assoc();
    if ($result["type"] === "none") {
        $query = "UPDATE `Stats_01` SET `type` = \"user\" WHERE `id` = \"" . $steamID . "\"";
        if ($conn->query($query)) {
            $conn->close();
            exit("{\"success\":true,\"secret\":\"" . $secret . "\"}");
        } else {
            error_log("14262 - The SQL query to complete user stats insert failed. Here's what we got: " . print_r($conn->error_list, true));
            $conn->close();
            exit("{\"success\":true,\"error\":\"14262\",\"secret\":\"" . $secret . "\"}");
        }
    } else {
        // In Stats table, and already existing account? idk, return true?
        $conn->close();
        exit("{\"success\":true}");
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
        exit("{\"success\":true}");
    } else {
        exit("{\"success\":true,\"secret\":\"" . $secret . "\"}");
    }
} else {
    // Query failed, I cri evrytiem ;(
    if ($get) {
        $conn->close();
        exit("{\"success\":false,\"error\":\"14261\"}");
    } else {
        error_log("14263 - The SQL query to complete user stats insert failed. Here's what we got: " . print_r($conn->error_list, true));
        $conn->close();
        exit("{\"success\":true,\"error\":\"14263\",\"secret\":\"" . $secret . "\"}");
    }

}



?>
