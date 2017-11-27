<?php

// Our server time is based in Denver
date_default_timezone_set("America/Denver");

require("dbInf.php");

$conn = mysqli_connect($server, $username, $password, $flaredb);
if ($conn->connect_error) {
    die('{"success":false}');
    // TODO: Implement error log for this.
}


?>
