<?php

// Our server time is based in Denver
date_default_timezone_set("America/Denver");

// Steam Setup
$steamauth['apikey'] = "585DCDF484116BAEED7DCEB43000A7F4";
$steamauth['domain'] = "http://www.flare-esports.net";
$steamauth['logout'] = $steamauth['domain'] . "/logout";
$steamauth['login'] = $steamauth['domain'];
define("STEAMKEY","585DCDF484116BAEED7DCEB43000A7F4");

// Javascript Console logging
// Prints the provided $output to the console and exits immediately.
function consoleExit($output){
    ?>
<!DOCTYPE html>
<html><body><script>console.log('FLARE-ESPORTS:<?=$output?>');</script><span id="out"><?=$output?></body></html>
    <?php
    exit();
}

// Convert JSON objects to a string for SQL statments
function jsonToSql($object) {
    // This is the best I got, and seems secure enough.
    return json_encode(json_encode($object));
}

// It's a little lonely here :(

?>
