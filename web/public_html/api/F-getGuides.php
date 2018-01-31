<?php

/* Don't forget to delete this when we eventually implement this */
exit("Coming in a future version");

/*
require_once("setup.php");

if (!isset($_POST["version"])) {
    error_log("11141 - Missing POST[version] information, perhaps a direct request?");
    die('{"success":false,"error":"11141"}');
} else {
    $json = json_decode($_POST["version"]);

    if (is_null($json)) {
        error_log("1115 - Invalid JSON for POST[version], perhaps a direct request?");
        die('{"success":false,"error":"1115"}');
    }

    if (!isset($json->major) || !is_int($json->major)){
        error_log("11142 - Provided MAJOR version is missing or NaN, perhaps a direct request?");
        die('{"success":false,"error":"11142"}');
    } else if (!isset($json->minor) || !is_int($json->minor)){
        error_log("11143 - Provided MINOR version is missing or NaN, perhaps a direct request?");
        die('{"success":false,"error":"11143"}');
    } else if (!isset($json->locale)){
        error_log("11144 - Provided LOCALE is missing, perhaps a direct request?");
        die('{"success":false,"error":"11144"}');
    } else {
        define("LOCALE",$json->locale);
        if (getLocale("") === "invalid") {
            // Error was already logged.
            die('{"success":false,"error":"11145"}');
        }
        $major = $json->major;
        $minor = $json->minor;
    }
}

// Here is all our supported languages
function getLocale($name) {
    if (LOCALE === "en"){
        return $name . "_en";
    } else {
        error_log("11145 - Given locale '" . LOCALE . "' not found.");
        return "invalid";
    }
}

// Various selection data for various versions
// v1.0.X
if ($major == 1 && $minor == 0) {
    $query = "SELECT `data`, `name` FROM `AppData` WHERE `version` LIKE 'v1.0.%' ";
    $query .= "AND `name` IN ('" . getLocale("equipment") . "', '" . getLocale("guides") . "', 'nades') ORDER BY `id` DESC";
} else if ($major > 1 && minor > 0) {
    error_log("1117 - FUTURE version `v" . $major . "." . $minor . "` requested?");
    die('{"success":false,"error":"1117"}');
} else {
    error_log("1116 - UNKNOWN version `v" . $major . "." . $minor . "` requested.");
    die('{"success":false,"error":"1116"}');
}

$conn = mysqli_connect(DB_SERVER, USERNAME, PASSWORD, FLAREDB);
if ($conn->connect_error) {
    error_log("1112 - Failed to connect to MySQL Database '" . FLAREDB . "' with error (" . $conn->connect_errno . "): " . $conn->connect_error);
    die('{"success":false,"error":"1112"}');
}

$result = $conn->query($query);

if ($result->num_rows == 0) {
    error_log("1113 - Failed to find any rows in `" . FLAREDB . "`.`AppData`, empty result: " . print_r($conn->error_list, true));
    die('{"success":false,"error":"1113"}');
} else {
    while ($row = $result->fetch_assoc()) {
        // v1.0.X
        if ($major == 1 && $minor == 0) {
            if ($row["name"] == getLocale("equipment") && !isset($equipment)) {
                $equipment = $row["data"];
            } else if ($row["name"] == getLocale("guides") && !isset($guides)) {
                $guides = $row["data"];
            } else if ($row["name"] == "nades" && !isset($nades)) {
                $nades = $row["data"];
            }
        }
    }
}

// Now we have what we need, puke out the data

// v1.0.X
if ($major == 1 && $minor == 0) {
    exit('{"equipment":' . $equipment .
         ',"guides":' . $guides .
         ',"nades":' . $nades .
         '}');
}

/** FOR THE FUTURE
 * We may eventually decide that certain versions of the data in our DB is so
 * old and unused that it may as well not even be there.
 *
 * In such cases we'll end up deleting those old entries, and then we must make
 * sure that we still handle requests for outdated information. The default way
 * that we'll do this is by going to the very first time we check that version,
 * and do a `die('{"success":false,"error":"Unsupported, please update"}')`
 */


?>
