<?php

require_once("dbInf.php");

function cron_log($message) {
    echo date("[Y-m-d H:i:s] ") . $message . "\r\n";
}

$conn = mysqli_connect(DB_SERVER, USERNAME, PASSWORD, FLAREDB);
if ($conn->connect_error) {
    cron_log("Failed to connect to DB (" . $conn->connect_errno . "): " . $conn->connect_error));
    die();
}

$logMail = "devteam@csgo-skill.com";
$headers  = "From: Skill Bot <skillbot@csgo-skill.com>\r\nTo: " . $devMails . "\r\n";
$headers .= "MIME-Version: 1.0\r\nContent-Type: text/html; charset=iso-8859-1\r\n";
$limit = new DateTime("-2 day");

$query = "DELETE FROM `Players_01` WHERE `verified` = 0 AND `created` <= \"" . $limit->format("Y-m-d H:i:s") . "\"";
if ($conn->query($query)) {
    $number = $conn->affected_rows;
    if ($number > 0) {
        // I don't see why we need to be notified if no accounts where removed.
        mail($logMail, "Removed Unverified Accounts", "System successfully deleted $number unverified accounts which where older than 2 days.", $headers);
    }
    cron_log("removeAccounts called successfully with $number accounts deleted.");
} else {
    cron_log("removeAccounts failed. Here's what we got: " . print_r($conn->error_list, true));
    mail($logMail, "Failed to Remove Accounts", "System was unable to delete unverified accounts due to a query error.", $headers);
}

?>
