<?php

require_once("setup");
require_once("dbInf");

$conn = mysqli_connect($server, $username, $password, $flaredb);
if ($conn->connect_error) {
    error_log("1012 - Failed to connect to MySQL Database '" . $flaredb . "' with error (" . $conn->connect_errno . "): " . $conn->connect_error);
    die('{"success":false,"error":"1012"}');
}

// Because version updates will add a new "version" row, we must check rows last-first
$query = "SELECT `data` FROM `AppData` WHERE `name`='version' ORDER BY `id` DESC";
$result = $conn->query($query);

if ($result->num_rows == 0) {
    error_log("1013 - Failed to find any 'version' rows in `" . $flaredb . "`.`AppData`, empty result.")
    die('{"success":false,"error":"1013"}');
} else {
    while ($row = $result->fetch_assoc()) {
        // Stop at first result, because it is the most up-to-date
        exit($row["data"]);
    }
}

?>
